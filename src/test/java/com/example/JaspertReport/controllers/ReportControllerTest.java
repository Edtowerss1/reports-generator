package com.example.JaspertReport.controllers;

import com.example.JaspertReport.dtos.PrintRequestDTO;
import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.dtos.ReportResult;
import com.example.JaspertReport.services.ReportOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that ReportController no longer performs inline token validation
 * (that responsibility moved to TokenValidator + TenantContextInitializer
 * interceptors in Phase 3). The controller only validates the request body
 * and delegates to the orchestrator.
 */
class ReportControllerTest {

    private final StubReportOrchestrator orchestrator = new StubReportOrchestrator();
    private ReportController controller;

    @BeforeEach
    void setUp() {
        controller = new ReportController(orchestrator);
    }

    @Test
    void shouldDelegateToOrchestratorOnValidRequest() {
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setFormat("PDF");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        ResponseEntity<?> response = controller.generar("any-token", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(orchestrator.wasGenerateCalled);
    }

    @Test
    void shouldReturnBadRequestWhenReportNameIsMissing() {
        var request = new ReportRequestDTO();
        request.setFormat("PDF");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        ResponseEntity<?> response = controller.generar("any-token", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(orchestrator.wasGenerateCalled);
    }

    @Test
    void shouldReturnBadRequestWhenFormatIsMissing() {
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        ResponseEntity<?> response = controller.generar("any-token", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenQueriesAreEmpty() {
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setFormat("PDF");
        request.setQueries(List.of());

        ResponseEntity<?> response = controller.generar("any-token", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenQueryHasNoParam() {
        var q = createQuery("", "SELECT 1");
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setFormat("PDF");
        request.setQueries(List.of(q));

        ResponseEntity<?> response = controller.generar("any-token", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldDelegateToOrchestratorOnValidPrintRequest() {
        var request = new PrintRequestDTO();
        request.setReportName("ventas");
        request.setPrinterName("Printer1");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        ResponseEntity<?> response = controller.imprimir("any-token", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(orchestrator.wasPrintCalled);
    }

    @Test
    void shouldReturnBadRequestWhenPrinterNameIsMissing() {
        var request = new PrintRequestDTO();
        request.setReportName("ventas");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        ResponseEntity<?> response = controller.imprimir("any-token", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(orchestrator.wasPrintCalled);
    }

    @Test
    void shouldAcceptAnyTokenValue() {
        // Token validation is done by interceptors, not the controller
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setFormat("PDF");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        ResponseEntity<?> response = controller.generar("any-value-at-all", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private static com.example.JaspertReport.dtos.QueryParamDTO createQuery(String param, String query) {
        var q = new com.example.JaspertReport.dtos.QueryParamDTO();
        q.setParam(param);
        q.setQuery(query);
        return q;
    }

    // --- Test Double ---

    static class StubReportOrchestrator extends ReportOrchestrator {
        boolean wasGenerateCalled;
        boolean wasPrintCalled;

        StubReportOrchestrator() {
            super(null, null, null, null);
        }

        @Override
        public ReportResult generate(ReportRequestDTO request) {
            wasGenerateCalled = true;
            return new ReportResult(new byte[]{1, 2, 3}, "application/pdf", "pdf");
        }

        @Override
        public void print(PrintRequestDTO request) {
            wasPrintCalled = true;
        }
    }
}
