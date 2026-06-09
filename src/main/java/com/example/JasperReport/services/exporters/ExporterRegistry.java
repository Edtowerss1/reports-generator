package com.example.JasperReport.services.exporters;

import com.example.JasperReport.exceptions.InvalidFormatException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExporterRegistry {

    private final Map<String, ReportExporter> exporters;

    public ExporterRegistry(List<ReportExporter> exporterList) {
        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(
                        e -> e.getSupportedFormat().toUpperCase(),
                        Function.identity()
                ));
    }

    public ReportExporter getExporter(String format) {
        return Optional.ofNullable(exporters.get(format.toUpperCase()))
                .orElseThrow(() -> new InvalidFormatException(format));
    }
}
