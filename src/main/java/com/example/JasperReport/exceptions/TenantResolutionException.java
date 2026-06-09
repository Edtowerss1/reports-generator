package com.example.JasperReport.exceptions;

public class TenantResolutionException extends RuntimeException {

    public TenantResolutionException(String message) {
        super(message);
    }

    public TenantResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
