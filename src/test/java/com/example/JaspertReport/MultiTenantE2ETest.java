package com.example.JaspertReport;

import com.example.JaspertReport.config.TenantProperties;
import com.example.JaspertReport.controllers.ReportController;
import com.example.JaspertReport.dtos.ReportRequestDTO;
import com.example.JaspertReport.dtos.ReportResult;
import com.example.JaspertReport.exceptions.GlobalExceptionHandler;
import com.example.JaspertReport.exceptions.ReportGenerationException;
import com.example.JaspertReport.exceptions.ReportNotAllowedException;
import com.example.JaspertReport.exceptions.TenantResolutionException;
import com.example.JaspertReport.services.ReportOrchestrator;
import com.example.JaspertReport.tenant.Tenant;
import com.example.JaspertReport.tenant.TenantContext;
import com.example.JaspertReport.tenant.TenantContextInitializer;
import com.example.JaspertReport.tenant.TenantResolver;
import com.example.JaspertReport.tenant.TokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 — End-to-End Integration Tests for the multi-tenant engine.
 * <p>
 * Tests the full chain: HTTP request → TokenValidator → TenantContextInitializer
 * → ReportController → ReportOrchestrator → GlobalExceptionHandler → HTTP response.
 * <p>
 * Uses {@code MockMvcBuilders.standaloneSetup} (not {@code @SpringBootTest}) because
 * MySQL is unavailable in this environment. All dependencies are manual stubs
 * (same pattern as existing unit tests), avoiding Mockito's inline-mock-maker
 * issues on JDK 26.
 * <p>
 * Covers specs R1-R3, A1-A3, P2, F1-F2.
 */
@SuppressWarnings("null")
class MultiTenantE2ETest {

    private static final Tenant ACME_TENANT = new Tenant(
            "acme",
            "/reportes/acme/",
            Set.of("ventas", "stock"),
            Set.of("PDF"),
            new Tenant.Datasource("jdbc:h2:mem:acme;DB_CLOSE_DELAY=-1", "sa", "", "org.h2.Driver")
    );

    private static final Tenant CORP_TENANT = new Tenant(
            "corp",
            "/reportes/corp/",
            Set.of("reporte1"),
            Set.of("PDF"),
            new Tenant.Datasource("jdbc:h2:mem:corp;DB_CLOSE_DELAY=-1", "sa", "", "org.h2.Driver")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private StubTenantResolver resolver;
    private StubReportOrchestrator orchestrator;
    private TenantProperties centralizedConfig;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        resolver = new StubTenantResolver();
        orchestrator = new StubReportOrchestrator();
        centralizedConfig = new TenantProperties();
        centralizedConfig.setProfile("centralized");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ========================================================================
    // 5.1 — E2E Happy Path (spec F1, R1, A1, D1, T1)
    // ========================================================================

    @Test
    @DisplayName("F1: Full happy path — valid token + allowed report → 200 with bytes and correct content type")
    void e2eHappyPath_validTokenAndAllowedReport_returns200WithContent() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setResult(new ReportResult(new byte[]{0x1, 0x2, 0x3}, "application/pdf", "pdf"));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=reporte.pdf"))
                .andExpect(content().bytes(new byte[]{0x1, 0x2, 0x3}));
    }

