package com.postech.osservice.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.StatusOrdemServico;

public record OrdemServicoResponse(
        UUID id,
        UUID clienteId,
        UUID veiculoId,
        String descricaoProblema,
        StatusOrdemServico status,
        BigDecimal valorEstimado,
        BigDecimal valorOrcamento,
        Boolean orcamentoAprovado,
        LocalDateTime dataAbertura,
        LocalDateTime dataFinalizacao,
        LocalDateTime atualizadoEm) {

    public static OrdemServicoResponse from(OrdemServico ordem) {
        return new OrdemServicoResponse(
                ordem.getId(),
                ordem.getClienteId(),
                ordem.getVeiculoId(),
                ordem.getDescricaoProblema(),
                ordem.getStatus(),
                ordem.getValorEstimado(),
                ordem.getValorOrcamento(),
                ordem.getOrcamentoAprovado(),
                ordem.getDataAbertura(),
                ordem.getDataFinalizacao(),
                ordem.getAtualizadoEm());
    }
}
