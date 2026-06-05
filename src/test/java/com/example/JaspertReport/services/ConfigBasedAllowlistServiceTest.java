package com.example.JaspertReport.services;

import com.example.JaspertReport.config.TenantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigBasedAllowlistServiceTest {

    private TenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();

        var acmeConfig = new TenantProperties.Tenant();
        acmeConfig.setServiceToken("tok-A");
        acmeConfig.setReportesRuta("/reportes/acme/");
        acmeConfig.setAllowedReports(List.of("ventas", "stock", "CLIENTES"));
        var acmeDs = new TenantProperties.Datasource();
        acmeDs.setUrl("jdbc:mysql://host/acme");
        acmeConfig.setDatasource(acmeDs);

        var corpConfig = new TenantProperties.Tenant();
        corpConfig.setServiceToken("tok-B");
        corpConfig.setReportesRuta("/reportes/corp/");
        corpConfig.setAllowedReports(List.of());
        var corpDs = new TenantProperties.Datasource();
        corpDs.setUrl("jdbc:mysql://host/corp");
        corpConfig.setDatasource(corpDs);

        var noAllowlistConfig = new TenantProperties.Tenant();
        noAllowlistConfig.setServiceToken("tok-C");
        noAllowlistConfig.setReportesRuta("/reportes/test/");
        var testDs = new TenantProperties.Datasource();
        testDs.setUrl("jdbc:mysql://host/test");
        noAllowlistConfig.setDatasource(testDs);
        // allowedReports left as null

        tenantProperties.setTenants(Map.of(
            "acme", acmeConfig,
            "corp", corpConfig,
            "test", noAllowlistConfig
        ));
    }

    @Test
    void shouldAllowReportInAllowlist() {
        var service = new ConfigBasedAllowlistService(tenantProperties);

        assertTrue(service.isAllowed("acme", "ventas"));
    }

    @Test
    void shouldRejectReportNotInAllowlist() {
        var service = new ConfigBasedAllowlistService(tenantProperties);

        assertFalse(service.isAllowed("acme", "nomina"));
    }

    @Test
    void shouldBlockAllWhenAllowlistIsEmpty() {
        var service = new ConfigBasedAllowlistService(tenantProperties);

        assertFalse(service.isAllowed("corp", "ventas"));
    }

    @Test
    void shouldAllowAllWhenAllowlistIsNull() {
        var service = new ConfigBasedAllowlistService(tenantProperties);

        // null = not configured → allow all (backward compatibility)
        assertTrue(service.isAllowed("test", "ventas"));
        assertTrue(service.isAllowed("test", "any-report"));
    }

    @Test
    void shouldBeCaseInsensitive() {
        var service = new ConfigBasedAllowlistService(tenantProperties);

        assertTrue(service.isAllowed("acme", "VENTAS"));
        assertTrue(service.isAllowed("acme", "Ventas"));
        assertTrue(service.isAllowed("acme", "clientes"));
    }

    @Test
    void shouldRejectForUnknownTenant() {
        var service = new ConfigBasedAllowlistService(tenantProperties);

        assertFalse(service.isAllowed("nonexistent", "ventas"));
    }
}
