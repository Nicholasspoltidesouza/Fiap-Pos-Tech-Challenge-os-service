package com.postech.osservice.infrastructure.messaging;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.postech.osservice.application.messaging.OficinaEvent;
import com.postech.osservice.application.saga.SagaOrchestrator;

@Component
public class OsEventListener {

    private static final Logger log = LoggerFactory.getLogger(OsEventListener.class);

    private final SagaOrchestrator sagaOrchestrator;

    public OsEventListener(SagaOrchestrator sagaOrchestrator) {
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORCAMENTO_GERADO)
    public void onOrcamentoGerado(OficinaEvent event) {
        log.info("Received OrcamentoGerado for OS {}", event.ordemServicoId());
        sagaOrchestrator.onOrcamentoGerado(event.ordemServicoId(), extractValor(event.payload()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORCAMENTO_APROVADO)
    public void onOrcamentoAprovado(OficinaEvent event) {
        log.info("Received OrcamentoAprovado for OS {}", event.ordemServicoId());
        sagaOrchestrator.onOrcamentoDecision(event.ordemServicoId(), true);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORCAMENTO_REJEITADO)
    public void onOrcamentoRejeitado(OficinaEvent event) {
        log.info("Received OrcamentoRejeitado for OS {}", event.ordemServicoId());
        sagaOrchestrator.onOrcamentoRejeitado(event.ordemServicoId(), extractMotivo(event.payload()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PAGAMENTO_APROVADO)
    public void onPagamentoAprovado(OficinaEvent event) {
        log.info("Received PagamentoAprovado for OS {}", event.ordemServicoId());
        sagaOrchestrator.onPagamentoAprovado(event.ordemServicoId());
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_PAGAMENTO_FALHOU)
    public void onPagamentoFalhou(OficinaEvent event) {
        log.info("Received PagamentoFalhou for OS {}", event.ordemServicoId());
        sagaOrchestrator.onPagamentoFalhou(event.ordemServicoId(), extractMotivo(event.payload()));
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_EXECUCAO_INICIADA)
    public void onExecucaoIniciada(OficinaEvent event) {
        log.info("Received ExecucaoIniciada for OS {}", event.ordemServicoId());
        sagaOrchestrator.onExecucaoIniciada(event.ordemServicoId());
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_EXECUCAO_FINALIZADA)
    public void onExecucaoFinalizada(OficinaEvent event) {
        log.info("Received ExecucaoFinalizada for OS {}", event.ordemServicoId());
        sagaOrchestrator.onExecucaoFinalizada(event.ordemServicoId());
    }

    private BigDecimal extractValor(Map<String, Object> payload) {
        if (payload == null) {
            return BigDecimal.ZERO;
        }
        Object value = payload.containsKey("valorTotal")
                ? payload.get("valorTotal")
                : payload.get("valorOrcamento");
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private String extractMotivo(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("motivo")) {
            return null;
        }
        return String.valueOf(payload.get("motivo"));
    }
}
