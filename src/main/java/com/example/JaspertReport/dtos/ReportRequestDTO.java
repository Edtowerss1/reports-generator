package com.example.JaspertReport.dtos;

import java.util.List;

public class ReportRequestDTO {

    private List<String> queries;

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }
}
