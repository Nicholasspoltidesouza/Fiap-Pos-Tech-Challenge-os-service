package com.postech.osservice.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.postech.osservice.domain.model.SagaStatus;
import com.postech.osservice.domain.model.SagaStep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "saga_instance")
@Getter
@Setter
public class SagaInstanceEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID ordemServicoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SagaStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SagaStatus status;

    @Column(length = 2000)
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
