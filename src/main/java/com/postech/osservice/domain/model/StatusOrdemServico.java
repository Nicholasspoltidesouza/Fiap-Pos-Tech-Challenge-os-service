package com.postech.osservice.domain.model;

public enum StatusOrdemServico {
    RECEBIDA,
    AGUARDANDO_ORCAMENTO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA,
    FALHA_COMPENSADA;

    public boolean isTerminal() {
        return this == ENTREGUE || this == CANCELADA || this == FALHA_COMPENSADA;
    }

    public boolean isActive() {
        return !isTerminal() && this != FINALIZADA;
    }
}
