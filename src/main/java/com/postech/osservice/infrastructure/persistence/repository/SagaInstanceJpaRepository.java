package com.postech.osservice.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.postech.osservice.infrastructure.persistence.entity.SagaInstanceEntity;

public interface SagaInstanceJpaRepository extends JpaRepository<SagaInstanceEntity, UUID> {

    Optional<SagaInstanceEntity> findByOrdemServicoId(UUID ordemServicoId);
}
