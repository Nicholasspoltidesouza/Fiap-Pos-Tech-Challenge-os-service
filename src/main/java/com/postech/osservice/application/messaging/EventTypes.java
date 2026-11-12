package com.postech.osservice.application.messaging;

public final class EventTypes {

    public static final String ORDEM_SERVICO_CRIADA = "OrdemServicoCriada";
    public static final String ORCAMENTO_GERADO = "OrcamentoGerado";
    public static final String ORCAMENTO_APROVADO = "OrcamentoAprovado";
    public static final String ORCAMENTO_REJEITADO = "OrcamentoRejeitado";
    public static final String PAGAMENTO_APROVADO = "PagamentoAprovado";
    public static final String PAGAMENTO_FALHOU = "PagamentoFalhou";
    public static final String EXECUCAO_INICIADA = "ExecucaoIniciada";
    public static final String EXECUCAO_FINALIZADA = "ExecucaoFinalizada";
    public static final String COMPENSAR_ORCAMENTO = "CompensarOrcamento";
    public static final String COMPENSAR_PAGAMENTO = "CompensarPagamento";
    public static final String COMPENSAR_EXECUCAO = "CompensarExecucao";

    private EventTypes() {
    }
}
