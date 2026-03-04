package com.example.JaspertReport.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Ejecuta consultas SQL contra la única base de datos configurada
 * en la instancia actual (spring.datasource.*).
 * Cada instancia del servicio conecta exclusivamente a su propia BD.
 */
@Service
public class QueryExecutor {

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ejecuta una consulta SQL contra la base de datos de esta instancia.
     *
     * @param sql        Consulta SQL a ejecutar.
     * @param datasource Ignorado. Se mantiene el parámetro por compatibilidad
     *                   con clientes existentes que aún lo envían.
     */
    public List<Map<String, Object>> execute(String sql, String datasource) {
        return jdbcTemplate.queryForList(sql);
    }
}
