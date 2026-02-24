package com.example.JaspertReport.services;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.JaspertReport.dtos.DatosEmpresaDTO;
import com.example.JaspertReport.dtos.AtencionDTO;
import com.example.JaspertReport.dtos.ResultadoExamenDTO;
import com.example.JaspertReport.dtos.ReportRequestDTO;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabReportService {

    private final ReportDataService reportDataService;

    @Value("${app.reportes.ruta}")
    private String reportesRuta;

    private static final String MAIN_REPORT_NAME = "Laboratorio.jasper";
    private static final String LOGO_FILE_NAME = "Logo.jpg";

    public LabReportService(ReportDataService reportDataService) {
        this.reportDataService = reportDataService;
    }

    public byte[] generarReporteAtencion(ReportRequestDTO request) {
        try {

            // ===== 1) Obtener datos reales =====
            // queries[0] = empresa | queries[1] = atencion | queries[2] = resultados
            DatosEmpresaDTO datosEmpresa = reportDataService.getDatosEmpresa(request.getQueries().get(0));
            AtencionDTO datosAtencion = reportDataService.getAtencion(request.getQueries().get(1));
            List<ResultadoExamenDTO> resultados = reportDataService.getResultados(request.getQueries().get(2));

            // ===== 2) Dividir la lista según tipores =====
            List<ResultadoExamenDTO> resultadosNormal = resultados.stream()
                    .filter(r -> BigDecimal.valueOf(1).equals(r.getTipores()))
                    .collect(Collectors.toList());

            List<ResultadoExamenDTO> resultadosVariables = resultados.stream()
                    .filter(r -> BigDecimal.valueOf(3).equals(r.getTipores()))
                    .collect(Collectors.toList());

            // Datasources separados para cada subreporte
            JRBeanCollectionDataSource dsNormal = new JRBeanCollectionDataSource(resultadosNormal);
            JRBeanCollectionDataSource dsVariables = new JRBeanCollectionDataSource(resultadosVariables);

            // ===== 3) Parámetros del reporte =====
            Map<String, Object> params = new HashMap<>();

            params.put("DATOS_EMPRESA", datosEmpresa);
            params.put("DATOS_ATENCION", datosAtencion);

            params.put("DS_RESULTADOS_NORMAL", dsNormal);
            params.put("DS_RESULTADOS_VARIABLES", dsVariables);

            // Directorio de subreportes (ruta real en disco — JasperReports la resuelve directamente)
            params.put("SUBREPORT_DIR", reportesRuta);

            // Logo desde disco
            params.put("LOGO_INPUT_STREAM", new FileInputStream(reportesRuta + LOGO_FILE_NAME));

            // ===== 4) Cargar y llenar el reporte principal =====
            try (InputStream in = new FileInputStream(reportesRuta + MAIN_REPORT_NAME)) {

                JasperPrint jasperPrint = JasperFillManager.fillReport(
                        in,
                        params,
                        new JREmptyDataSource()
                );

                // ===== 5) Exportar a PDF =====
                return JasperExportManager.exportReportToPdf(jasperPrint);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error generando reporte", e);
        }
    }
}
