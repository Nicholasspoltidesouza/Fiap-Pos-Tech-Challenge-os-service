package com.postech.osservice.domain.exception;

import java.util.List;

import com.postech.osservice.domain.model.StatusOrdemServico;

public class TransicaoStatusInvalidaException extends RuntimeException {

    public TransicaoStatusInvalidaException(
            String acao,
            StatusOrdemServico atual,
            List<StatusOrdemServico> permitidos) {
        super("Invalid transition for action '%s'. Current status: %s. Allowed: %s"
                .formatted(acao, atual, permitidos));
    }
}
