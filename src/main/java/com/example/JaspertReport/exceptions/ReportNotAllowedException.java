package com.example.JaspertReport.exceptions;

public class ReportNotAllowedException extends RuntimeException {

    public ReportNotAllowedException(String tenantId, String reportName) {
        super("Reporte no permitido para tenant '" + tenantId + "': " + reportName);
    }

    public ReportNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
