package com.postech.osservice.domain.exception;

import java.util.UUID;

public class OrdemServicoNaoEncontradaException extends RuntimeException {

    public OrdemServicoNaoEncontradaException(UUID id) {
        super("Service order not found: " + id);
    }
}
