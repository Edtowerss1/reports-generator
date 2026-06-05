package com.example.JaspertReport.services;

import com.example.JaspertReport.exceptions.TenantResolutionException;
import com.example.JaspertReport.tenant.DataSourceProvider;
import com.example.JaspertReport.tenant.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ejecuta consultas SQL contra la base de datos del tenant activo
 * en la instancia actual. El datasource se resuelve a través de
 * {@link DataSourceProvider} según el tenant del contexto.
 */
@Service
public class QueryExecutor {

    private final DataSourceProvider dataSourceProvider;

    public QueryExecutor(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    /**
     * Ejecuta una consulta SQL contra la base de datos del tenant activo.
     *
     * @param sql        Consulta SQL a ejecutar.
     * @param datasource Ignorado. Se mantiene el parámetro por compatibilidad
     *                   con clientes existentes que aún lo envían.
     */
    public List<Map<String, Object>> execute(String sql, String datasource) {
        Objects.requireNonNull(sql, "sql debe ser no nulo");
        String tenantId = resolveTenantId();
        JdbcTemplate template = dataSourceProvider.getTemplate(tenantId);
        return template.queryForList(sql);
    }

    private String resolveTenantId() {
        var tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            throw new TenantResolutionException("No tenant in context — TenantContext not initialized");
        }
        return tenant.id();
    }
}
