package com.postech.osservice.domain.model;

public enum SagaStatus {
    RUNNING,
    COMPLETED,
    COMPENSATING,
    FAILED,
    COMPENSATED
}
