package com.example.JaspertReport.services;

import com.example.JaspertReport.dtos.PrintRequestDTO;
import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.dtos.ReportResult;
import com.example.JaspertReport.exceptions.ReportGenerationException;
import com.example.JaspertReport.services.exporters.ExporterRegistry;
import com.example.JaspertReport.services.exporters.ReportExporter;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.stereotype.Service;

@Service
public class ReportOrchestrator {

    private final JasperFiller jasperFiller;
    private final ExporterRegistry exporterRegistry;
    private final ReportPrintService reportPrintService;

    public ReportOrchestrator(JasperFiller jasperFiller,
            ExporterRegistry exporterRegistry,
            ReportPrintService reportPrintService) {
        this.jasperFiller = jasperFiller;
        this.exporterRegistry = exporterRegistry;
        this.reportPrintService = reportPrintService;
    }

    public ReportResult generate(ReportRequestDTO request) {
        try {
            JasperPrint jasperPrint = jasperFiller.fill(request.getReportName(), request.getQueries());
            ReportExporter exporter = exporterRegistry.getExporter(request.getFormat());
            byte[] content = exporter.export(jasperPrint);
            return new ReportResult(content, exporter.getContentType(), exporter.getFileExtension());
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new ReportGenerationException("Error al exportar el reporte", e);
        }
    }

    public void print(PrintRequestDTO request) {
        JasperPrint jasperPrint = jasperFiller.fill(request.getReportName(), request.getQueries());
        int copies = request.getCopies() == null || request.getCopies() < 1 ? 1 : request.getCopies();
        reportPrintService.print(jasperPrint, request.getPrinterName(), copies);
    }
}
