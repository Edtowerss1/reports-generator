package com.example.JaspertReport.exceptions;

public class InvalidFormatException extends RuntimeException {
    public InvalidFormatException(String format) {
        super("Formato no soportado: '" + format + "'. Formatos válidos: PDF, XLSX");
    }
}
