package com.postech.osservice.application.saga;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.postech.osservice.application.messaging.EventTypes;
import com.postech.osservice.application.messaging.OficinaEvent;
import com.postech.osservice.application.messaging.RoutingKeys;
import com.postech.osservice.application.port.EventPublisherPort;
import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.application.port.SagaRepositoryPort;
import com.postech.osservice.domain.exception.OrdemServicoNaoEncontradaException;
import com.postech.osservice.domain.exception.SagaException;
import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.SagaState;
import com.postech.osservice.domain.model.SagaStep;
import com.postech.osservice.domain.model.StatusOrdemServico;

@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final SagaRepositoryPort sagaRepository;
    private final EventPublisherPort eventPublisher;

    public SagaOrchestrator(
            OrdemServicoRepositoryPort ordemServicoRepository,
            SagaRepositoryPort sagaRepository,
            EventPublisherPort eventPublisher) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.sagaRepository = sagaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrdemServico startSaga(OrdemServico ordem) {
        OrdemServico saved = ordemServicoRepository.save(ordem);
        ordemServicoRepository.appendStatusHistory(saved.getId(), saved.getStatus(), "Service order created");

        SagaState saga = SagaState.start(saved.getId());
        sagaRepository.save(saga);

        saved.solicitarOrcamento();
        saved = ordemServicoRepository.save(saved);
        ordemServicoRepository.appendStatusHistory(
                saved.getId(), saved.getStatus(), "Quote generation requested");

        Map<String, Object> payload = basePayload(saved);
        payload.put("descricaoProblema", saved.getDescricaoProblema());
        payload.put("veiculoId", saved.getVeiculoId().toString());
        if (saved.getValorEstimado() != null) {
            payload.put("valorEstimado", saved.getValorEstimado());
        }

        eventPublisher.publish(
                RoutingKeys.ORDEM_CRIADA,
                OficinaEvent.of(EventTypes.ORDEM_SERVICO_CRIADA, saved.getId(), saved.getClienteId(), payload));
        eventPublisher.publish(
                RoutingKeys.ORCAMENTO_GERAR,
                OficinaEvent.of(EventTypes.ORDEM_SERVICO_CRIADA, saved.getId(), saved.getClienteId(), payload));

        saga.advanceTo(SagaStep.QUOTE_REQUESTED);
        sagaRepository.save(saga);

        log.info("Saga started for OS {}", saved.getId());
        return saved;
    }

    @Transactional
    public void onOrcamentoGerado(UUID ordemServicoId, BigDecimal valorOrcamento) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);

        ordem.registrarOrcamentoGerado(valorOrcamento);
        ordemServicoRepository.save(ordem);
        ordemServicoRepository.appendStatusHistory(
                ordem.getId(), ordem.getStatus(), "Quote generated, awaiting approval");

        saga.advanceTo(SagaStep.QUOTE_GENERATED);
        sagaRepository.save(saga);
        log.info("Quote generated for OS {}", ordemServicoId);
    }

    @Transactional
    public OrdemServico onOrcamentoDecision(UUID ordemServicoId, boolean aprovado) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);

        ordem.aprovarOrcamento(aprovado);
        ordem = ordemServicoRepository.save(ordem);

        if (!aprovado) {
            ordemServicoRepository.appendStatusHistory(
                    ordem.getId(), ordem.getStatus(), "Quote rejected by customer");
            compensate(ordem, saga, "Quote rejected");
            eventPublisher.publish(
                    RoutingKeys.COMPENSAR_ORCAMENTO,
                    OficinaEvent.of(
                            EventTypes.ORCAMENTO_REJEITADO,
                            ordem.getId(),
                            ordem.getClienteId(),
                            Map.of("motivo", "Quote rejected by customer")));
            return ordem;
        }

        ordemServicoRepository.appendStatusHistory(
                ordem.getId(), ordem.getStatus(), "Quote approved, requesting payment");

        Map<String, Object> payload = basePayload(ordem);
        payload.put("valorOrcamento", ordem.getValorOrcamento());

        eventPublisher.publish(
                RoutingKeys.PAGAMENTO_SOLICITAR,
                OficinaEvent.of(EventTypes.ORCAMENTO_APROVADO, ordem.getId(), ordem.getClienteId(), payload));

        saga.advanceTo(SagaStep.QUOTE_APPROVED);
        saga.advanceTo(SagaStep.PAYMENT_REQUESTED);
        sagaRepository.save(saga);
        return ordem;
    }

    @Transactional
    public void onPagamentoAprovado(UUID ordemServicoId) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);

        ordem.registrarPagamentoAprovado();
        ordemServicoRepository.save(ordem);
        ordemServicoRepository.appendStatusHistory(
                ordem.getId(), ordem.getStatus(), "Payment approved, starting execution");

        Map<String, Object> payload = basePayload(ordem);
        eventPublisher.publish(
                RoutingKeys.EXECUCAO_INICIAR,
                OficinaEvent.of(EventTypes.PAGAMENTO_APROVADO, ordem.getId(), ordem.getClienteId(), payload));

        saga.advanceTo(SagaStep.PAYMENT_APPROVED);
        saga.advanceTo(SagaStep.EXECUTION_STARTED);
        sagaRepository.save(saga);
    }

    @Transactional
    public void onPagamentoFalhou(UUID ordemServicoId, String motivo) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);
        compensate(ordem, saga, motivo == null ? "Payment failed" : motivo);
        publishCompensationEvents(ordem, true, true, false);
    }

    @Transactional
    public void onOrcamentoRejeitado(UUID ordemServicoId, String motivo) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);
        if (ordem.getStatus() != StatusOrdemServico.CANCELADA
                && ordem.getStatus() != StatusOrdemServico.FALHA_COMPENSADA) {
            compensate(ordem, saga, motivo == null ? "Quote rejected remotely" : motivo);
            publishCompensationEvents(ordem, true, false, false);
        }
    }

    @Transactional
    public void onExecucaoIniciada(UUID ordemServicoId) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);

        if (ordem.getStatus() != StatusOrdemServico.EM_EXECUCAO) {
            ordem.registrarExecucaoIniciada();
            ordemServicoRepository.save(ordem);
            ordemServicoRepository.appendStatusHistory(
                    ordem.getId(), ordem.getStatus(), "Execution started");
        }

        saga.advanceTo(SagaStep.EXECUTION_STARTED);
        sagaRepository.save(saga);
    }

    @Transactional
    public void onExecucaoFinalizada(UUID ordemServicoId) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        SagaState saga = requireSaga(ordemServicoId);

        ordem.finalizar();
        ordemServicoRepository.save(ordem);
        ordemServicoRepository.appendStatusHistory(
                ordem.getId(), ordem.getStatus(), "Execution finished, OS finalized");

        saga.advanceTo(SagaStep.EXECUTION_FINISHED);
        saga.advanceTo(SagaStep.COMPLETED);
        sagaRepository.save(saga);
        log.info("Saga completed for OS {}", ordemServicoId);
    }

    @Transactional
    public OrdemServico entregar(UUID ordemServicoId) {
        OrdemServico ordem = requireOrder(ordemServicoId);
        ordem.entregar();
        ordem = ordemServicoRepository.save(ordem);
        ordemServicoRepository.appendStatusHistory(
                ordem.getId(), ordem.getStatus(), "Vehicle delivered to customer");
        return ordem;
    }

    private void compensate(OrdemServico ordem, SagaState saga, String error) {
        saga.markCompensating(error);
        sagaRepository.save(saga);

        try {
            if (ordem.getStatus() != StatusOrdemServico.CANCELADA
                    && ordem.getStatus() != StatusOrdemServico.FALHA_COMPENSADA) {
                ordem.cancelar();
            }
        } catch (RuntimeException ex) {
            ordem.marcarFalhaCompensada();
        }

        if (ordem.getStatus() == StatusOrdemServico.CANCELADA) {
            ordem.marcarFalhaCompensada();
        }

        ordemServicoRepository.save(ordem);
        ordemServicoRepository.appendStatusHistory(
                ordem.getId(), ordem.getStatus(), "Compensation executed: " + error);

        saga.markCompensated();
        sagaRepository.save(saga);
        log.warn("Saga compensated for OS {} reason={}", ordem.getId(), error);
    }

    private void publishCompensationEvents(
            OrdemServico ordem,
            boolean orcamento,
            boolean pagamento,
            boolean execucao) {
        Map<String, Object> payload = basePayload(ordem);
        payload.put("motivo", "Saga compensation");

        if (orcamento) {
            eventPublisher.publish(
                    RoutingKeys.COMPENSAR_ORCAMENTO,
                    OficinaEvent.of(EventTypes.COMPENSAR_ORCAMENTO, ordem.getId(), ordem.getClienteId(), payload));
        }
        if (pagamento) {
            eventPublisher.publish(
                    RoutingKeys.COMPENSAR_PAGAMENTO,
                    OficinaEvent.of(EventTypes.COMPENSAR_PAGAMENTO, ordem.getId(), ordem.getClienteId(), payload));
        }
        if (execucao) {
            eventPublisher.publish(
                    RoutingKeys.COMPENSAR_EXECUCAO,
                    OficinaEvent.of(EventTypes.COMPENSAR_EXECUCAO, ordem.getId(), ordem.getClienteId(), payload));
        }
    }

    private Map<String, Object> basePayload(OrdemServico ordem) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ordemServicoId", ordem.getId().toString());
        payload.put("clienteId", ordem.getClienteId().toString());
        payload.put("status", ordem.getStatus().name());
        return payload;
    }

    private OrdemServico requireOrder(UUID id) {
        return ordemServicoRepository.findById(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
    }

    private SagaState requireSaga(UUID ordemServicoId) {
        return sagaRepository.findByOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new SagaException("Saga not found for OS " + ordemServicoId));
    }
}
