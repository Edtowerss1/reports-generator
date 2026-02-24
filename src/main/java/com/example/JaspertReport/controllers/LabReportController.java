package com.example.JaspertReport.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.services.LabReportService;

@RestController
@RequestMapping("/reportes")
public class LabReportController {

    private final LabReportService labReportService;

    @Value("${service.token}")
    private String serviceToken;

    public LabReportController(LabReportService labReportService) {
        this.labReportService = labReportService;
    }

    @PostMapping("/atencion")
    public ResponseEntity<?> reporteAtencion(
            @RequestHeader("X-Service-Token") String token,
            @RequestBody ReportRequestDTO request) {

        if (!serviceToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request.getSqlEmpresa() == null || request.getSqlEmpresa().isBlank() ||
            request.getSqlAtencion() == null || request.getSqlAtencion().isBlank() ||
            request.getSqlResultados() == null || request.getSqlResultados().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] pdf = labReportService.generarReporteAtencion(request);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=reporte.pdf")
                .body(pdf);
    }
}
