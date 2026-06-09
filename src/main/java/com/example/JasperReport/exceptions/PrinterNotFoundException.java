package com.example.JasperReport.exceptions;

public class PrinterNotFoundException extends RuntimeException {
	public PrinterNotFoundException(String printerName) {
		super("Impresora no encontrada: " + printerName);
	}
}
