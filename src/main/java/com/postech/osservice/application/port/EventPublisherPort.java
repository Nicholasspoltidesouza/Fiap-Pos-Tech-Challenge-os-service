package com.postech.osservice.application.port;

import com.postech.osservice.application.messaging.OficinaEvent;

public interface EventPublisherPort {

    void publish(String routingKey, OficinaEvent event);
}
