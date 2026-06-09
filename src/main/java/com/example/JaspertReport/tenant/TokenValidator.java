package com.example.JaspertReport.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Validates the presence and format of the {@code X-Service-Token} header.
 * <p>
 * SRP: validates token presence/format ONLY. Delegates to {@link TenantResolver#validate(String)}
 * for existence check. Does NOT resolve the tenant or populate {@link TenantContext}.
 * Returns HTTP 401 if the token is missing, empty, blank, or unknown.
 * <p>
 * Must be registered before {@link TenantContextInitializer} in the interceptor chain.
 */
@Slf4j
@Component
public class TokenValidator implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "X-Service-Token";

    private final TenantResolver tenantResolver;

    public TokenValidator(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String token = request.getHeader(TOKEN_HEADER);

        if (token == null || token.isBlank()) {
            log.warn("Missing or empty X-Service-Token header");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "X-Service-Token header is missing or empty");
            return false;
        }

        if (!tenantResolver.validate(token)) {
            log.warn("Invalid service token received");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown or invalid service token");
            return false;
        }

        return true;
    }
}
