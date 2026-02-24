package com.example.JaspertReport.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.JaspertReport.services.LabReportService;

@RestController
@RequestMapping("/reportes")
public class LabReportController {

    private final LabReportService labReportService;

    public LabReportController(LabReportService labReportService) {
        this.labReportService = labReportService;
    }

        @GetMapping("/atencion/{codAten}")
        public ResponseEntity<byte[]> reporteAtencion(@PathVariable String codAten,
            @RequestParam(value = "row_ids", required = true) String rowIds) {
        byte[] pdf = labReportService.generarReporteAtencion(codAten, rowIds);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=reporte_" + codAten + ".pdf")
                .body(pdf);
    }
}
