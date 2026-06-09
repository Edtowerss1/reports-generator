package com.example.JasperReport.services;

import com.example.JasperReport.dtos.PrintRequestDTO;
import com.example.JasperReport.dtos.ReportRequestDTO;
import com.example.JasperReport.dtos.ReportResult;
import com.example.JasperReport.exceptions.ReportGenerationException;
import com.example.JasperReport.exceptions.ReportNotAllowedException;
import com.example.JasperReport.services.exporters.ExporterRegistry;
import com.example.JasperReport.services.exporters.ReportExporter;
import com.example.JasperReport.tenant.TenantContext;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.stereotype.Service;

@Service
public class ReportOrchestrator {

    private final JasperFiller jasperFiller;
    private final ExporterRegistry exporterRegistry;
    private final ReportPrintService reportPrintService;
    private final ReportAllowlistService allowlistService;

    public ReportOrchestrator(JasperFiller jasperFiller,
            ExporterRegistry exporterRegistry,
            ReportPrintService reportPrintService,
            ReportAllowlistService allowlistService) {
        this.jasperFiller = jasperFiller;
        this.exporterRegistry = exporterRegistry;
        this.reportPrintService = reportPrintService;
        this.allowlistService = allowlistService;
    }

    public ReportResult generate(ReportRequestDTO request) {
        try {
            enforceAllowlist(request.getReportName());
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
        enforceAllowlist(request.getReportName());
        JasperPrint jasperPrint = jasperFiller.fill(request.getReportName(), request.getQueries());
        int copies = request.getCopies() == null || request.getCopies() < 1 ? 1 : request.getCopies();
        reportPrintService.print(jasperPrint, request.getPrinterName(), copies);
    }

    private void enforceAllowlist(String reportName) {
        var tenant = TenantContext.getCurrentTenant();
        String tenantId = tenant != null ? tenant.id() : null;
        if (tenantId == null || !allowlistService.isAllowed(tenantId, reportName)) {
            throw new ReportNotAllowedException(
                tenantId != null ? tenantId : "unknown",
                reportName
            );
        }
    }
}
