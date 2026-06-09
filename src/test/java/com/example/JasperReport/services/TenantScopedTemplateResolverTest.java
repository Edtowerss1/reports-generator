package com.example.JasperReport.services;

import com.example.JasperReport.config.TenantProperties;
import com.example.JasperReport.exceptions.ReportNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class TenantScopedTemplateResolverTest {

    private TenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();

        var acmeConfig = new TenantProperties.Tenant();
        acmeConfig.setServiceToken("tok-A");
        acmeConfig.setReportesRuta("/reportes/acme/");
        var acmeDs = new TenantProperties.Datasource();
        acmeDs.setUrl("jdbc:mysql://host/acme");
        acmeConfig.setDatasource(acmeDs);
        acmeConfig.setAllowedReports(List.of("ventas"));

        tenantProperties.setTenants(Map.of("acme", acmeConfig));
    }

    @Test
    void shouldResolvePathFromTenantRuta(@TempDir Path tempDir) throws IOException {
        // Create actual .jasper file in temp dir
        Path jasperFile = tempDir.resolve("ventas.jasper");
        Files.createFile(jasperFile);

        var acmeConfig = tenantProperties.getTenants().get("acme");
        acmeConfig.setReportesRuta(tempDir.toString() + "/");

        var resolver = new TenantScopedTemplateResolver(tenantProperties);
        Path resolved = resolver.resolve("acme", "ventas");

        assertEquals(jasperFile, resolved);
        assertTrue(Files.exists(resolved));
    }

    @Test
    void shouldThrowWhenTemplateNotFound() {
        var resolver = new TenantScopedTemplateResolver(tenantProperties);

        assertThrows(ReportNotFoundException.class, () -> resolver.resolve("acme", "missing"));
    }

    @Test
    void shouldThrowForUnknownTenant() {
        var resolver = new TenantScopedTemplateResolver(tenantProperties);

        assertThrows(ReportNotFoundException.class, () -> resolver.resolve("nonexistent", "ventas"));
    }

    @Test
    void shouldHandleRutaWithAndWithoutTrailingSlash(@TempDir Path tempDir) throws IOException {
        Path jasperFile = tempDir.resolve("stock.jasper");
        Files.createFile(jasperFile);

        var acmeConfig = tenantProperties.getTenants().get("acme");
        acmeConfig.setReportesRuta(tempDir.toString()); // no trailing slash

        var resolver = new TenantScopedTemplateResolver(tenantProperties);
        Path resolved = resolver.resolve("acme", "stock");

        assertEquals(jasperFile, resolved);
        assertTrue(Files.exists(resolved));
    }
}
