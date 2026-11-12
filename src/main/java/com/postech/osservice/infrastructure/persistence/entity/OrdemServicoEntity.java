package com.postech.osservice.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.postech.osservice.domain.model.StatusOrdemServico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ordem_servico")
@Getter
@Setter
public class OrdemServicoEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID clienteId;

    @Column(nullable = false)
    private UUID veiculoId;

    @Column(nullable = false, length = 2000)
    private String descricaoProblema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusOrdemServico status;

    @Column(precision = 14, scale = 2)
    private BigDecimal valorEstimado;

    @Column(precision = 14, scale = 2)
    private BigDecimal valorOrcamento;

    private Boolean orcamentoAprovado;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataFinalizacao;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;
}
