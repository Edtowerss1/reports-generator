package com.example.JasperReport.tenant;

import com.example.JasperReport.config.TenantProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TenantContextInitializerTest {

    private static final Tenant ACME_TENANT = new Tenant(
            "acme",
            "/reportes/acme/",
            Set.of("ventas", "stock"),
            Set.of("PDF"),
            new Tenant.Datasource("jdbc:mysql://host/acme", "user", "pass", "com.mysql.cj.jdbc.Driver")
    );

    private static final Tenant CORP_TENANT = new Tenant(
            "corp",
            "/reportes/corp/",
            Set.of("reporte1"),
            Set.of("PDF"),
            new Tenant.Datasource("jdbc:mysql://host/corp", "user2", "pass2", "com.mysql.cj.jdbc.Driver")
    );

    @Mock
    private TenantResolver tenantResolver;

    private TenantProperties centralizedProperties;
    private TenantProperties dedicatedAcmeProperties;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        centralizedProperties = new TenantProperties();
        centralizedProperties.setProfile("centralized");
        centralizedProperties.setAssignedTenant(null);

        dedicatedAcmeProperties = new TenantProperties();
        dedicatedAcmeProperties.setProfile("dedicated");
        dedicatedAcmeProperties.setAssignedTenant("acme");

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveAndSetTenantContextInCentralizedMode() throws Exception {
        request.addHeader("X-Service-Token", "tok-A");
        when(tenantResolver.resolve("tok-A")).thenReturn(ACME_TENANT);

        var initializer = new TenantContextInitializer(tenantResolver, centralizedProperties);
        boolean result = initializer.preHandle(request, response, null);

        assertTrue(result);
        assertSame(ACME_TENANT, TenantContext.getCurrentTenant());
        verify(tenantResolver).resolve("tok-A");
    }

    @Test
    void shouldResolveAndSetTenantContextInDedicatedModeWithCorrectTenant() throws Exception {
        request.addHeader("X-Service-Token", "tok-A");
        when(tenantResolver.resolve("tok-A")).thenReturn(ACME_TENANT);

        var initializer = new TenantContextInitializer(tenantResolver, dedicatedAcmeProperties);
        boolean result = initializer.preHandle(request, response, null);

        assertTrue(result);
        assertSame(ACME_TENANT, TenantContext.getCurrentTenant());
    }

    @Test
    void shouldRejectWrongTenantInDedicatedMode() throws Exception {
        request.addHeader("X-Service-Token", "tok-B");
        when(tenantResolver.resolve("tok-B")).thenReturn(CORP_TENANT);

        var initializer = new TenantContextInitializer(tenantResolver, dedicatedAcmeProperties);
        boolean result = initializer.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldPassForAnyTenantInCentralizedMode() throws Exception {
        request.addHeader("X-Service-Token", "tok-B");
        when(tenantResolver.resolve("tok-B")).thenReturn(CORP_TENANT);

        var initializer = new TenantContextInitializer(tenantResolver, centralizedProperties);
        boolean result = initializer.preHandle(request, response, null);

        assertTrue(result);
        assertSame(CORP_TENANT, TenantContext.getCurrentTenant());
    }

    @Test
    void shouldClearTenantContextOnAfterCompletion() throws Exception {
        TenantContext.set(ACME_TENANT);
        assertNotNull(TenantContext.getCurrentTenant());

        var initializer = new TenantContextInitializer(tenantResolver, centralizedProperties);
        initializer.afterCompletion(request, response, null, null);

        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldThrowTenantResolutionExceptionForUnknownToken() {
        request.addHeader("X-Service-Token", "tok-X");
        when(tenantResolver.resolve("tok-X")).thenThrow(
                new com.example.JasperReport.exceptions.TenantResolutionException("Unknown token: tok-X")
        );

        var initializer = new TenantContextInitializer(tenantResolver, centralizedProperties);

        assertThrows(com.example.JasperReport.exceptions.TenantResolutionException.class,
                () -> initializer.preHandle(request, response, null));
        assertNull(TenantContext.getCurrentTenant());
    }
}
