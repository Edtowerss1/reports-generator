package com.example.JasperReport.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ReportNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<String> handleInvalidFormat(InvalidFormatException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<String> handleGenerationError(ReportGenerationException e) {
        log.error("Error de generación de reporte", e);
        String rootCause = e.getCause() != null && e.getCause().getMessage() != null
                ? " | Causa: " + e.getCause().getMessage()
                : "";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage() + rootCause);
    }

    @ExceptionHandler(PrinterNotFoundException.class)
    public ResponseEntity<String> handlePrinterNotFound(PrinterNotFoundException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(ReportPrintException.class)
    public ResponseEntity<String> handlePrintError(ReportPrintException e) {
        log.error("Error de impresión de reporte", e);
        String rootCause = e.getCause() != null && e.getCause().getMessage() != null
                ? " | Causa: " + e.getCause().getMessage()
                : "";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage() + rootCause);
    }

    @ExceptionHandler(TenantResolutionException.class)
    public ResponseEntity<String> handleTenantResolution(TenantResolutionException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(ReportNotAllowedException.class)
    public ResponseEntity<String> handleReportNotAllowed(ReportNotAllowedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}
