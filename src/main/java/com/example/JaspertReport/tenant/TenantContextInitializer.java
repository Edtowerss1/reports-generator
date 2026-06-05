package com.example.JaspertReport.tenant;

import com.example.JaspertReport.config.TenantProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Resolves the tenant from a validated token, enforces dedicated mode if active,
 * and populates {@link TenantContext} for the duration of the request.
 * <p>
 * SRP: tenant resolution and context lifecycle ONLY. Token format validation
 * is handled by {@link TokenValidator} (registered earlier in the chain).
 * <p>
 * {@link #afterCompletion(HttpServletRequest, HttpServletResponse, Object, Exception)}
 * guarantees {@link TenantContext#clear()} is called after every request,
 * preventing ThreadLocal leaks in pooled environments.
 */
@Slf4j
@Component
public class TenantContextInitializer implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "X-Service-Token";

    private final TenantResolver tenantResolver;
    private final TenantProperties tenantProperties;

    public TenantContextInitializer(TenantResolver tenantResolver, TenantProperties tenantProperties) {
        this.tenantResolver = tenantResolver;
        this.tenantProperties = tenantProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(TOKEN_HEADER);

        Tenant tenant = tenantResolver.resolve(token);

        // Enforce dedicated mode: only the assigned tenant's requests are allowed
        if ("dedicated".equalsIgnoreCase(tenantProperties.getProfile())) {
            String assignedTenant = tenantProperties.getAssignedTenant();
            if (assignedTenant != null && !assignedTenant.equals(tenant.id())) {
                log.warn("Dedicated mode: tenant '{}' attempted request but '{}' is assigned",
                        tenant.id(), assignedTenant);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "This instance is dedicated to tenant '" + assignedTenant
                                + "'; token for tenant '" + tenant.id() + "' is not allowed");
                return false;
            }
        }

        TenantContext.set(tenant);
        log.debug("Tenant context set for tenant: {}", tenant.id());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        TenantContext.clear();
    }
}
