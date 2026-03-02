package com.example.JaspertReport.dtos;

import lombok.Data;

@Data
public class QueryParamDTO {
    private String param;
    private String query;

    /**
     * Nombre del datasource a usar para esta consulta.
     * Valores posibles: "primary" (por defecto) o "gases".
     * Si se omite, se usa la BD principal (DB_NAME).
     */
    private String datasource;
}
