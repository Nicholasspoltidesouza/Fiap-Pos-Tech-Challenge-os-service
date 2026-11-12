package com.postech.osservice.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.StatusOrdemServico;

public interface OrdemServicoRepositoryPort {

    OrdemServico save(OrdemServico ordemServico);

    Optional<OrdemServico> findById(UUID id);

    List<OrdemServico> findAll();

    void appendStatusHistory(UUID ordemServicoId, StatusOrdemServico status, String observacao);

    List<StatusHistoryEntry> findStatusHistory(UUID ordemServicoId);

    record StatusHistoryEntry(StatusOrdemServico status, String observacao, java.time.LocalDateTime alteradoEm) {
    }
}
