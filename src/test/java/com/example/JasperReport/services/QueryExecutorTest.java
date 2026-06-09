package com.example.JasperReport.services;

import com.example.JasperReport.exceptions.TenantResolutionException;
import com.example.JasperReport.tenant.DataSourceProvider;
import com.example.JasperReport.tenant.Tenant;
import com.example.JasperReport.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class QueryExecutorTest {

    @Mock
    private DataSourceProvider dataSourceProvider;

    private QueryExecutor queryExecutor;

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
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExecuteQueryUsingTenantSpecificTemplate() {
        TenantContext.set(acmeTenant);
        String sql = "SELECT * FROM reports";
        List<Map<String, Object>> expected = List.of(Map.of("id", 1));
        JdbcTemplate testTemplate = new JdbcTemplate() {
            @Override
            public List<Map<String, Object>> queryForList(String sqlToExecute) {
                assertEquals(sql, sqlToExecute);
                return expected;
            }
        };

        when(dataSourceProvider.getTemplate("acme")).thenReturn(testTemplate);

        List<Map<String, Object>> result = queryExecutor.execute(sql, "ignored-datasource");

        assertSame(expected, result);
        verify(dataSourceProvider).getTemplate("acme");
    }

    @Test
    void shouldDelegateToProviderWithCurrentTenantId() {
        TenantContext.set(acmeTenant);
        String sql = "SELECT COUNT(*) FROM orders";
        List<Map<String, Object>> expected = List.of(Map.of("count", 42));
        JdbcTemplate testTemplate = new JdbcTemplate() {
            @Override
            public List<Map<String, Object>> queryForList(String sqlToExecute) {
                assertEquals(sql, sqlToExecute);
                return expected;
            }
        };

        when(dataSourceProvider.getTemplate("acme")).thenReturn(testTemplate);

        List<Map<String, Object>> result = queryExecutor.execute(sql, null);

        assertSame(expected, result);
        verify(dataSourceProvider).getTemplate("acme");
    }

    @Test
    void shouldThrowWhenNoTenantInContext() {
        assertThrows(TenantResolutionException.class,
            () -> queryExecutor.execute("SELECT 1", null));
    }

    @Test
    void shouldNotAccessOtherTenantDatasource() {
        // D3: Cross-tenant isolation — query for acme must not reach corp's datasource
        TenantContext.set(acmeTenant);
        String sql = "SELECT * FROM reports";
        JdbcTemplate testTemplate = new JdbcTemplate() {
            @Override
            public List<Map<String, Object>> queryForList(String sqlToExecute) {
                return List.of();
            }
        };

        when(dataSourceProvider.getTemplate("acme")).thenReturn(testTemplate);

        queryExecutor.execute(sql, null);

        verify(dataSourceProvider).getTemplate("acme");
        verify(dataSourceProvider, never()).getTemplate("corp");
        verifyNoMoreInteractions(dataSourceProvider);
    }

    @Test
    void shouldRejectNullSql() {
        assertThrows(NullPointerException.class,
            () -> queryExecutor.execute(null, "ignored"));
    }
}
