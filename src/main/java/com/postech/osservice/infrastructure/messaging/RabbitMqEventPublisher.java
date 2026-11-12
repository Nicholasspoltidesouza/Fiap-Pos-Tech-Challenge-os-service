package com.postech.osservice.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.postech.osservice.application.messaging.OficinaEvent;
import com.postech.osservice.application.port.EventPublisherPort;

@Component
public class RabbitMqEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitMqEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${oficina.events.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(String routingKey, OficinaEvent event) {
        log.info("Publishing eventType={} routingKey={} osId={}",
                event.eventType(), routingKey, event.ordemServicoId());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
