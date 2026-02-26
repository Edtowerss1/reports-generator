package com.example.JaspertReport.services.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;

public interface ReportExporter {
    String getSupportedFormat();
    String getContentType();
    String getFileExtension();
    byte[] export(JasperPrint jasperPrint) throws JRException;
}
