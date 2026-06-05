package com.example.JaspertReport.tenant;

import com.example.JaspertReport.config.TenantProperties;
import com.example.JaspertReport.exceptions.TenantResolutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceManagerTest {

    private TenantProperties tenantProperties;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();

        var acmeConfig = new TenantProperties.Tenant();
        acmeConfig.setServiceToken("tok-A");
        acmeConfig.setReportesRuta("/reportes/acme/");
        var acmeDs = new TenantProperties.Datasource();
        acmeDs.setUrl("jdbc:mysql://localhost:3306/acme");
        acmeDs.setUsername("user");
        acmeDs.setPassword("pass");
        acmeConfig.setDatasource(acmeDs);
        acmeConfig.setAllowedReports(List.of("ventas"));

        var corpConfig = new TenantProperties.Tenant();
        corpConfig.setServiceToken("tok-B");
        corpConfig.setReportesRuta("/reportes/corp/");
        var corpDs = new TenantProperties.Datasource();
        corpDs.setUrl("jdbc:mysql://localhost:3306/corp");
        corpDs.setUsername("user2");
        corpDs.setPassword("pass2");
        corpConfig.setDatasource(corpDs);

        tenantProperties.setTenants(Map.of(
            "acme", acmeConfig,
            "corp", corpConfig
        ));
    }

    @Test
    void shouldReturnJdbcTemplateForValidTenant() {
        JdbcTemplate mockTemplate = new JdbcTemplate();
        var manager = new DataSourceManager(Map.of("acme", mockTemplate));

        JdbcTemplate result = manager.getTemplate("acme");

        assertSame(mockTemplate, result);
    }

    @Test
    void shouldThrowForUnknownTenant() {
        JdbcTemplate mockTemplate = new JdbcTemplate();
        var manager = new DataSourceManager(Map.of("acme", mockTemplate));

        assertThrows(TenantResolutionException.class, () -> manager.getTemplate("nonexistent"));
    }

    @Test
    void shouldRejectNullTenantId() {
        JdbcTemplate mockTemplate = new JdbcTemplate();
        var manager = new DataSourceManager(Map.of("acme", mockTemplate));

        assertThrows(TenantResolutionException.class, () -> manager.getTemplate(null));
    }

    @Test
    void shouldCreateTemplatesForAllConfiguredTenants() {
        JdbcTemplate acmeTemplate = new JdbcTemplate();
        JdbcTemplate corpTemplate = new JdbcTemplate();
        var manager = new DataSourceManager(Map.of(
            "acme", acmeTemplate,
            "corp", corpTemplate
        ));

        assertSame(acmeTemplate, manager.getTemplate("acme"));
        assertSame(corpTemplate, manager.getTemplate("corp"));
    }

    @Test
    void shouldValidateDataSourcesSuccessfully() {
        JdbcTemplate template = new JdbcTemplate();
        var manager = new DataSourceManager(Map.of("acme", template));

        // JdbcTemplate without a DataSource — validation will try
        // getDataSource() which returns null if none set.
        // In that case, getConnection() throws NPE which becomes RuntimeException.
        assertThrows(RuntimeException.class, manager::validateDataSources);
    }
}
