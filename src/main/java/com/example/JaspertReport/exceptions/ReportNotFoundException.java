package com.example.JaspertReport.exceptions;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(String reportName) {
        super("Reporte no encontrado: " + reportName + ".jasper");
    }
}
