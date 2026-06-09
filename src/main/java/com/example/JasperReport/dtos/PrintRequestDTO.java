package com.example.JasperReport.dtos;

import lombok.Data;

import java.util.List;

@Data
public class PrintRequestDTO {
	private String reportName;
	private String printerName;
	private Integer copies;
	private List<QueryParamDTO> queries;
}
