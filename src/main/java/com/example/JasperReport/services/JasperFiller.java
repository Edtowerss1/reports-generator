package com.example.JasperReport.services;

import com.example.JasperReport.dtos.QueryParamDTO;
import com.example.JasperReport.exceptions.ReportGenerationException;
import com.example.JasperReport.exceptions.ReportNotFoundException;
import com.example.JasperReport.tenant.Tenant;
import com.example.JasperReport.tenant.TenantContext;
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

    /**
     * Resolve the main datasource for the report.
     *
     * IMPORTANT: Different reports have different datasource requirements:
     * - SimpleReport: Needs DS_DATOS as main datasource for field evaluation.
     * - MasterReport: Uses JREmptyDataSource as main (with DS_DATOS passed to subreport only).
     * - Other reports: Use JREmptyDataSource by default.
     *
     * This method implements report-specific datasource routing. Do not add new
     * special cases without explicit business requirement and template review.
     *
     * @param reportName the name of the report template
     * @param params parameters map containing datasources
     * @return the main datasource for the report
     */
    private JRDataSource resolveMainDataSource(String reportName, Map<String, Object> params) {
        // SimpleReport requires DS_DATOS as main datasource
        if ("SimpleReport".equalsIgnoreCase(reportName)) {
            Object dataSource = params.get("DS_DATOS");
            if (dataSource instanceof JRDataSource) {
                return (JRDataSource) dataSource;
            }
        }

        return new JREmptyDataSource();
    }
}
