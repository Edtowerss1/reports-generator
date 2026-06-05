package com.example.JaspertReport.services;

import com.example.JaspertReport.config.TenantProperties;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads the per-tenant report allowlist from {@link TenantProperties}.
 * <p>
 * Match is case-insensitive. An absent or empty allowlist blocks all reports,
 * matching spec requirements A1 (allowed), A2 (disallowed), and A3 (empty blocks all).
 */
public class ConfigBasedAllowlistService implements ReportAllowlistService {

    private final Map<String, List<String>> tenantIdToAllowedReports;

    public ConfigBasedAllowlistService(TenantProperties tenantProperties) {
        this.tenantIdToAllowedReports = tenantProperties.getTenants().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    List<String> raw = entry.getValue().getAllowedReports();
                    if (raw == null) {
                        return Collections.emptyList();
                    }
                    return raw.stream()
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList());
                }
            ));
    }

    @Override
    public boolean isAllowed(String tenantId, String reportName) {
        List<String> allowed = tenantIdToAllowedReports.get(tenantId);
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        return allowed.contains(reportName.toLowerCase(Locale.ROOT));
    }
}
