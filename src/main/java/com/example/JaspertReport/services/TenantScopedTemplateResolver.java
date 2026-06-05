package com.example.JaspertReport.services;

import com.example.JaspertReport.config.TenantProperties;
import com.example.JaspertReport.exceptions.ReportNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves .jasper template paths from a tenant-specific root directory.
 * <p>
 * Constructs the path as {@code tenant.reportesRuta / reportName.jasper}
 * and validates the file exists. No fallback to a shared directory —
 * each tenant has isolated templates (spec T1, T3).
 */
public class TenantScopedTemplateResolver implements TemplateResolver {

    private final Map<String, String> tenantIdToRuta;

    public TenantScopedTemplateResolver(TenantProperties tenantProperties) {
        this.tenantIdToRuta = tenantProperties.getTenants().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getReportesRuta()
            ));
    }

    @Override
    public Path resolve(String tenantId, String reportName) {
        String ruta = tenantIdToRuta.get(tenantId);
        if (ruta == null) {
            throw new ReportNotFoundException(reportName);
        }

        // Ensure trailing slash for clean path construction
        String normalizedRuta = ruta.endsWith("/") ? ruta : ruta + "/";
        Path jasperPath = Path.of(normalizedRuta + reportName + ".jasper").toAbsolutePath().normalize();
        Path jrxmlPath = Path.of(normalizedRuta + reportName + ".jrxml");

        // Support lazy compilation: template exists if either .jasper or .jrxml is present
        if (!Files.exists(jasperPath) && !Files.exists(jrxmlPath)) {
            throw new ReportNotFoundException(reportName);
        }

        return jasperPath;
    }
}
