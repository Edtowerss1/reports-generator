package com.example.JaspertReport.controllers;

import com.example.JaspertReport.dtos.PrintRequestDTO;
import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.dtos.ReportResult;
import com.example.JaspertReport.services.ReportOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin controller that validates request body and delegates to the orchestrator.
 * Authentication is handled by {@code TokenValidator} and
 * {@code TenantContextInitializer} interceptors (registered in WebConfig).
 */
@RestController
@RequestMapping("/reportes")
public class ReportController {

    private final ReportOrchestrator reportOrchestrator;

    public ReportController(ReportOrchestrator reportOrchestrator) {
        this.reportOrchestrator = reportOrchestrator;
    }

    @PostMapping("/generar")
    public ResponseEntity<?> generar(
            @RequestHeader("X-Service-Token") String token,
            @RequestBody ReportRequestDTO request) {

        if (!isValidRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El cuerpo debe incluir reportName, format y al menos una query con param y query.");
        }

        ReportResult result = reportOrchestrator.generate(request);

        return ResponseEntity.ok()
                .header("Content-Type", result.getContentType())
                .header("Content-Disposition", "inline; filename=reporte." + result.getFileExtension())
                .body(result.getContent());
    }

    @PostMapping("/imprimir")
    public ResponseEntity<?> imprimir(
            @RequestHeader("X-Service-Token") String token,
            @RequestBody PrintRequestDTO request) {

        if (!isValidPrintRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El cuerpo debe incluir reportName, printerName y al menos una query con param y query.");
        }

        reportOrchestrator.print(request);
        int copies = request.getCopies() == null || request.getCopies() < 1 ? 1 : request.getCopies();

        return ResponseEntity
                .ok("Reporte enviado a impresión en '" + request.getPrinterName() + "' (copias: " + copies + ").");
    }

    private boolean isValidRequest(ReportRequestDTO request) {
        if (request.getReportName() == null || request.getReportName().isBlank())
            return false;
        if (request.getFormat() == null || request.getFormat().isBlank())
            return false;
        if (request.getQueries() == null || request.getQueries().isEmpty())
            return false;
        return request.getQueries().stream().allMatch(q -> q.getParam() != null && !q.getParam().isBlank()
                && q.getQuery() != null && !q.getQuery().isBlank());
    }

    private boolean isValidPrintRequest(PrintRequestDTO request) {
        if (request.getReportName() == null || request.getReportName().isBlank())
            return false;
        if (request.getPrinterName() == null || request.getPrinterName().isBlank())
            return false;
        if (request.getQueries() == null || request.getQueries().isEmpty())
            return false;
        return request.getQueries().stream().allMatch(q -> q.getParam() != null && !q.getParam().isBlank()
                && q.getQuery() != null && !q.getQuery().isBlank());
    }
}
