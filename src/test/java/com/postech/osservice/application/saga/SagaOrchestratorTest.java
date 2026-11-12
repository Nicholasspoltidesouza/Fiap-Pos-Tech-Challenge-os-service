package com.postech.osservice.application.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.postech.osservice.application.messaging.OficinaEvent;
import com.postech.osservice.application.messaging.RoutingKeys;
import com.postech.osservice.application.port.EventPublisherPort;
import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.application.port.SagaRepositoryPort;
import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.SagaState;
import com.postech.osservice.domain.model.SagaStatus;
import com.postech.osservice.domain.model.SagaStep;
import com.postech.osservice.domain.model.StatusOrdemServico;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

    @Mock
    private OrdemServicoRepositoryPort ordemServicoRepository;

    @Mock
    private SagaRepositoryPort sagaRepository;

    @Mock
    private EventPublisherPort eventPublisher;

    private SagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SagaOrchestrator(ordemServicoRepository, sagaRepository, eventPublisher);
        when(ordemServicoRepository.save(any(OrdemServico.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sagaRepository.save(any(SagaState.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldStartSagaAndPublishQuoteRequest() {
        OrdemServico ordem = OrdemServico.criar(
                UUID.randomUUID(), UUID.randomUUID(), "Engine noise", BigDecimal.TEN);

        OrdemServico result = orchestrator.startSaga(ordem);

        assertEquals(StatusOrdemServico.AGUARDANDO_ORCAMENTO, result.getStatus());
        verify(eventPublisher).publish(eq(RoutingKeys.ORDEM_CRIADA), any(OficinaEvent.class));
        verify(eventPublisher).publish(eq(RoutingKeys.ORCAMENTO_GERAR), any(OficinaEvent.class));
        verify(sagaRepository, atLeastOnce()).save(any(SagaState.class));
    }

    @Test
    void shouldMoveToAguardandoAprovacaoOnQuoteGenerated() {
        OrdemServico ordem = preparedOrder(StatusOrdemServico.AGUARDANDO_ORCAMENTO);
        SagaState saga = SagaState.start(ordem.getId());
        saga.advanceTo(SagaStep.QUOTE_REQUESTED);

        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));
        when(sagaRepository.findByOrdemServicoId(ordem.getId())).thenReturn(Optional.of(saga));

        orchestrator.onOrcamentoGerado(ordem.getId(), new BigDecimal("200.00"));

        assertEquals(StatusOrdemServico.AGUARDANDO_APROVACAO, ordem.getStatus());
        assertEquals(SagaStep.QUOTE_GENERATED, saga.getCurrentStep());
    }

    @Test
    void shouldRequestPaymentWhenQuoteApproved() {
        OrdemServico base = preparedOrder(StatusOrdemServico.AGUARDANDO_APROVACAO);
        OrdemServico ordem = OrdemServico.reconstituir(
                base.getId(),
                base.getClienteId(),
                base.getVeiculoId(),
                base.getDescricaoProblema(),
                StatusOrdemServico.AGUARDANDO_APROVACAO,
                base.getValorEstimado(),
                new BigDecimal("200.00"),
                null,
                base.getDataAbertura(),
                null,
                base.getAtualizadoEm());

        SagaState saga = SagaState.start(ordem.getId());
        saga.advanceTo(SagaStep.QUOTE_GENERATED);

        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));
        when(sagaRepository.findByOrdemServicoId(ordem.getId())).thenReturn(Optional.of(saga));

        OrdemServico updated = orchestrator.onOrcamentoDecision(ordem.getId(), true);

        assertEquals(StatusOrdemServico.AGUARDANDO_PAGAMENTO, updated.getStatus());
        verify(eventPublisher).publish(eq(RoutingKeys.PAGAMENTO_SOLICITAR), any(OficinaEvent.class));
    }

    @Test
    void shouldCompensateWhenPaymentFails() {
        OrdemServico ordem = OrdemServico.reconstituir(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Brake issue",
                StatusOrdemServico.AGUARDANDO_PAGAMENTO,
                BigDecimal.TEN,
                new BigDecimal("150.00"),
                true,
                java.time.LocalDateTime.now(),
                null,
                java.time.LocalDateTime.now());
        SagaState saga = SagaState.start(ordem.getId());
        saga.advanceTo(SagaStep.PAYMENT_REQUESTED);

        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));
        when(sagaRepository.findByOrdemServicoId(ordem.getId())).thenReturn(Optional.of(saga));

        orchestrator.onPagamentoFalhou(ordem.getId(), "card declined");

        assertEquals(StatusOrdemServico.FALHA_COMPENSADA, ordem.getStatus());
        assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        verify(eventPublisher).publish(eq(RoutingKeys.COMPENSAR_ORCAMENTO), any(OficinaEvent.class));
        verify(eventPublisher).publish(eq(RoutingKeys.COMPENSAR_PAGAMENTO), any(OficinaEvent.class));
    }

    @Test
    void shouldFinalizeOnExecutionFinished() {
        OrdemServico ordem = OrdemServico.reconstituir(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Brake issue",
                StatusOrdemServico.EM_EXECUCAO,
                BigDecimal.TEN,
                new BigDecimal("150.00"),
                true,
                java.time.LocalDateTime.now(),
                null,
                java.time.LocalDateTime.now());
        SagaState saga = SagaState.start(ordem.getId());
        saga.advanceTo(SagaStep.EXECUTION_STARTED);

        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));
        when(sagaRepository.findByOrdemServicoId(ordem.getId())).thenReturn(Optional.of(saga));

        orchestrator.onExecucaoFinalizada(ordem.getId());

        assertEquals(StatusOrdemServico.FINALIZADA, ordem.getStatus());
        assertEquals(SagaStatus.COMPLETED, saga.getStatus());
    }

    @Test
    void shouldStartExecutionAfterPaymentApproved() {
        OrdemServico ordem = OrdemServico.reconstituir(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Brake issue",
                StatusOrdemServico.AGUARDANDO_PAGAMENTO,
                BigDecimal.TEN,
                new BigDecimal("150.00"),
                true,
                java.time.LocalDateTime.now(),
                null,
                java.time.LocalDateTime.now());
        SagaState saga = SagaState.start(ordem.getId());
        saga.advanceTo(SagaStep.PAYMENT_REQUESTED);

        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));
        when(sagaRepository.findByOrdemServicoId(ordem.getId())).thenReturn(Optional.of(saga));

        orchestrator.onPagamentoAprovado(ordem.getId());

        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordem.getStatus());
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).publish(keyCaptor.capture(), any(OficinaEvent.class));
        assertEquals(RoutingKeys.EXECUCAO_INICIAR, keyCaptor.getValue());
    }

    private OrdemServico preparedOrder(StatusOrdemServico status) {
        OrdemServico created = OrdemServico.criar(
                UUID.randomUUID(), UUID.randomUUID(), "Noise", BigDecimal.ONE);
        return OrdemServico.reconstituir(
                created.getId(),
                created.getClienteId(),
                created.getVeiculoId(),
                created.getDescricaoProblema(),
                status,
                created.getValorEstimado(),
                null,
                null,
                created.getDataAbertura(),
                null,
                created.getAtualizadoEm());
    }
}
