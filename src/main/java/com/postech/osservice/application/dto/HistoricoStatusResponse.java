package com.postech.osservice.application.dto;

import java.time.LocalDateTime;

import com.postech.osservice.application.port.OrdemServicoRepositoryPort;
import com.postech.osservice.domain.model.StatusOrdemServico;

public record HistoricoStatusResponse(
        StatusOrdemServico status,
        String observacao,
        LocalDateTime alteradoEm) {

    public static HistoricoStatusResponse from(OrdemServicoRepositoryPort.StatusHistoryEntry entry) {
        return new HistoricoStatusResponse(entry.status(), entry.observacao(), entry.alteradoEm());
    }
}
