package com.example.JaspertReport.services.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class DocxReportExporter implements ReportExporter {

    @Override
    public String getSupportedFormat() {
        return "DOCX";
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    @Override
    public String getFileExtension() {
        return "docx";
    }

    @Override
    public byte[] export(JasperPrint jasperPrint) throws JRException {
        JRDocxExporter exporter = new JRDocxExporter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        exporter.exportReport();

        return outputStream.toByteArray();
    }
}
