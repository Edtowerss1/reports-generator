package com.example.JaspertReport.tenant;

import java.util.Set;

public record Tenant(
    String id,
    String reportesRuta,
    Set<String> allowedReports,
    Set<String> allowedFormats,
    Datasource datasource
) {
    public record Datasource(
        String url,
        String username,
        String password,
        String driverClassName
    ) {}
}
