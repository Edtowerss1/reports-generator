package com.example.JasperReport.tenant;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Provides tenant-specific JDBC templates for database access.
 * <p>
 * Single responsibility: return the correct JdbcTemplate for a tenant.
 * Decouples datasource routing (OCP) — any compliant implementation
 * (pool-per-tenant, single-pool, mock) satisfies the contract (LSP).
 */
public interface DataSourceProvider {

    /**
     * Returns the JDBC template configured for the given tenant.
     *
     * @param tenantId the tenant identifier
     * @return the tenant-specific JdbcTemplate
     * @throws com.example.JasperReport.exceptions.TenantResolutionException
     *         if the tenant is unknown or no datasource is configured
     */
    JdbcTemplate getTemplate(String tenantId);
}
