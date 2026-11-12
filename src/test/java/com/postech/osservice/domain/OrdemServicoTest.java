package com.postech.osservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.postech.osservice.domain.exception.TransicaoStatusInvalidaException;
import com.postech.osservice.domain.model.OrdemServico;
import com.postech.osservice.domain.model.StatusOrdemServico;

class OrdemServicoTest {

    @Test
    void shouldCreateWithRecebidaStatus() {
        OrdemServico ordem = sampleOrder();
        assertEquals(StatusOrdemServico.RECEBIDA, ordem.getStatus());
        assertTrue(ordem.getStatus().isActive());
    }

    @Test
    void shouldFollowHappyPathUntilEntrega() {
        OrdemServico ordem = sampleOrder();
        ordem.solicitarOrcamento();
        assertEquals(StatusOrdemServico.AGUARDANDO_ORCAMENTO, ordem.getStatus());

        ordem.registrarOrcamentoGerado(new BigDecimal("350.00"));
        assertEquals(StatusOrdemServico.AGUARDANDO_APROVACAO, ordem.getStatus());
        assertEquals(new BigDecimal("350.00"), ordem.getValorOrcamento());

        ordem.aprovarOrcamento(true);
        assertEquals(StatusOrdemServico.AGUARDANDO_PAGAMENTO, ordem.getStatus());
        assertTrue(ordem.getOrcamentoAprovado());

        ordem.registrarPagamentoAprovado();
        assertEquals(StatusOrdemServico.EM_EXECUCAO, ordem.getStatus());

        ordem.finalizar();
        assertEquals(StatusOrdemServico.FINALIZADA, ordem.getStatus());

        ordem.entregar();
        assertEquals(StatusOrdemServico.ENTREGUE, ordem.getStatus());
        assertTrue(ordem.getStatus().isTerminal());
    }

    @Test
    void shouldCancelWhenQuoteRejected() {
        OrdemServico ordem = sampleOrder();
        ordem.solicitarOrcamento();
        ordem.registrarOrcamentoGerado(BigDecimal.TEN);
        ordem.aprovarOrcamento(false);
        assertEquals(StatusOrdemServico.CANCELADA, ordem.getStatus());
    }

    @Test
    void shouldRejectInvalidTransition() {
        OrdemServico ordem = sampleOrder();
        assertThrows(TransicaoStatusInvalidaException.class, ordem::finalizar);
    }

    @Test
    void shouldMarkCompensatedFailure() {
        OrdemServico ordem = sampleOrder();
        ordem.marcarFalhaCompensada();
        assertEquals(StatusOrdemServico.FALHA_COMPENSADA, ordem.getStatus());
    }

    private OrdemServico sampleOrder() {
        return OrdemServico.criar(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Noise when braking",
                new BigDecimal("100.00"));
    }
}
