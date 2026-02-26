package com.example.JaspertReport.services;

import com.example.JaspertReport.dtos.QueryParamDTO;
import com.example.JaspertReport.exceptions.ReportGenerationException;
import com.example.JaspertReport.exceptions.ReportNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JasperFiller {

    private final QueryExecutor queryExecutor;

    @Value("${app.reportes.ruta}")
    private String reportesRuta;

    public JasperFiller(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @PostConstruct
    public void compileReports() {
        File dir = new File(reportesRuta);
        File[] jrxmlFiles = dir.listFiles((d, name) -> name.endsWith(".jrxml"));
        if (jrxmlFiles == null) return;

        for (File jrxmlFile : jrxmlFiles) {
            String jasperPath = jrxmlFile.getAbsolutePath().replace(".jrxml", ".jasper");
            File jasperFile = new File(jasperPath);
            if (!jasperFile.exists() || jrxmlFile.lastModified() > jasperFile.lastModified()) {
                try {
                    JasperCompileManager.compileReportToFile(jrxmlFile.getAbsolutePath(), jasperPath);
                    log.info("Compilado: {}", jrxmlFile.getName());
                } catch (Exception e) {
                    log.warn("No se pudo compilar {}: {}", jrxmlFile.getName(), e.getMessage());
                }
            }
        }
    }

    public JasperPrint fill(String reportName, List<QueryParamDTO> queries) {
        File jasperFile = new File(reportesRuta + reportName + ".jasper");
        if (!jasperFile.exists()) {
            throw new ReportNotFoundException(reportName);
        }

        try {
            Map<String, Object> params = buildParams(queries);

            try (InputStream in = new FileInputStream(jasperFile)) {
                return JasperFillManager.fillReport(in, params, new JREmptyDataSource());
            }
        } catch (ReportNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ReportGenerationException("Error al generar el reporte: " + reportName, e);
        }
    }

    private Map<String, Object> buildParams(List<QueryParamDTO> queries) {
        Map<String, Object> params = new HashMap<>();
        params.put("SUBREPORT_DIR", reportesRuta);

        for (QueryParamDTO q : queries) {
            List<Map<String, Object>> rows = queryExecutor.execute(q.getQuery());
            Collection<Map<String, ?>> data = rows.stream()
                    .map(m -> (Map<String, ?>) m)
                    .collect(Collectors.toList());
            params.put(q.getParam(), new JRMapCollectionDataSource(data));
        }

        return params;
    }
}
