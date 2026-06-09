package com.example.JaspertReport.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleTenantResolutionExceptionWith401() {
        var exception = new TenantResolutionException("Token desconocido: tok-X");
        ResponseEntity<String> response = handler.handleTenantResolution(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token desconocido: tok-X", response.getBody());
    }

    @Test
    void shouldHandleReportNotAllowedExceptionWith403() {
        var exception = new ReportNotAllowedException("acme", "nomina");
        ResponseEntity<String> response = handler.handleReportNotAllowed(exception);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().contains("acme"));
        assertTrue(response.getBody().contains("nomina"));
    }
}
