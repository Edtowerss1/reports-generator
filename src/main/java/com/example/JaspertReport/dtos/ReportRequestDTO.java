package com.example.JaspertReport.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ReportRequestDTO {
    private String reportName;
    private String format;
    private List<QueryParamDTO> queries;
}
