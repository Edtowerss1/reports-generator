package com.example.JaspertReport.config;

import com.example.JaspertReport.tenant.TenantContextInitializer;
import com.example.JaspertReport.tenant.TenantResolver;
import com.example.JaspertReport.tenant.TokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.*;

class WebConfigTest {

    @Test
    void shouldConstructWithInterceptors() {
        var tenantResolver = mockResolver();
        var tokenValidator = new TokenValidator(tenantResolver);
        var contextInitializer = new TenantContextInitializer(tenantResolver, new TenantProperties());

        var webConfig = new WebConfig(tokenValidator, contextInitializer);

        assertNotNull(webConfig);
    }

    @Test
    void shouldRegisterInterceptorsWithoutError() {
        var tenantResolver = mockResolver();
        var tokenValidator = new TokenValidator(tenantResolver);
        var contextInitializer = new TenantContextInitializer(tenantResolver, new TenantProperties());

        var webConfig = new WebConfig(tokenValidator, contextInitializer);
        var registry = new InterceptorRegistry();

        assertDoesNotThrow(() -> webConfig.addInterceptors(registry));
    }

    private TenantResolver mockResolver() {
        return new TenantResolver() {
            @Override
            public com.example.JaspertReport.tenant.Tenant resolve(String token) {
                throw new com.example.JaspertReport.exceptions.TenantResolutionException("mock");
            }

            @Override
            public boolean validate(String token) {
                return "valid".equals(token);
            }
        };
    }
}
