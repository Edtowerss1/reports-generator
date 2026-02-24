package com.example.JaspertReport.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportRequestDTO {

    @JsonProperty("sql_empresa")
    private String sqlEmpresa;

    @JsonProperty("sql_atencion")
    private String sqlAtencion;

    @JsonProperty("sql_resultados")
    private String sqlResultados;

    public String getSqlEmpresa() {
        return sqlEmpresa;
    }

    public void setSqlEmpresa(String sqlEmpresa) {
        this.sqlEmpresa = sqlEmpresa;
    }

    public String getSqlAtencion() {
        return sqlAtencion;
    }

    public void setSqlAtencion(String sqlAtencion) {
        this.sqlAtencion = sqlAtencion;
    }

    public String getSqlResultados() {
        return sqlResultados;
    }

    public void setSqlResultados(String sqlResultados) {
        this.sqlResultados = sqlResultados;
    }
}
