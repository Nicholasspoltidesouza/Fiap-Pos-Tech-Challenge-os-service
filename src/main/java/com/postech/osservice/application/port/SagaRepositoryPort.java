package com.postech.osservice.application.port;

import java.util.Optional;
import java.util.UUID;

import com.postech.osservice.domain.model.SagaState;

public interface SagaRepositoryPort {

    SagaState save(SagaState sagaState);

    Optional<SagaState> findByOrdemServicoId(UUID ordemServicoId);
}
