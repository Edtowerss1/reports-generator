package com.example.JaspertReport.tenant;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class TenantTest {

    @Test
    void shouldCreateTenantWithAllFields() {
        var datasource = new Tenant.Datasource("jdbc:mysql://localhost:3306/acme", "user", "pass", "com.mysql.cj.jdbc.Driver");
        var allowedReports = Set.of("ventas", "stock");
        var allowedFormats = Set.of("PDF", "XLSX");

        var tenant = new Tenant("acme", "/reportes/acme/", allowedReports, allowedFormats, datasource);

        assertEquals("acme", tenant.id());
        assertEquals("/reportes/acme/", tenant.reportesRuta());
        assertEquals(allowedReports, tenant.allowedReports());
        assertEquals(allowedFormats, tenant.allowedFormats());
        assertEquals(datasource, tenant.datasource());
    }

    @Test
    void shouldAllowEmptyAllowedReports() {
        var datasource = new Tenant.Datasource("jdbc:mysql://localhost:3306/acme", "user", "pass", "com.mysql.cj.jdbc.Driver");

        var tenant = new Tenant("acme", "/reportes/acme/", Set.of(), Set.of("PDF"), datasource);

        assertTrue(tenant.allowedReports().isEmpty());
    }

    @Test
    void shouldAllowNullAllowedFormats() {
        var datasource = new Tenant.Datasource("jdbc:mysql://localhost:3306/acme", "user", "pass", "com.mysql.cj.jdbc.Driver");

        var tenant = new Tenant("acme", "/reportes/acme/", Set.of("ventas"), null, datasource);

        assertNull(tenant.allowedFormats());
    }

    @Test
    void shouldCreateDatasourceRecord() {
        var ds = new Tenant.Datasource("jdbc:mysql://host:3306/db", "user", "pass", "com.mysql.cj.jdbc.Driver");

        assertEquals("jdbc:mysql://host:3306/db", ds.url());
        assertEquals("user", ds.username());
        assertEquals("pass", ds.password());
        assertEquals("com.mysql.cj.jdbc.Driver", ds.driverClassName());
    }

    @Test
    void tenantsWithSameValuesShouldBeEqual() {
        var ds1 = new Tenant.Datasource("jdbc:mysql://host:3306/db", "user", "pass", "com.mysql.cj.jdbc.Driver");
        var ds2 = new Tenant.Datasource("jdbc:mysql://host:3306/db", "user", "pass", "com.mysql.cj.jdbc.Driver");

        var tenant1 = new Tenant("acme", "/reportes/", Set.of("ventas"), Set.of("PDF"), ds1);
        var tenant2 = new Tenant("acme", "/reportes/", Set.of("ventas"), Set.of("PDF"), ds2);

        assertEquals(tenant1, tenant2);
        assertEquals(tenant1.hashCode(), tenant2.hashCode());
    }
}
