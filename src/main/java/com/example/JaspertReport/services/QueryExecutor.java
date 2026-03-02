package com.example.JaspertReport.services;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcTemplate gasesJdbcTemplate;

    public QueryExecutor(
            JdbcTemplate jdbcTemplate,
            @Qualifier("gasesJdbcTemplate") JdbcTemplate gasesJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.gasesJdbcTemplate = gasesJdbcTemplate;
    }

    /**
     * Ejecuta una consulta SQL en el datasource indicado.
     *
     * @param sql        Consulta SQL a ejecutar.
     * @param datasource Nombre del datasource: "gases" para la BD secundaria,
     *                   cualquier otro valor (o null) usa la BD principal.
     */
    public List<Map<String, Object>> execute(String sql, String datasource) {
        JdbcTemplate template = "gases".equalsIgnoreCase(datasource)
                ? gasesJdbcTemplate
                : jdbcTemplate;
        return template.queryForList(sql);
    }
}
