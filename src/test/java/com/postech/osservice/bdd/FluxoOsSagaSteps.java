package com.postech.osservice.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.postech.osservice.application.messaging.OficinaEvent;
import com.postech.osservice.application.messaging.RoutingKeys;
import com.postech.osservice.application.port.EventPublisherPort;
import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.application.port.SagaRepositoryPort;
import com.postech.osservice.application.saga.SagaOrchestrator;
import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.SagaState;
import com.postech.osservice.domain.model.SagaStatus;
import com.postech.osservice.domain.model.StatusOrdemServico;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class FluxoOsSagaSteps {

    private OrdemServicoRepositoryPort ordemRepo;
    private SagaRepositoryPort sagaRepo;
    private EventPublisherPort eventPublisher;
    private SagaOrchestrator orchestrator;

    private final Map<UUID, OrdemServico> orders = new ConcurrentHashMap<>();
    private final Map<UUID, SagaState> sagas = new ConcurrentHashMap<>();
    private final List<PublishedEvent> published = new ArrayList<>();

    private OrdemServico currentOrder;
    private SagaState currentSaga;

    @Before
    public void setUp() {
        orders.clear();
        sagas.clear();
        published.clear();

        ordemRepo = mock(OrdemServicoRepositoryPort.class);
        sagaRepo = mock(SagaRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);

        when(ordemRepo.save(any(OrdemServico.class))).thenAnswer(inv -> {
            OrdemServico ordem = inv.getArgument(0);
            orders.put(ordem.getId(), ordem);
            return ordem;
        });
        when(ordemRepo.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(orders.get(inv.getArgument(0))));
        when(sagaRepo.save(any(SagaState.class))).thenAnswer(inv -> {
            SagaState saga = inv.getArgument(0);
            sagas.put(saga.getOrdemServicoId(), saga);
            return saga;
        });
        when(sagaRepo.findByOrdemServicoId(any(UUID.class)))
                .thenAnswer(inv -> Optional.ofNullable(sagas.get(inv.getArgument(0))));

        doAnswer(inv -> {
            published.add(new PublishedEvent(inv.getArgument(0), inv.getArgument(1)));
            return null;
        }).when(eventPublisher).publish(any(), any());

        orchestrator = new SagaOrchestrator(ordemRepo, sagaRepo, eventPublisher);
    }

    @Given("uma nova ordem de servico para o cliente")
    public void umaNovaOrdem() {
        currentOrder = OrdemServico.criar(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Ruido no motor",
                new BigDecimal("120.00"));
    }

    @When("a saga e iniciada")
    public void sagaIniciada() {
        currentOrder = orchestrator.startSaga(currentOrder);
        currentSaga = sagas.get(currentOrder.getId());
    }

    @When("o orcamento e gerado com valor {double}")
    public void orcamentoGerado(Double valor) {
        orchestrator.onOrcamentoGerado(currentOrder.getId(), BigDecimal.valueOf(valor));
        currentOrder = orders.get(currentOrder.getId());
        currentSaga = sagas.get(currentOrder.getId());
    }

    @When("o cliente aprova o orcamento")
    public void clienteAprova() {
        currentOrder = orchestrator.onOrcamentoDecision(currentOrder.getId(), true);
        currentSaga = sagas.get(currentOrder.getId());
    }

    @When("o pagamento e aprovado")
    public void pagamentoAprovado() {
        orchestrator.onPagamentoAprovado(currentOrder.getId());
        currentOrder = orders.get(currentOrder.getId());
        currentSaga = sagas.get(currentOrder.getId());
    }

    @When("o pagamento falha com motivo {string}")
    public void pagamentoFalhou(String motivo) {
        orchestrator.onPagamentoFalhou(currentOrder.getId(), motivo);
        currentOrder = orders.get(currentOrder.getId());
        currentSaga = sagas.get(currentOrder.getId());
    }

    @When("a execucao e finalizada")
    public void execucaoFinalizada() {
        orchestrator.onExecucaoFinalizada(currentOrder.getId());
        currentOrder = orders.get(currentOrder.getId());
        currentSaga = sagas.get(currentOrder.getId());
    }

    @Then("o status da OS deve ser {string}")
    public void statusDeveSer(String status) {
        assertEquals(StatusOrdemServico.valueOf(status), currentOrder.getStatus());
    }

    @Then("o evento de geracao de orcamento deve ter sido publicado")
    public void eventoGeracaoOrcamento() {
        assertTrue(published.stream().anyMatch(e -> RoutingKeys.ORCAMENTO_GERAR.equals(e.routingKey())));
    }

    @Then("o evento de solicitacao de pagamento deve ter sido publicado")
    public void eventoPagamento() {
        assertTrue(published.stream().anyMatch(e -> RoutingKeys.PAGAMENTO_SOLICITAR.equals(e.routingKey())));
    }

    @Then("o evento de inicio de execucao deve ter sido publicado")
    public void eventoExecucao() {
        assertTrue(published.stream().anyMatch(e -> RoutingKeys.EXECUCAO_INICIAR.equals(e.routingKey())));
    }

    @Then("a saga deve estar COMPLETED")
    public void sagaCompleted() {
        assertEquals(SagaStatus.COMPLETED, currentSaga.getStatus());
    }

    @Then("eventos de compensacao devem ter sido publicados")
    public void eventosCompensacao() {
        assertTrue(published.stream().anyMatch(e -> RoutingKeys.COMPENSAR_ORCAMENTO.equals(e.routingKey())));
        assertTrue(published.stream().anyMatch(e -> RoutingKeys.COMPENSAR_PAGAMENTO.equals(e.routingKey())));
        verify(eventPublisher, atLeastOnce()).publish(any(), any());
    }

    private record PublishedEvent(String routingKey, OficinaEvent event) {
    }
}
