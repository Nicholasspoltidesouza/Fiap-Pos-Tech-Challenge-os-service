package com.postech.osservice.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.postech.osservice.domain.exception.TransicaoStatusInvalidaException;

public class OrdemServico {

    private final UUID id;
    private final UUID clienteId;
    private final UUID veiculoId;
    private final String descricaoProblema;
    private StatusOrdemServico status;
    private BigDecimal valorEstimado;
    private BigDecimal valorOrcamento;
    private Boolean orcamentoAprovado;
    private final LocalDateTime dataAbertura;
    private LocalDateTime dataFinalizacao;
    private LocalDateTime atualizadoEm;

    private OrdemServico(
            UUID id,
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            StatusOrdemServico status,
            BigDecimal valorEstimado,
            BigDecimal valorOrcamento,
            Boolean orcamentoAprovado,
            LocalDateTime dataAbertura,
            LocalDateTime dataFinalizacao,
            LocalDateTime atualizadoEm) {
        this.id = id;
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.descricaoProblema = descricaoProblema;
        this.status = status;
        this.valorEstimado = valorEstimado;
        this.valorOrcamento = valorOrcamento;
        this.orcamentoAprovado = orcamentoAprovado;
        this.dataAbertura = dataAbertura;
        this.dataFinalizacao = dataFinalizacao;
        this.atualizadoEm = atualizadoEm;
    }

    public static OrdemServico criar(
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            BigDecimal valorEstimado) {
        LocalDateTime now = LocalDateTime.now();
        return new OrdemServico(
                UUID.randomUUID(),
                clienteId,
                veiculoId,
                descricaoProblema,
                StatusOrdemServico.RECEBIDA,
                valorEstimado,
                null,
                null,
                now,
                null,
                now);
    }

    public static OrdemServico reconstituir(
            UUID id,
            UUID clienteId,
            UUID veiculoId,
            String descricaoProblema,
            StatusOrdemServico status,
            BigDecimal valorEstimado,
            BigDecimal valorOrcamento,
            Boolean orcamentoAprovado,
            LocalDateTime dataAbertura,
            LocalDateTime dataFinalizacao,
            LocalDateTime atualizadoEm) {
        return new OrdemServico(
                id,
                clienteId,
                veiculoId,
                descricaoProblema,
                status,
                valorEstimado,
                valorOrcamento,
                orcamentoAprovado,
                dataAbertura,
                dataFinalizacao,
                atualizadoEm);
    }

    public void solicitarOrcamento() {
        garantirStatus("solicitar orcamento", StatusOrdemServico.RECEBIDA);
        this.status = StatusOrdemServico.AGUARDANDO_ORCAMENTO;
        touch();
    }

    public void registrarOrcamentoGerado(BigDecimal valorOrcamento) {
        garantirStatus("registrar orcamento gerado", StatusOrdemServico.AGUARDANDO_ORCAMENTO);
        this.valorOrcamento = valorOrcamento;
        this.status = StatusOrdemServico.AGUARDANDO_APROVACAO;
        touch();
    }

    public void aprovarOrcamento(boolean aprovado) {
        garantirStatus("aprovar orcamento", StatusOrdemServico.AGUARDANDO_APROVACAO);
        this.orcamentoAprovado = aprovado;
        if (aprovado) {
            this.status = StatusOrdemServico.AGUARDANDO_PAGAMENTO;
        } else {
            this.status = StatusOrdemServico.CANCELADA;
        }
        touch();
    }

    public void registrarPagamentoAprovado() {
        garantirStatus("registrar pagamento aprovado", StatusOrdemServico.AGUARDANDO_PAGAMENTO);
        this.status = StatusOrdemServico.EM_EXECUCAO;
        touch();
    }

    public void registrarExecucaoIniciada() {
        garantirStatus("registrar execucao iniciada", StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.AGUARDANDO_PAGAMENTO);
        this.status = StatusOrdemServico.EM_EXECUCAO;
        touch();
    }

    public void finalizar() {
        garantirStatus("finalizar ordem", StatusOrdemServico.EM_EXECUCAO);
        this.status = StatusOrdemServico.FINALIZADA;
        this.dataFinalizacao = LocalDateTime.now();
        touch();
    }

    public void entregar() {
        garantirStatus("entregar ordem", StatusOrdemServico.FINALIZADA);
        this.status = StatusOrdemServico.ENTREGUE;
        touch();
    }

    public void cancelar() {
        if (status.isTerminal()) {
            throw new TransicaoStatusInvalidaException(
                    "cancelar ordem",
                    status,
                    List.of(
                            StatusOrdemServico.RECEBIDA,
                            StatusOrdemServico.AGUARDANDO_ORCAMENTO,
                            StatusOrdemServico.AGUARDANDO_APROVACAO,
                            StatusOrdemServico.AGUARDANDO_PAGAMENTO,
                            StatusOrdemServico.EM_EXECUCAO));
        }
        this.status = StatusOrdemServico.CANCELADA;
        touch();
    }

    public void marcarFalhaCompensada() {
        this.status = StatusOrdemServico.FALHA_COMPENSADA;
        touch();
    }

    private void garantirStatus(String acao, StatusOrdemServico... permitidos) {
        List<StatusOrdemServico> permitidosLista = List.of(permitidos);
        if (!permitidosLista.contains(this.status)) {
            throw new TransicaoStatusInvalidaException(acao, this.status, permitidosLista);
        }
    }

    private void touch() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getVeiculoId() {
        return veiculoId;
    }

    public String getDescricaoProblema() {
        return descricaoProblema;
    }

    public StatusOrdemServico getStatus() {
        return status;
    }

    public BigDecimal getValorEstimado() {
        return valorEstimado;
    }

    public BigDecimal getValorOrcamento() {
        return valorOrcamento;
    }

    public Boolean getOrcamentoAprovado() {
        return orcamentoAprovado;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
