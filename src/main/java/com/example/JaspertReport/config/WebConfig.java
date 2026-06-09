package com.example.JaspertReport.config;

import com.example.JaspertReport.tenant.TenantContextInitializer;
import com.example.JaspertReport.tenant.TokenValidator;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers tenant interceptors for the multi-tenant engine.
 * <p>
 * Interceptor order (SRP):
 * <ol>
 *   <li>{@link TokenValidator} — validates {@code X-Service-Token} header presence and format</li>
 *   <li>{@link TenantContextInitializer} — resolves tenant, enforces dedicated mode, populates context</li>
 * </ol>
 * Order matters: token validation must happen before tenant context initialization.
 * <p>
 * Only applies to {@code /reportes/**} endpoints.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @NonNull
    private final TokenValidator tokenValidator;

    @NonNull
    private final TenantContextInitializer tenantContextInitializer;

    public WebConfig(@NonNull TokenValidator tokenValidator, @NonNull TenantContextInitializer tenantContextInitializer) {
        this.tokenValidator = tokenValidator;
        this.tenantContextInitializer = tenantContextInitializer;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(tokenValidator)
                .addPathPatterns("/reportes/**");

        registry.addInterceptor(tenantContextInitializer)
                .addPathPatterns("/reportes/**");
    }
}
