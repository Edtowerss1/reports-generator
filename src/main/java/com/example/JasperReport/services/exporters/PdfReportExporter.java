package com.example.JasperReport.services.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.stereotype.Component;

@Component
public class PdfReportExporter implements ReportExporter {

    @Override
    public String getSupportedFormat() {
        return "PDF";
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    @Override
    public byte[] export(JasperPrint jasperPrint) throws JRException {
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
