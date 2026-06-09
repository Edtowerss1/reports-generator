package com.example.JasperReport.tenant;

import com.example.JasperReport.config.TenantProperties;
import com.example.JasperReport.exceptions.TenantResolutionException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates one HikariCP DataSource per tenant and provides tenant-scoped
 * {@link JdbcTemplate} instances.
 * <p>
 * Validates all datasources at startup via {@link #validateDataSources()}.
 * If any datasource is unreachable, the application context fails to start
 * (fail-fast on bad JDBC URL — spec D2, design AD#3).
 * <p>
 * Implements {@link DataSourceProvider} so consumers depend on the
 * abstraction, never on HikariCP directly (DIP).
 */
@Slf4j
@Component
public class DataSourceManager implements DataSourceProvider {

    private final Map<String, JdbcTemplate> templates = new HashMap<>();

    /**
     * Production constructor — creates a HikariCP DataSource per tenant.
     *
     * @param tenantProperties the tenant configuration
     */
    @Autowired
    public DataSourceManager(TenantProperties tenantProperties) {
        for (var entry : tenantProperties.getTenants().entrySet()) {
            String tenantId = entry.getKey();
            TenantProperties.Tenant config = entry.getValue();
            TenantProperties.Datasource dsConfig = config.getDatasource();

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(dsConfig.getUrl());
            hikariConfig.setUsername(dsConfig.getUsername());
            hikariConfig.setPassword(dsConfig.getPassword());
            hikariConfig.setDriverClassName(dsConfig.getDriverClassName());
            hikariConfig.setPoolName("hikari-" + tenantId);
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(5000);
            hikariConfig.setValidationTimeout(3000);

            DataSource dataSource = new HikariDataSource(hikariConfig);
            templates.put(tenantId, new JdbcTemplate(dataSource));
            log.info("Created datasource pool for tenant '{}' -> {}", tenantId, dsConfig.getUrl());
        }
    }

    /**
     * Test-friendly constructor — accepts pre-built templates.
     */
    DataSourceManager(Map<String, JdbcTemplate> templates) {
        this.templates.putAll(templates);
    }

    /**
     * Validates all datasource pools by attempting to get and close a connection.
     * Called automatically by Spring after bean construction.
     * Throws on first failure — fast fail prevents deployment with bad config.
     */
    @PostConstruct
    public void validateDataSources() {
        log.info("Validating {} datasource(s)...", templates.size());
        for (var entry : templates.entrySet()) {
            String tenantId = entry.getKey();
            try {
                var ds = entry.getValue().getDataSource();
                if (ds == null) {
                    throw new RuntimeException(
                        "Failed to validate datasource for tenant '" + tenantId + "': datasource is null"
                    );
                }
                try (var conn = ds.getConnection()) {
                    log.debug("Datasource OK for tenant '{}'", tenantId);
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                    "Failed to validate datasource for tenant '" + tenantId + "': " + e.getMessage(), e
                );
            }
        }
        log.info("All {} datasource(s) validated successfully", templates.size());
    }

    @Override
    public JdbcTemplate getTemplate(String tenantId) {
        if (tenantId == null) {
            throw new TenantResolutionException("tenantId must not be null");
        }
        JdbcTemplate template = templates.get(tenantId);
        if (template == null) {
            throw new TenantResolutionException("Unknown tenant: " + tenantId);
        }
        return template;
    }
}
