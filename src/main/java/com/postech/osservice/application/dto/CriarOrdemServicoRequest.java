package com.postech.osservice.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CriarOrdemServicoRequest", example = """
        {
          "clienteId": "11111111-1111-1111-1111-111111111111",
          "veiculoId": "22222222-2222-2222-2222-222222222222",
          "descricaoProblema": "Barulho no motor ao acelerar",
          "valorEstimado": 350.00
        }
        """)
public record CriarOrdemServicoRequest(
        @NotNull
        @Schema(example = "11111111-1111-1111-1111-111111111111")
        UUID clienteId,
        @NotNull
        @Schema(example = "22222222-2222-2222-2222-222222222222")
        UUID veiculoId,
        @NotBlank
        @Schema(example = "Barulho no motor ao acelerar")
        String descricaoProblema,
        @Schema(example = "350.00")
        BigDecimal valorEstimado) {
}
