package com.example.JasperReport.dtos;

import lombok.Data;

@Data
public class QueryParamDTO {
    private String param;
    private String query;

    /**
     * Campo mantenido por compatibilidad con clientes existentes.
     * En la arquitectura actual de servicios separados, cada instancia
     * conecta a una única BD, por lo que este valor es ignorado.
     */
    private String datasource;
}
