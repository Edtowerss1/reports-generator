package com.example.JaspertReport.tenant;

import com.example.JaspertReport.config.TenantProperties;
import com.example.JaspertReport.exceptions.TenantResolutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigBasedTenantResolverTest {

    private TenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
        var acmeConfig = new TenantProperties.Tenant();
        acmeConfig.setServiceToken("tok-A");
        acmeConfig.setReportesRuta("/reportes/acme/");
        var acmeDs = new TenantProperties.Datasource();
        acmeDs.setUrl("jdbc:mysql://host/acme");
        acmeDs.setUsername("user");
        acmeDs.setPassword("pass");
        acmeConfig.setDatasource(acmeDs);
        acmeConfig.setAllowedReports(List.of("ventas", "stock"));

        var corpConfig = new TenantProperties.Tenant();
        corpConfig.setServiceToken("tok-B");
        corpConfig.setReportesRuta("/reportes/corp/");
        var corpDs = new TenantProperties.Datasource();
        corpDs.setUrl("jdbc:mysql://host/corp");
        corpDs.setUsername("user2");
        corpDs.setPassword("pass2");
        corpConfig.setDatasource(corpDs);

        tenantProperties.setTenants(Map.of(
            "acme", acmeConfig,
            "corp", corpConfig
        ));
    }

    @Test
    void shouldResolveTenantForValidToken() {
        var resolver = new ConfigBasedTenantResolver(tenantProperties);
        Tenant tenant = resolver.resolve("tok-A");

        assertNotNull(tenant);
        assertEquals("acme", tenant.id());
        assertEquals("/reportes/acme/", tenant.reportesRuta());
    }

    @Test
    void shouldThrowForUnknownToken() {
        var resolver = new ConfigBasedTenantResolver(tenantProperties);

        assertThrows(TenantResolutionException.class, () -> resolver.resolve("tok-X"));
    }

    @Test
    void shouldThrowForNullToken() {
        var resolver = new ConfigBasedTenantResolver(tenantProperties);

        assertThrows(TenantResolutionException.class, () -> resolver.resolve(null));
    }

    @Test
    void shouldValidateKnownToken() {
        var resolver = new ConfigBasedTenantResolver(tenantProperties);

        assertTrue(resolver.validate("tok-A"));
    }

    @Test
    void shouldNotValidateUnknownToken() {
        var resolver = new ConfigBasedTenantResolver(tenantProperties);

        assertFalse(resolver.validate("tok-X"));
    }

    @Test
    void shouldResolveMultipleTenantsByToken() {
        var resolver = new ConfigBasedTenantResolver(tenantProperties);

        Tenant acme = resolver.resolve("tok-A");
        Tenant corp = resolver.resolve("tok-B");

        assertEquals("acme", acme.id());
        assertEquals("corp", corp.id());
    }
}
