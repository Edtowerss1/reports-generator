package com.example.JaspertReport.services;

import com.example.JaspertReport.dtos.QueryParamDTO;
import com.example.JaspertReport.exceptions.ReportGenerationException;
import com.example.JaspertReport.exceptions.ReportNotFoundException;
import com.example.JaspertReport.tenant.DataSourceProvider;
import com.example.JaspertReport.tenant.Tenant;
import com.example.JaspertReport.tenant.TenantContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JasperFillerTest {

    @Mock
    private TemplateResolver templateResolver;

    @Mock
    private ReportCompiler reportCompiler;

    @Mock
    private DataSourceProvider dataSourceProvider;

    private QueryExecutor queryExecutor;
    private JasperFiller jasperFiller;

    private final Tenant acmeTenant = new Tenant(
        "acme",
        "/reportes/acme/",
        Set.of("ventas", "stock"),
        Set.of("PDF"),
        new Tenant.Datasource("jdbc:mysql://host/acme", "u", "p", "com.mysql.cj.jdbc.Driver")
    );

    @BeforeEach
    void setUp() {
        queryExecutor = new QueryExecutor(dataSourceProvider);
        jasperFiller = new JasperFiller(queryExecutor, templateResolver, reportCompiler);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /**
     * Creates a stub JdbcTemplate that returns empty results for any query.
     * Uses anonymous subclass (Mockito cannot instrument concrete classes on JDK 26).
     */
    private void stubQueryTemplate() {
        JdbcTemplate stubTemplate = new JdbcTemplate() {
            @Override
            public List<Map<String, Object>> queryForList(String sql) {
                return List.of();
            }
        };
        when(dataSourceProvider.getTemplate("acme")).thenReturn(stubTemplate);
    }

    @Test
    void shouldDelegateToTemplateResolverAndCompiler(@TempDir Path tempDir) throws Exception {
        TenantContext.set(acmeTenant);

        Path jasperPath = tempDir.resolve("ventas.jasper");
        Files.createFile(jasperPath);

        when(templateResolver.resolve("acme", "ventas")).thenReturn(jasperPath.toAbsolutePath());
        Path jrxmlPath = jasperPath.resolveSibling("ventas.jrxml");
        when(reportCompiler.compileIfNeeded(jrxmlPath)).thenReturn(jasperPath.toAbsolutePath());
        stubQueryTemplate();

        var queries = List.of(createQuery("q1", "SELECT 1"));

        try (MockedStatic<JasperFillManager> jfm = mockStatic(JasperFillManager.class)) {
            jfm.when(() -> JasperFillManager.fillReport(
                    any(InputStream.class), anyMap(), any(JRDataSource.class)))
                .thenThrow(new RuntimeException("expected"));

            assertThrows(ReportGenerationException.class,
                () -> jasperFiller.fill("ventas", queries));

            verify(templateResolver).resolve("acme", "ventas");
            verify(reportCompiler).compileIfNeeded(jrxmlPath);
        }
    }

    @Test
    void shouldPropagateTemplateNotFoundException() {
        TenantContext.set(acmeTenant);
        when(templateResolver.resolve("acme", "missing"))
            .thenThrow(new ReportNotFoundException("missing"));

        var queries = List.of(createQuery("q1", "SELECT 1"));

        assertThrows(ReportNotFoundException.class,
            () -> jasperFiller.fill("missing", queries));
    }

    @Test
    void shouldUseCompiledPathForFill(@TempDir Path tempDir) throws Exception {
        TenantContext.set(acmeTenant);

        Path jasperPath = tempDir.resolve("stock.jasper");
        Files.createFile(jasperPath);

        when(templateResolver.resolve("acme", "stock")).thenReturn(jasperPath.toAbsolutePath());
        Path jrxmlPath = jasperPath.resolveSibling("stock.jrxml");
        when(reportCompiler.compileIfNeeded(jrxmlPath)).thenReturn(jasperPath.toAbsolutePath());
        stubQueryTemplate();

        var queries = List.of(createQuery("q1", "SELECT 1"));

        try (MockedStatic<JasperFillManager> jfm = mockStatic(JasperFillManager.class)) {
            jfm.when(() -> JasperFillManager.fillReport(
                    any(InputStream.class), anyMap(), any(JRDataSource.class)))
                .thenThrow(new RuntimeException("expected-compiled-path"));

            ReportGenerationException ex = assertThrows(ReportGenerationException.class,
                () -> jasperFiller.fill("stock", queries));
            assertTrue(ex.getMessage().contains("stock"));
            assertNotNull(ex.getCause());
            assertEquals("expected-compiled-path", ex.getCause().getMessage());
        }
    }

    @Test
    void shouldSetSubreportDirToTenantRuta(@TempDir Path tempDir) throws Exception {
        // T2: SUBREPORT_DIR must be scoped to the tenant's reportesRuta
        TenantContext.set(acmeTenant);

        Path jasperPath = tempDir.resolve("ventas.jasper");
        Files.createFile(jasperPath);

        when(templateResolver.resolve("acme", "ventas")).thenReturn(jasperPath.toAbsolutePath());
        Path jrxmlPath = jasperPath.resolveSibling("ventas.jrxml");
        when(reportCompiler.compileIfNeeded(jrxmlPath)).thenReturn(jasperPath.toAbsolutePath());
        stubQueryTemplate();

        var queries = List.of(createQuery("q1", "SELECT 1"));

        try (MockedStatic<JasperFillManager> jfm = mockStatic(JasperFillManager.class)) {
            final Map<String, Object>[] capturedParams = new Map[]{null};
            jfm.when(() -> JasperFillManager.fillReport(
                    any(InputStream.class), anyMap(), any(JRDataSource.class)))
                .thenAnswer(invocation -> {
                    capturedParams[0] = invocation.getArgument(1);
                    throw new RuntimeException("expected short-circuit");
                });

            assertThrows(ReportGenerationException.class,
                () -> jasperFiller.fill("ventas", queries));

            assertNotNull(capturedParams[0], "params map must not be null");
            assertEquals("/reportes/acme/", capturedParams[0].get("SUBREPORT_DIR"));
        }
    }

    @Test
    void shouldUseDsDatosAsMainDatasourceForSimpleReport(@TempDir Path tempDir) throws Exception {
        TenantContext.set(acmeTenant);

        Path jasperPath = tempDir.resolve("SimpleReport.jasper");
        Files.createFile(jasperPath);

        when(templateResolver.resolve("acme", "SimpleReport")).thenReturn(jasperPath.toAbsolutePath());
        Path jrxmlPath = jasperPath.resolveSibling("SimpleReport.jrxml");
        when(reportCompiler.compileIfNeeded(jrxmlPath)).thenReturn(jasperPath.toAbsolutePath());

        JdbcTemplate stubTemplate = new JdbcTemplate() {
            @Override
            public List<Map<String, Object>> queryForList(String sql) {
                return List.of(Map.of("id", "1", "nombre", "Producto", "valor", "10.00"));
            }
        };
        when(dataSourceProvider.getTemplate("acme")).thenReturn(stubTemplate);

        var queries = List.of(createQuery("DS_DATOS", "SELECT 1"));

        try (MockedStatic<JasperFillManager> jfm = mockStatic(JasperFillManager.class)) {
            final JRDataSource[] capturedDataSource = new JRDataSource[1];
            jfm.when(() -> JasperFillManager.fillReport(
                    any(InputStream.class), anyMap(), any(JRDataSource.class)))
                .thenAnswer(invocation -> {
                    capturedDataSource[0] = invocation.getArgument(2);
                    throw new RuntimeException("expected short-circuit");
                });

            assertThrows(ReportGenerationException.class,
                () -> jasperFiller.fill("SimpleReport", queries));

            assertNotNull(capturedDataSource[0], "main datasource must not be null");
            assertTrue(capturedDataSource[0] instanceof JRMapCollectionDataSource);
            assertFalse(capturedDataSource[0] instanceof JREmptyDataSource);
        }
    }

    @Test
    void shouldThrowWhenNoTenantInContext() {
        var queries = List.of(createQuery("q1", "SELECT 1"));

        assertThrows(NullPointerException.class,
            () -> jasperFiller.fill("ventas", queries));
    }

    private static QueryParamDTO createQuery(String param, String query) {
        var q = new QueryParamDTO();
        q.setParam(param);
        q.setQuery(query);
        return q;
    }
}
