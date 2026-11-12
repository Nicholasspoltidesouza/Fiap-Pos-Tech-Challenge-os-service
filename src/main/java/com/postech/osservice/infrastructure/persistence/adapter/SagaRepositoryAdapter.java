package com.postech.osservice.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.postech.osservice.application.port.SagaRepositoryPort;
import com.postech.osservice.domain.model.SagaState;
import com.postech.osservice.infrastructure.persistence.entity.SagaInstanceEntity;
import com.postech.osservice.infrastructure.persistence.repository.SagaInstanceJpaRepository;

@Component
public class SagaRepositoryAdapter implements SagaRepositoryPort {

    private final SagaInstanceJpaRepository sagaInstanceJpaRepository;

    public SagaRepositoryAdapter(SagaInstanceJpaRepository sagaInstanceJpaRepository) {
        this.sagaInstanceJpaRepository = sagaInstanceJpaRepository;
    }

    @Override
    public SagaState save(SagaState sagaState) {
        SagaInstanceEntity entity = toEntity(sagaState);
        return toDomain(sagaInstanceJpaRepository.save(entity));
    }

    @Override
    public Optional<SagaState> findByOrdemServicoId(UUID ordemServicoId) {
        return sagaInstanceJpaRepository.findByOrdemServicoId(ordemServicoId).map(this::toDomain);
    }

    private SagaInstanceEntity toEntity(SagaState saga) {
        SagaInstanceEntity entity = new SagaInstanceEntity();
        entity.setId(saga.getId());
        entity.setOrdemServicoId(saga.getOrdemServicoId());
        entity.setCurrentStep(saga.getCurrentStep());
        entity.setStatus(saga.getStatus());
        entity.setLastError(saga.getLastError());
        entity.setCreatedAt(saga.getCreatedAt());
        entity.setUpdatedAt(saga.getUpdatedAt());
        return entity;
    }

    private SagaState toDomain(SagaInstanceEntity entity) {
        return SagaState.reconstitute(
                entity.getId(),
                entity.getOrdemServicoId(),
                entity.getCurrentStep(),
                entity.getStatus(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
