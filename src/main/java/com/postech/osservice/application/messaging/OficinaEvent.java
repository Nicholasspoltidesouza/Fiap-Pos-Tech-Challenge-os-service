package com.postech.osservice.application.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OficinaEvent(
        UUID eventId,
        String eventType,
        Instant ocorridoEm,
        UUID ordemServicoId,
        UUID clienteId,
        Map<String, Object> payload) {

    public static OficinaEvent of(
            String eventType,
            UUID ordemServicoId,
            UUID clienteId,
            Map<String, Object> payload) {
        return new OficinaEvent(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                ordemServicoId,
                clienteId,
                payload == null ? Map.of() : payload);
    }
}
