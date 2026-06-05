package com.example.JaspertReport.services;

import java.nio.file.Path;

/**
 * Resolves a report template path for a given tenant and report name.
 * <p>
 * Single responsibility: maps (tenantId, reportName) to a concrete
 * filesystem path. Implementations control the template source
 * (local filesystem, S3, classpath) — OCP ensures new sources
 * are new implementations, not modifications.
 */
public interface TemplateResolver {

    /**
     * Resolves the filesystem path to a compiled .jasper template
     * for the given tenant and report name.
     *
     * @param tenantId   the tenant identifier
     * @param reportName the report name (without extension)
     * @return the absolute path to the .jasper file
     * @throws com.example.JaspertReport.exceptions.ReportNotFoundException
     *         if the template does not exist for this tenant
     */
    Path resolve(String tenantId, String reportName);
}
