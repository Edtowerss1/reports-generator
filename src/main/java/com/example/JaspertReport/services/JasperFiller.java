package com.example.JaspertReport.services;

import com.example.JaspertReport.dtos.QueryParamDTO;
import com.example.JaspertReport.exceptions.ReportGenerationException;
import com.example.JaspertReport.exceptions.ReportNotFoundException;
import com.example.JaspertReport.tenant.Tenant;
import com.example.JaspertReport.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JasperFiller {

    private final QueryExecutor queryExecutor;
    private final TemplateResolver templateResolver;
    private final ReportCompiler reportCompiler;

    public JasperFiller(QueryExecutor queryExecutor,
                        TemplateResolver templateResolver,
                        ReportCompiler reportCompiler) {
        this.queryExecutor = queryExecutor;
        this.templateResolver = templateResolver;
        this.reportCompiler = reportCompiler;
    }

    public JasperPrint fill(String reportName, List<QueryParamDTO> queries) {
        Tenant tenant = TenantContext.getCurrentTenant();
        String tenantId = tenant.id();

        try {
            // Resolve the .jasper template path via the tenant-scoped resolver
            Path jasperPath = templateResolver.resolve(tenantId, reportName);

            // Lazy compilation: compile .jrxml → .jasper if needed
            Path jrxmlPath = jasperPath.resolveSibling(reportName + ".jrxml");
            Path compiledPath = reportCompiler.compileIfNeeded(jrxmlPath);

            Map<String, Object> params = buildParams(queries, tenant);
            JRDataSource mainDataSource = resolveMainDataSource(reportName, params);

            try (InputStream in = new FileInputStream(compiledPath.toFile())) {
                return JasperFillManager.fillReport(in, params, mainDataSource);
            }
        } catch (ReportNotFoundException e) {
            throw e;
        } catch (ReportGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al generar el reporte {}", reportName, e);
            throw new ReportGenerationException("Error al generar el reporte: " + reportName, e);
        }
    }

    private Map<String, Object> buildParams(List<QueryParamDTO> queries, Tenant tenant) {
        Map<String, Object> params = new HashMap<>();
        String tenantRuta = tenant.reportesRuta();
        // Ensure trailing slash for SUBREPORT_DIR
        if (!tenantRuta.endsWith("/")) {
            tenantRuta = tenantRuta + "/";
        }
        params.put("SUBREPORT_DIR", tenantRuta);

        for (QueryParamDTO q : queries) {
            try {
                List<Map<String, Object>> rows = queryExecutor.execute(q.getQuery(), q.getDatasource());
                Collection<Map<String, ?>> data = rows.stream()
                        .map(m -> (Map<String, ?>) m)
                        .collect(Collectors.toList());
                params.put(q.getParam(), new JRMapCollectionDataSource(data));
            } catch (Exception e) {
                String datasource = q.getDatasource() == null ? "default" : q.getDatasource();
                String message = "Error ejecutando query para param '" + q.getParam() + "'";
                log.error("{} (datasource={})", message, datasource, e);
                throw new ReportGenerationException(message, e);
            }
        }

        return params;
    }

    private JRDataSource resolveMainDataSource(String reportName, Map<String, Object> params) {
        if ("StickerQR".equalsIgnoreCase(reportName)) {
            Object dataSource = params.get("DS_STICKER");
            if (dataSource instanceof JRDataSource) {
                return (JRDataSource) dataSource;
            }
        }
        return new JREmptyDataSource();
    }
}