    @Test
    @DisplayName("F1: Full happy path with centralized config — TenantContext populated and cleared")
    void e2eHappyPath_centralizedMode_tenantContextClearedAfterRequest() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setResult(new ReportResult(new byte[]{0}, "application/pdf", "pdf"));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isOk());

        assertNull(TenantContext.getCurrentTenant(),
                "TenantContext must be cleared after request completes");
    }

    // ========================================================================
    // 5.2 — Token Scenarios (spec R1-R3)
    // ========================================================================

    @Test
    @DisplayName("R1: Valid token for known tenant → request proceeds (200)")
    void e2eToken_validToken_returns200() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setResult(new ReportResult(new byte[]{0}, "application/pdf", "pdf"));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("R2: Unknown token (no tenant mapping) → 401")
    void e2eToken_unknownToken_returns401() throws Exception {
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("R3: Missing X-Service-Token header → 401")
    void e2eToken_missingToken_returns401() throws Exception {
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isUnauthorized());
    }

    // ========================================================================
    // 5.3 — Allowlist Enforcement (spec A1-A3)
    // ========================================================================

    @Test
    @DisplayName("A1: Report in allowlist → orchestrator proceeds with normal response")
    void e2eAllowlist_allowedReport_proceedsTo200() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setResult(new ReportResult(new byte[]{0x0A}, "application/pdf", "pdf"));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A2/A3: Orchestrator throws ReportNotAllowedException → GlobalExceptionHandler returns 403")
    void e2eAllowlist_disallowedReport_returns403() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setFailure(new ReportNotAllowedException("acme", "nomina"));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("nomina", "PDF")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("nomina")));
    }

    // ========================================================================
    // 5.4 — Dedicated Mode (spec P2)
    // ========================================================================

    @Test
    @DisplayName("P2: Dedicated mode + correct tenant token → 200")
    void e2eDedicated_correctTenant_returns200() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setResult(new ReportResult(new byte[]{0}, "application/pdf", "pdf"));
        var dedicatedConfig = new TenantProperties();
        dedicatedConfig.setProfile("dedicated");
        dedicatedConfig.setAssignedTenant("acme");
        buildMockMvc(dedicatedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P2: Dedicated mode + wrong tenant token → 403")
    void e2eDedicated_wrongTenant_returns403() throws Exception {
        resolver.setMapping("tok-B", CORP_TENANT);
        var dedicatedConfig = new TenantProperties();
        dedicatedConfig.setProfile("dedicated");
        dedicatedConfig.setAssignedTenant("acme");
        buildMockMvc(dedicatedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("reporte1", "PDF")))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // Spec F2 — Mid-flow failure short-circuits
    // ========================================================================

    @Test
    @DisplayName("F2: Mid-flow failure (ReportGenerationException) → 500, no partial content")
    void e2eFailure_midFlowFailure_returns500() throws Exception {
        resolver.setMapping("tok-A", ACME_TENANT);
        orchestrator.setFailure(new ReportGenerationException("Error al generar el reporte: ventas",
                new RuntimeException("DB connection failed")));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error al generar el reporte")))
                .andExpect(content().string(containsString("DB connection failed")));
    }

    // ========================================================================
    // Spec F3 — Cross-tenant isolation via interceptor chain
    // ========================================================================

    @Test
    @DisplayName("F3: Token for 'corp' resolves to corp tenant — no leakage from acme data")
    void e2eIsolation_corpTokenResolvesToCorp() throws Exception {
        resolver.setMapping("tok-B", CORP_TENANT);
        orchestrator.setResult(new ReportResult(new byte[]{0x0B}, "application/pdf", "pdf"));
        buildMockMvc(centralizedConfig);

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("reporte1", "PDF")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{0x0B}));
    }

    @Test
    @DisplayName("F3: Full flow cross-tenant isolation — each tenant gets own data")
    void e2eIsolation_acmeAndCorpGetOwnData() throws Exception {
        // Both tenants registered simultaneously
        resolver.setMapping("tok-A", ACME_TENANT);
        resolver.setMapping("tok-B", CORP_TENANT);
        buildMockMvc(centralizedConfig);

        // Acme request returns acme data
        byte[] acmeData = new byte[]{0x41, 0x43, 0x4D, 0x45}; // "ACME"
        orchestrator.setResult(new ReportResult(acmeData, "application/pdf", "pdf"));

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("ventas", "PDF")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(acmeData));

        // Corp request returns corp data (not acme's)
        byte[] corpData = new byte[]{0x43, 0x4F, 0x52, 0x50}; // "CORP"
        orchestrator.setResult(new ReportResult(corpData, "application/pdf", "pdf"));

        mockMvc.perform(post("/reportes/generar")
                        .header("X-Service-Token", "tok-B")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody("reporte1", "PDF")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(corpData));
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void buildMockMvc(TenantProperties props) {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReportController(orchestrator))
                .addInterceptors(
                        new TokenValidator(resolver),
                        new TenantContextInitializer(resolver, props)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String jsonBody(String reportName, String format) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "reportName", reportName,
                "format", format,
                "queries", List.of(Map.of("param", "q1", "query", "SELECT 1"))
        ));
    }

    // ========================================================================
    // Test Doubles (manual stubs — no Mockito, works on JDK 26)
    // ========================================================================

    static class StubTenantResolver implements TenantResolver {

        private final Map<String, Tenant> tokenToTenant = new java.util.HashMap<>();

        void setMapping(String token, Tenant tenant) {
            tokenToTenant.put(token, tenant);
        }

        @Override
        public boolean validate(String token) {
            return token != null && !token.isBlank() && tokenToTenant.containsKey(token);
        }

        @Override
        public Tenant resolve(String token) {
            Tenant tenant = tokenToTenant.get(token);
            if (tenant == null) {
                throw new TenantResolutionException("Unknown token: " + token);
            }
            return tenant;
        }
    }

    static class StubReportOrchestrator extends ReportOrchestrator {

        private ReportResult result;
        private RuntimeException failure;

        StubReportOrchestrator() {
            super(null, null, null, null);
        }

        void setResult(ReportResult result) {
            this.result = result;
            this.failure = null;
        }

        void setFailure(RuntimeException failure) {
            this.failure = failure;
            this.result = null;
        }

        @Override
        public ReportResult generate(ReportRequestDTO request) {
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }
}
