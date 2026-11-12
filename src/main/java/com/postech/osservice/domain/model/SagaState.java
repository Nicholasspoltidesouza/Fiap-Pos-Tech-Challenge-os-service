package com.postech.osservice.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SagaState {

    private final UUID id;
    private final UUID ordemServicoId;
    private SagaStep currentStep;
    private SagaStatus status;
    private String lastError;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private SagaState(
            UUID id,
            UUID ordemServicoId,
            SagaStep currentStep,
            SagaStatus status,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.currentStep = currentStep;
        this.status = status;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SagaState start(UUID ordemServicoId) {
        LocalDateTime now = LocalDateTime.now();
        return new SagaState(
                UUID.randomUUID(),
                ordemServicoId,
                SagaStep.CREATED,
                SagaStatus.RUNNING,
                null,
                now,
                now);
    }

    public static SagaState reconstitute(
            UUID id,
            UUID ordemServicoId,
            SagaStep currentStep,
            SagaStatus status,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new SagaState(id, ordemServicoId, currentStep, status, lastError, createdAt, updatedAt);
    }

    public void advanceTo(SagaStep step) {
        this.currentStep = step;
        this.updatedAt = LocalDateTime.now();
        if (step == SagaStep.COMPLETED) {
            this.status = SagaStatus.COMPLETED;
        }
    }

    public void markCompensating(String error) {
        this.status = SagaStatus.COMPENSATING;
        this.currentStep = SagaStep.COMPENSATING;
        this.lastError = error;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompensated() {
        this.status = SagaStatus.COMPENSATED;
        this.currentStep = SagaStep.COMPENSATED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.status = SagaStatus.FAILED;
        this.lastError = error;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrdemServicoId() {
        return ordemServicoId;
    }

    public SagaStep getCurrentStep() {
        return currentStep;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
