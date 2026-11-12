package com.postech.osservice.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.postech.osservice.infrastructure.persistence.entity.OrdemServicoStatusHistoricoEntity;

public interface OrdemServicoStatusHistoricoJpaRepository
        extends JpaRepository<OrdemServicoStatusHistoricoEntity, UUID> {

    List<OrdemServicoStatusHistoricoEntity> findByOrdemServicoIdOrderByAlteradoEmAsc(UUID ordemServicoId);
}
