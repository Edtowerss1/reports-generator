package com.example.JasperReport.config;

import com.example.JasperReport.tenant.TenantContextInitializer;
import com.example.JasperReport.tenant.TenantResolver;
import com.example.JasperReport.tenant.TokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
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
            public com.example.JasperReport.tenant.Tenant resolve(String token) {
                throw new com.example.JasperReport.exceptions.TenantResolutionException("mock");
            }

            @Override
            public boolean validate(String token) {
                return "valid".equals(token);
            }
        };
    }
}
