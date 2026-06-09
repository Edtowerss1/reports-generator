package com.example.JaspertReport.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class TenantPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(TenantPropertiesTestConfig.class);

    @Test
    void shouldApplyDefaultProfile() {
        runner.run(ctx -> {
            TenantProperties props = ctx.getBean(TenantProperties.class);
            assertEquals("centralized", props.getProfile());
            assertNull(props.getAssignedTenant());
            assertTrue(props.getTenants().isEmpty());
        });
    }

    @Test
    void shouldBindExplicitProfile() {
        runner.withPropertyValues("app.profile=dedicated")
            .run(ctx -> {
                TenantProperties props = ctx.getBean(TenantProperties.class);
                assertEquals("dedicated", props.getProfile());
            });
    }

    @Test
    void shouldBindAssignedTenant() {
        runner.withPropertyValues("app.assigned-tenant=acme")
            .run(ctx -> {
                TenantProperties props = ctx.getBean(TenantProperties.class);
                assertEquals("acme", props.getAssignedTenant());
            });
    }

    @Test
    void shouldBindSingleTenant() {
        runner.withPropertyValues(
                "app.tenants.acme.service-token=tok-A",
                "app.tenants.acme.reportes-ruta=/reportes/acme/",
                "app.tenants.acme.datasource.url=jdbc:mysql://host/acme",
                "app.tenants.acme.datasource.username=user",
                "app.tenants.acme.datasource.password=pass",
                "app.tenants.acme.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "app.tenants.acme.allowed-reports=ventas,stock",
                "app.tenants.acme.allowed-formats=PDF,XLSX"
            )
            .run(ctx -> {
                TenantProperties props = ctx.getBean(TenantProperties.class);
                assertEquals(1, props.getTenants().size());

                TenantProperties.Tenant tenant = props.getTenants().get("acme");
                assertNotNull(tenant);
                assertEquals("tok-A", tenant.getServiceToken());
                assertEquals("/reportes/acme/", tenant.getReportesRuta());
                assertEquals("jdbc:mysql://host/acme", tenant.getDatasource().getUrl());
                assertEquals("user", tenant.getDatasource().getUsername());
                assertEquals("pass", tenant.getDatasource().getPassword());
                assertEquals("com.mysql.cj.jdbc.Driver", tenant.getDatasource().getDriverClassName());
                assertTrue(tenant.getAllowedReports().containsAll(java.util.List.of("ventas", "stock")));
                assertTrue(tenant.getAllowedFormats().containsAll(java.util.List.of("PDF", "XLSX")));
            });
    }

    @Test
    void shouldBindMultipleTenants() {
        runner.withPropertyValues(
                "app.tenants.acme.service-token=tok-A",
                "app.tenants.acme.reportes-ruta=/reportes/acme/",
                "app.tenants.acme.datasource.url=jdbc:mysql://host/acme",
                "app.tenants.acme.datasource.username=user",
                "app.tenants.acme.datasource.password=pass",
                "app.tenants.acme.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "app.tenants.corp.service-token=tok-B",
                "app.tenants.corp.reportes-ruta=/reportes/corp/",
                "app.tenants.corp.datasource.url=jdbc:mysql://host/corp",
                "app.tenants.corp.datasource.username=user2",
                "app.tenants.corp.datasource.password=pass2",
                "app.tenants.corp.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
            )
            .run(ctx -> {
                TenantProperties props = ctx.getBean(TenantProperties.class);
                assertEquals(2, props.getTenants().size());
                assertNotNull(props.getTenants().get("acme"));
                assertNotNull(props.getTenants().get("corp"));
            });
    }

    @Configuration
    @EnableConfigurationProperties(TenantProperties.class)
    static class TenantPropertiesTestConfig {
    }
}
