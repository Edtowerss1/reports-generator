package com.example.JaspertReport.controllers;

import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.dtos.ReportResult;
import com.example.JaspertReport.services.ReportOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes")
public class ReportController {

    private final ReportOrchestrator reportOrchestrator;

    @Value("${service.token}")
    private String serviceToken;

    public ReportController(ReportOrchestrator reportOrchestrator) {
        this.reportOrchestrator = reportOrchestrator;
    }

    @PostMapping("/generar")
    public ResponseEntity<?> generar(
            @RequestHeader("X-Service-Token") String token,
            @RequestBody ReportRequestDTO request) {

        if (!serviceToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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

    private boolean isValidRequest(ReportRequestDTO request) {
        if (request.getReportName() == null || request.getReportName().isBlank()) return false;
        if (request.getFormat() == null || request.getFormat().isBlank()) return false;
        if (request.getQueries() == null || request.getQueries().isEmpty()) return false;
        return request.getQueries().stream().allMatch(q ->
                q.getParam() != null && !q.getParam().isBlank()
                && q.getQuery() != null && !q.getQuery().isBlank()
        );
    }
}
