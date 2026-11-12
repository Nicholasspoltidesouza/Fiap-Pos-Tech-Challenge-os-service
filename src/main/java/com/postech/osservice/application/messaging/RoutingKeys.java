package com.postech.osservice.application.messaging;

public final class RoutingKeys {

    public static final String ORDEM_CRIADA = "ordem.criada";
    public static final String ORCAMENTO_GERAR = "orcamento.gerar";
    public static final String PAGAMENTO_SOLICITAR = "pagamento.solicitar";
    public static final String EXECUCAO_INICIAR = "execucao.iniciar";
    public static final String COMPENSAR_ORCAMENTO = "compensar.orcamento";
    public static final String COMPENSAR_PAGAMENTO = "compensar.pagamento";
    public static final String COMPENSAR_EXECUCAO = "compensar.execucao";

    private RoutingKeys() {
    }
}
