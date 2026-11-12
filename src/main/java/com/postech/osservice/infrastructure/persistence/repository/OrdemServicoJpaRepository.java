package com.postech.osservice.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.postech.osservice.infrastructure.persistence.entity.OrdemServicoEntity;

public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoEntity, UUID> {
}
