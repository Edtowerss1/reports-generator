package com.example.JaspertReport.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportResult {
    private final byte[] content;
    private final String contentType;
    private final String fileExtension;
}
