package com.example.JasperReport.tenant;

import com.example.JasperReport.config.TenantProperties;
import com.example.JasperReport.exceptions.TenantResolutionException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resolves service tokens to Tenants using the {@link TenantProperties} configuration map.
 * <p>
 * Builds an internal token→tenantId lookup at construction time for O(1) resolution.
 * Throws {@link TenantResolutionException} for unknown or null tokens, matching
 * spec requirement R2 (unknown token → HTTP 401) and R3 (missing token → HTTP 401).
 */
@Component
public class ConfigBasedTenantResolver implements TenantResolver {

    private final Map<String, Tenant> tokenToTenant = new LinkedHashMap<>();

    public ConfigBasedTenantResolver(TenantProperties tenantProperties) {
        for (var entry : tenantProperties.getTenants().entrySet()) {
            String tenantId = entry.getKey();
            TenantProperties.Tenant config = entry.getValue();

            Set<String> allowedReports = config.getAllowedReports() == null
                ? Set.of()
                : Set.copyOf(config.getAllowedReports());

            Set<String> allowedFormats = config.getAllowedFormats() == null
                ? Set.of()
                : Set.copyOf(config.getAllowedFormats());

            Tenant.Datasource ds = new Tenant.Datasource(
                config.getDatasource().getUrl(),
                config.getDatasource().getUsername(),
                config.getDatasource().getPassword(),
                config.getDatasource().getDriverClassName()
            );

            Tenant tenant = new Tenant(
                tenantId,
                config.getReportesRuta(),
                allowedReports,
                allowedFormats,
                ds
            );

            tokenToTenant.put(config.getServiceToken(), tenant);
        }
    }

    @Override
    public Tenant resolve(String token) {
        if (token == null) {
            throw new TenantResolutionException("X-Service-Token header is missing");
        }
        Tenant tenant = tokenToTenant.get(token);
        if (tenant == null) {
            throw new TenantResolutionException("Unknown service token: " + token);
        }
        return tenant;
    }

    @Override
    public boolean validate(String token) {
        return token != null && tokenToTenant.containsKey(token);
    }
}
