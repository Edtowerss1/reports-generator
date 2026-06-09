package com.example.JasperReport.services;

import com.example.JasperReport.config.TenantProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads the per-tenant report allowlist from {@link TenantProperties}.
 * <p>
 * Match is case-insensitive. Semantics:
 * <ul>
 *   <li>{@code null} (not configured) → allow all reports (backward compatibility)</li>
 *   <li>Empty list (explicitly configured empty) → block all reports (spec A3)</li>
 *   <li>Non-empty list → check membership (spec A1, A2)</li>
 * </ul>
 */
@Component
public class ConfigBasedAllowlistService implements ReportAllowlistService {

    /**
     * Sentinel value stored when the tenant has no allowlist configured ({@code null}).
     * A missing / null allowlist means "allow all" for backward compatibility.
     */
    private static final List<String> ALLOW_ALL = Collections.emptyList();

    /**
     * Sentinel to distinguish "null" (no config = allow all) from "empty list" (block all).
     */
    private static final List<String> BLOCK_ALL = List.of();

    private final Map<String, List<String>> tenantIdToAllowedReports;

    public ConfigBasedAllowlistService(TenantProperties tenantProperties) {
        this.tenantIdToAllowedReports = tenantProperties.getTenants().entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    List<String> raw = entry.getValue().getAllowedReports();
                    if (raw == null) {
                        // Not configured → allow all (backward compat)
                        return ALLOW_ALL;
                    }
                    return raw.stream()
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> list.isEmpty() ? BLOCK_ALL : list
                        ));
                }
            ));
    }

    @Override
    public boolean isAllowed(String tenantId, String reportName) {
        List<String> allowed = tenantIdToAllowedReports.get(tenantId);
        if (allowed == null) {
            // Unknown tenant → deny
            return false;
        }
        // ALLOW_ALL means "no allowlist configured" → allow everything
        if (allowed == ALLOW_ALL) {
            return true;
        }
        // BLOCK_ALL means "explicitly empty allowlist" → deny everything
        if (allowed == BLOCK_ALL) {
            return false;
        }
        // Normal list → check membership (case-insensitive)
        return allowed.contains(reportName.toLowerCase(Locale.ROOT));
    }
}
