package com.example.JasperReport.tenant;

/**
 * Resolves a service token to a Tenant identity.
 * <p>
 * Implementations provide tenant lookup from configuration, database,
 * or any other registry. This is the entry point for tenant resolution
 * in the multi-tenant engine (SRP: single responsibility — resolve only).
 */
public interface TenantResolver {

    /**
     * Resolves a service token to the corresponding Tenant.
     *
     * @param token the service token (X-Service-Token header value)
     * @return the resolved Tenant
     * @throws com.example.JasperReport.exceptions.TenantResolutionException
     *         if the token is unknown, missing, or invalid
     */
    Tenant resolve(String token);

    /**
     * Validates whether a service token exists in the tenant registry.
     *
     * @param token the service token to validate
     * @return {@code true} if the token maps to a known tenant, {@code false} otherwise
     */
    boolean validate(String token);
}
