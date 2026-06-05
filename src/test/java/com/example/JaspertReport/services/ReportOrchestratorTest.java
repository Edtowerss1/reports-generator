package com.example.JaspertReport.services;

import com.example.JaspertReport.dtos.PrintRequestDTO;
import com.example.JaspertReport.dtos.QueryParamDTO;
import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.dtos.ReportResult;
import com.example.JaspertReport.exceptions.ReportNotAllowedException;
import com.example.JaspertReport.services.exporters.ExporterRegistry;
import com.example.JaspertReport.services.exporters.ReportExporter;
import com.example.JaspertReport.tenant.Tenant;
import com.example.JaspertReport.tenant.TenantContext;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReportOrchestrator with manual test doubles.
 */
class ReportOrchestratorTest {

    private final StubAllowlistService allowlistService = new StubAllowlistService();
    private final StubJasperFiller jasperFiller = new StubJasperFiller();
    private ReportOrchestrator orchestrator;

    private final Tenant acmeTenant = new Tenant(
        "acme",
        "/reportes/acme/",
        Set.of("ventas", "stock"),
        Set.of("PDF"),
        new Tenant.Datasource("jdbc:mysql://host/acme", "u", "p", "com.mysql.cj.jdbc.Driver")
    );

    @BeforeEach
    void setUp() {
        orchestrator = new ReportOrchestrator(
            jasperFiller,
            new StubExporterRegistry(),
            new StubReportPrintService(),
            allowlistService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        allowlistService.reset();
        jasperFiller.reset();
    }

    @Test
    void shouldProceedWhenReportIsAllowed() {
        TenantContext.set(acmeTenant);
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setFormat("PDF");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        allowlistService.setAllowed(true);

        ReportResult result = orchestrator.generate(request);

        assertNotNull(result);
        assertArrayEquals(StubReportExporter.FIXED_CONTENT, result.getContent());
        assertEquals("application/pdf", result.getContentType());
        assertEquals("pdf", result.getFileExtension());
        assertEquals("ventas", allowlistService.lastCheckedReport);
        assertEquals("acme", allowlistService.lastCheckedTenant);
        assertTrue(jasperFiller.wasFilled);
    }

    @Test
    void shouldThrowWhenReportIsNotAllowed() {
        TenantContext.set(acmeTenant);
        var request = new ReportRequestDTO();
        request.setReportName("nomina");
        request.setFormat("PDF");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        allowlistService.setAllowed(false);

        assertThrows(ReportNotAllowedException.class,
            () -> orchestrator.generate(request));
        assertFalse(jasperFiller.wasFilled);
    }

    @Test
    void shouldCheckAllowlistInPrintFlow() {
        TenantContext.set(acmeTenant);
        var request = new PrintRequestDTO();
        request.setReportName("stock");
        request.setPrinterName("Printer1");
        request.setCopies(2);
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        allowlistService.setAllowed(true);

        orchestrator.print(request);

        assertEquals("acme", allowlistService.lastCheckedTenant);
        assertEquals("stock", allowlistService.lastCheckedReport);
        assertTrue(jasperFiller.wasFilled);
    }

    @Test
    void shouldThrowInPrintWhenReportIsNotAllowed() {
        TenantContext.set(acmeTenant);
        var request = new PrintRequestDTO();
        request.setReportName("secret");
        request.setPrinterName("P1");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        allowlistService.setAllowed(false);

        assertThrows(ReportNotAllowedException.class,
            () -> orchestrator.print(request));
        assertFalse(jasperFiller.wasFilled);
    }

    @Test
    void shouldThrowWhenNoTenantInContext() {
        var request = new ReportRequestDTO();
        request.setReportName("ventas");
        request.setFormat("PDF");
        request.setQueries(List.of(createQuery("q1", "SELECT 1")));

        allowlistService.setAllowed(true);

        assertThrows(ReportNotAllowedException.class,
            () -> orchestrator.generate(request));
    }

    private static QueryParamDTO createQuery(String param, String query) {
        var q = new QueryParamDTO();
        q.setParam(param);
        q.setQuery(query);
        return q;
    }

    // --- Test Doubles ---

    static class StubAllowlistService implements ReportAllowlistService {
        String lastCheckedTenant;
        String lastCheckedReport;
        private boolean allowed;

        void setAllowed(boolean allowed) { this.allowed = allowed; }

        @Override
        public boolean isAllowed(String tenantId, String reportName) {
            this.lastCheckedTenant = tenantId;
            this.lastCheckedReport = reportName;
            return allowed;
        }

        void reset() {
            lastCheckedTenant = null;
            lastCheckedReport = null;
            allowed = false;
        }
    }

    static class StubJasperFiller extends JasperFiller {
        boolean wasFilled;

        StubJasperFiller() {
            super(null, null, null);
        }

        @Override
        public JasperPrint fill(String reportName, List<QueryParamDTO> queries) {
            this.wasFilled = true;
            return null; // orchestrator passes it to exporter/print which are stubbed
        }

        void reset() { wasFilled = false; }
    }

    static class StubExporterRegistry extends ExporterRegistry {
        StubExporterRegistry() { super(List.of()); }

        @Override
        public ReportExporter getExporter(String format) {
            return new StubReportExporter();
        }
    }

    static class StubReportExporter implements ReportExporter {
        static final byte[] FIXED_CONTENT = new byte[]{1, 2, 3};

        @Override
        public byte[] export(JasperPrint jasperPrint) { return FIXED_CONTENT; }

        @Override
        public String getContentType() { return "application/pdf"; }

        @Override
        public String getFileExtension() { return "pdf"; }

        @Override
        public String getSupportedFormat() { return "PDF"; }
    }

    static class StubReportPrintService extends ReportPrintService {
        @Override
        public void print(JasperPrint jasperPrint, String printerName, int copies) { /* no-op */ }
    }
}
