package com.postech.osservice.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.postech.osservice.domain.model.StatusOrdemServico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ordem_servico_status_historico")
@Getter
@Setter
public class OrdemServicoStatusHistoricoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ordemServicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusOrdemServico status;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false)
    private LocalDateTime alteradoEm;
}
