package com.example.JaspertReport.tenant;

/**
 * Thread-local holder for the current request's tenant.
 * Set at the beginning of a request (in TenantContextInitializer) and
 * cleared in afterCompletion. Each thread (request) gets its own copy.
 */
public final class TenantContext {

    private static final ThreadLocal<Tenant> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // utility class
    }

    public static Tenant getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void set(Tenant tenant) {
        CURRENT_TENANT.set(tenant);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
