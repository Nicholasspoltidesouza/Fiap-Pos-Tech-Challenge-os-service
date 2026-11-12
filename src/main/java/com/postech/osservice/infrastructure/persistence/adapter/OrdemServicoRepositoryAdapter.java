package com.postech.osservice.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.StatusOrdemServico;
import com.postech.osservice.infrastructure.persistence.entity.OrdemServicoEntity;
import com.postech.osservice.infrastructure.persistence.entity.OrdemServicoStatusHistoricoEntity;
import com.postech.osservice.infrastructure.persistence.repository.OrdemServicoJpaRepository;
import com.postech.osservice.infrastructure.persistence.repository.OrdemServicoStatusHistoricoJpaRepository;

@Component
public class OrdemServicoRepositoryAdapter implements OrdemServicoRepositoryPort {

    private final OrdemServicoJpaRepository ordemServicoJpaRepository;
    private final OrdemServicoStatusHistoricoJpaRepository historicoJpaRepository;

    public OrdemServicoRepositoryAdapter(
            OrdemServicoJpaRepository ordemServicoJpaRepository,
            OrdemServicoStatusHistoricoJpaRepository historicoJpaRepository) {
        this.ordemServicoJpaRepository = ordemServicoJpaRepository;
        this.historicoJpaRepository = historicoJpaRepository;
    }

    @Override
    public OrdemServico save(OrdemServico ordemServico) {
        OrdemServicoEntity entity = toEntity(ordemServico);
        return toDomain(ordemServicoJpaRepository.save(entity));
    }

    @Override
    public Optional<OrdemServico> findById(UUID id) {
        return ordemServicoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<OrdemServico> findAll() {
        return ordemServicoJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void appendStatusHistory(UUID ordemServicoId, StatusOrdemServico status, String observacao) {
        OrdemServicoStatusHistoricoEntity historico = new OrdemServicoStatusHistoricoEntity();
        historico.setOrdemServicoId(ordemServicoId);
        historico.setStatus(status);
        historico.setObservacao(observacao);
        historico.setAlteradoEm(LocalDateTime.now());
        historicoJpaRepository.save(historico);
    }

    @Override
    public List<StatusHistoryEntry> findStatusHistory(UUID ordemServicoId) {
        return historicoJpaRepository.findByOrdemServicoIdOrderByAlteradoEmAsc(ordemServicoId).stream()
                .map(h -> new StatusHistoryEntry(h.getStatus(), h.getObservacao(), h.getAlteradoEm()))
                .toList();
    }

    private OrdemServicoEntity toEntity(OrdemServico ordem) {
        OrdemServicoEntity entity = new OrdemServicoEntity();
        entity.setId(ordem.getId());
        entity.setClienteId(ordem.getClienteId());
        entity.setVeiculoId(ordem.getVeiculoId());
        entity.setDescricaoProblema(ordem.getDescricaoProblema());
        entity.setStatus(ordem.getStatus());
        entity.setValorEstimado(ordem.getValorEstimado());
        entity.setValorOrcamento(ordem.getValorOrcamento());
        entity.setOrcamentoAprovado(ordem.getOrcamentoAprovado());
        entity.setDataAbertura(ordem.getDataAbertura());
        entity.setDataFinalizacao(ordem.getDataFinalizacao());
        entity.setAtualizadoEm(ordem.getAtualizadoEm());
        return entity;
    }

    private OrdemServico toDomain(OrdemServicoEntity entity) {
        return OrdemServico.reconstituir(
                entity.getId(),
                entity.getClienteId(),
                entity.getVeiculoId(),
                entity.getDescricaoProblema(),
                entity.getStatus(),
                entity.getValorEstimado(),
                entity.getValorOrcamento(),
                entity.getOrcamentoAprovado(),
                entity.getDataAbertura(),
                entity.getDataFinalizacao(),
                entity.getAtualizadoEm());
    }
}
