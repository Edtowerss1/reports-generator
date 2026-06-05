package com.example.JaspertReport.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantResolutionExceptionTest {

    @Test
    void shouldExtendRuntimeException() {
        var exception = new TenantResolutionException("Token inválido");
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void shouldPreserveMessage() {
        var exception = new TenantResolutionException("Token desconocido: tok-X");
        assertEquals("Token desconocido: tok-X", exception.getMessage());
    }

    @Test
    void shouldPreserveMessageAndCause() {
        var cause = new IllegalArgumentException("root cause");
        var exception = new TenantResolutionException("Error de resolución", cause);
        assertEquals("Error de resolución", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
