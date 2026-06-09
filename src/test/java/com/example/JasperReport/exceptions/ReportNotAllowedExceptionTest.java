package com.example.JasperReport.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class ReportNotAllowedExceptionTest {

    @Test
    void shouldExtendRuntimeException() {
        var exception = new ReportNotAllowedException("acme", "nomina");
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void shouldPreserveMessage() {
        var tenantId = "acme";
        var reportName = "nomina";
        var exception = new ReportNotAllowedException(tenantId, reportName);
        assertTrue(exception.getMessage().contains(tenantId));
        assertTrue(exception.getMessage().contains(reportName));
    }

    @Test
    void shouldPreserveMessageAndCause() {
        var cause = new IllegalArgumentException("root cause");
        var exception = new ReportNotAllowedException("Reporte no permitido", cause);
        assertEquals("Reporte no permitido", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
