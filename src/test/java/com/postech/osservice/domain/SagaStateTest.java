package com.postech.osservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.postech.osservice.domain.model.SagaState;
import com.postech.osservice.domain.model.SagaStatus;
import com.postech.osservice.domain.model.SagaStep;

class SagaStateTest {

    @Test
    void shouldStartRunning() {
        UUID osId = UUID.randomUUID();
        SagaState saga = SagaState.start(osId);
        assertEquals(osId, saga.getOrdemServicoId());
        assertEquals(SagaStep.CREATED, saga.getCurrentStep());
        assertEquals(SagaStatus.RUNNING, saga.getStatus());
        assertNull(saga.getLastError());
    }

    @Test
    void shouldAdvanceAndComplete() {
        SagaState saga = SagaState.start(UUID.randomUUID());
        saga.advanceTo(SagaStep.QUOTE_REQUESTED);
        assertEquals(SagaStep.QUOTE_REQUESTED, saga.getCurrentStep());
        saga.advanceTo(SagaStep.COMPLETED);
        assertEquals(SagaStatus.COMPLETED, saga.getStatus());
    }

    @Test
    void shouldCompensate() {
        SagaState saga = SagaState.start(UUID.randomUUID());
        saga.markCompensating("payment failed");
        assertEquals(SagaStatus.COMPENSATING, saga.getStatus());
        assertEquals("payment failed", saga.getLastError());
        saga.markCompensated();
        assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        assertEquals(SagaStep.COMPENSATED, saga.getCurrentStep());
    }

    @Test
    void shouldMarkFailed() {
        SagaState saga = SagaState.start(UUID.randomUUID());
        saga.markFailed("unexpected");
        assertEquals(SagaStatus.FAILED, saga.getStatus());
        assertEquals("unexpected", saga.getLastError());
    }
}
