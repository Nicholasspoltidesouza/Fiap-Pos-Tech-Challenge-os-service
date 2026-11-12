package com.postech.osservice.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.postech.osservice.application.dto.AprovarOrcamentoRequest;
import com.postech.osservice.application.dto.CriarOrdemServicoRequest;
import com.postech.osservice.application.dto.HistoricoStatusResponse;
import com.postech.osservice.application.dto.OrdemServicoResponse;
import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.application.saga.SagaOrchestrator;
import com.postech.osservice.domain.exception.OrdemServicoNaoEncontradaException;
import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.StatusOrdemServico;

@ExtendWith(MockitoExtension.class)
class OrdemServicoUseCaseTest {

    @Mock
    private OrdemServicoRepositoryPort ordemServicoRepository;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    private OrdemServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new OrdemServicoUseCase(ordemServicoRepository, sagaOrchestrator);
    }

    @Test
    void shouldCreateViaSaga() {
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        CriarOrdemServicoRequest request = new CriarOrdemServicoRequest(
                clienteId, veiculoId, "Oil leak", new BigDecimal("80"));

        OrdemServico started = OrdemServico.criar(clienteId, veiculoId, "Oil leak", new BigDecimal("80"));
        started.solicitarOrcamento();
        when(sagaOrchestrator.startSaga(any(OrdemServico.class))).thenReturn(started);

        OrdemServicoResponse response = useCase.criar(request);

        assertEquals(StatusOrdemServico.AGUARDANDO_ORCAMENTO, response.status());
        assertEquals(clienteId, response.clienteId());
        verify(sagaOrchestrator).startSaga(any(OrdemServico.class));
    }

    @Test
    void shouldFindById() {
        OrdemServico ordem = OrdemServico.criar(UUID.randomUUID(), UUID.randomUUID(), "Noise", null);
        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));

        OrdemServicoResponse response = useCase.buscarPorId(ordem.getId());
        assertEquals(ordem.getId(), response.id());
    }

    @Test
    void shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(ordemServicoRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(OrdemServicoNaoEncontradaException.class, () -> useCase.buscarPorId(id));
    }

    @Test
    void shouldListOrders() {
        OrdemServico ordem = OrdemServico.criar(UUID.randomUUID(), UUID.randomUUID(), "Noise", null);
        when(ordemServicoRepository.findAll()).thenReturn(List.of(ordem));
        assertEquals(1, useCase.listar().size());
    }

    @Test
    void shouldReturnHistory() {
        OrdemServico ordem = OrdemServico.criar(UUID.randomUUID(), UUID.randomUUID(), "Noise", null);
        when(ordemServicoRepository.findById(ordem.getId())).thenReturn(Optional.of(ordem));
        when(ordemServicoRepository.findStatusHistory(ordem.getId())).thenReturn(List.of(
                new OrdemServicoRepositoryPort.StatusHistoryEntry(
                        StatusOrdemServico.RECEBIDA, "created", LocalDateTime.now())));

        List<HistoricoStatusResponse> historico = useCase.historico(ordem.getId());
        assertEquals(1, historico.size());
        assertEquals(StatusOrdemServico.RECEBIDA, historico.getFirst().status());
    }

    @Test
    void shouldApproveQuote() {
        OrdemServico ordem = OrdemServico.criar(UUID.randomUUID(), UUID.randomUUID(), "Noise", null);
        when(sagaOrchestrator.onOrcamentoDecision(eq(ordem.getId()), eq(true))).thenReturn(ordem);

        OrdemServicoResponse response = useCase.aprovarOrcamento(ordem.getId(), new AprovarOrcamentoRequest(true));
        assertEquals(ordem.getId(), response.id());
    }

    @Test
    void shouldDeliver() {
        OrdemServico ordem = OrdemServico.criar(UUID.randomUUID(), UUID.randomUUID(), "Noise", null);
        when(sagaOrchestrator.entregar(ordem.getId())).thenReturn(ordem);
        assertEquals(ordem.getId(), useCase.entregar(ordem.getId()).id());
    }
}
