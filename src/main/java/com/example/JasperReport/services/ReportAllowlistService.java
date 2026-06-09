package com.example.JasperReport.services;

/**
 * Enforces per-tenant report allowlist.
 * <p>
 * Single responsibility: answers whether a tenant is permitted
 * to generate a specific report. Decouples allowlist policy
 * (OCP) — new rules (time-based, quota-based) are new implementations.
 */
public interface ReportAllowlistService {

    /**
     * Checks whether a tenant is allowed to generate the given report.
     *
     * @param tenantId   the tenant identifier
     * @param reportName the report name to check
     * @return {@code true} if the report is in the tenant's allowlist,
     *         {@code false} if disallowed or tenant unknown
     */
    boolean isAllowed(String tenantId, String reportName);
}
