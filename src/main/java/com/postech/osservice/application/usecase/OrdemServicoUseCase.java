package com.postech.osservice.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.postech.osservice.application.dto.AprovarOrcamentoRequest;
import com.postech.osservice.application.dto.CriarOrdemServicoRequest;
import com.postech.osservice.application.dto.HistoricoStatusResponse;
import com.postech.osservice.application.dto.OrdemServicoResponse;
import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.application.saga.SagaOrchestrator;
import com.postech.osservice.domain.exception.OrdemServicoNaoEncontradaException;
import com.postech.osservice.domain.model.OrdemServico;

@Service
public class OrdemServicoUseCase {

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final SagaOrchestrator sagaOrchestrator;

    public OrdemServicoUseCase(
            OrdemServicoRepositoryPort ordemServicoRepository,
            SagaOrchestrator sagaOrchestrator) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public OrdemServicoResponse criar(CriarOrdemServicoRequest request) {
        OrdemServico ordem = OrdemServico.criar(
                request.clienteId(),
                request.veiculoId(),
                request.descricaoProblema(),
                request.valorEstimado());
        OrdemServico started = sagaOrchestrator.startSaga(ordem);
        return OrdemServicoResponse.from(started);
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(UUID id) {
        return ordemServicoRepository.findById(id)
                .map(OrdemServicoResponse::from)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar() {
        return ordemServicoRepository.findAll().stream()
                .map(OrdemServicoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> historico(UUID id) {
        if (ordemServicoRepository.findById(id).isEmpty()) {
            throw new OrdemServicoNaoEncontradaException(id);
        }
        return ordemServicoRepository.findStatusHistory(id).stream()
                .map(HistoricoStatusResponse::from)
                .toList();
    }

    @Transactional
    public OrdemServicoResponse aprovarOrcamento(UUID id, AprovarOrcamentoRequest request) {
        OrdemServico updated = sagaOrchestrator.onOrcamentoDecision(id, Boolean.TRUE.equals(request.aprovado()));
        return OrdemServicoResponse.from(updated);
    }

    @Transactional
    public OrdemServicoResponse entregar(UUID id) {
        return OrdemServicoResponse.from(sagaOrchestrator.entregar(id));
    }
}
