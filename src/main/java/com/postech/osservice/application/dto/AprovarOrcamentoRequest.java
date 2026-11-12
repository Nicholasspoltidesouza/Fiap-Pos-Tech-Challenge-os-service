package com.postech.osservice.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AprovarOrcamentoRequest", example = """
        {
          "aprovado": true
        }
        """)
public record AprovarOrcamentoRequest(
        @NotNull
        @Schema(description = "true continues saga to payment; false triggers compensation", example = "true")
        Boolean aprovado) {
}
