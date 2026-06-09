package com.example.JaspertReport.services;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LazyReportCompilerTest {

    @Test
    void shouldRecompileWhenJrxmlIsNewerThanJasper(@TempDir Path tempDir) throws IOException {
        Path jrxmlPath = tempDir.resolve("ventas.jrxml");
        Files.createFile(jrxmlPath);
        jrxmlPath.toFile().setLastModified(System.currentTimeMillis());

        Path jasperPath = tempDir.resolve("ventas.jasper");
        Files.createFile(jasperPath);
        jasperPath.toFile().setLastModified(System.currentTimeMillis() - 10_000);

        try (MockedStatic<JasperCompileManager> jasperMock = mockStatic(JasperCompileManager.class)) {
            var compiler = new LazyReportCompiler();
            Path result = compiler.compileIfNeeded(jrxmlPath);

            jasperMock.verify(() ->
                JasperCompileManager.compileReportToFile(
                    jrxmlPath.toString(),
                    jasperPath.toString()
                )
            );
            assertEquals(jasperPath, result);
        }
    }

    @Test
    void shouldSkipWhenJasperIsNewerThanJrxml(@TempDir Path tempDir) throws IOException {
        Path jrxmlPath = tempDir.resolve("stock.jrxml");
        Files.createFile(jrxmlPath);
        jrxmlPath.toFile().setLastModified(System.currentTimeMillis() - 10_000);

        Path jasperPath = tempDir.resolve("stock.jasper");
        Files.createFile(jasperPath);
        jasperPath.toFile().setLastModified(System.currentTimeMillis());

        try (MockedStatic<JasperCompileManager> jasperMock = mockStatic(JasperCompileManager.class)) {
            var compiler = new LazyReportCompiler();
            Path result = compiler.compileIfNeeded(jrxmlPath);

            jasperMock.verifyNoInteractions();
            assertEquals(jasperPath, result);
        }
    }

    @Test
    void shouldRecompileWhenJasperDoesNotExist(@TempDir Path tempDir) throws IOException {
        Path jrxmlPath = tempDir.resolve("nuevo.jrxml");
        Files.createFile(jrxmlPath);
        jrxmlPath.toFile().setLastModified(System.currentTimeMillis());

        Path expectedJasper = jrxmlPath.resolveSibling("nuevo.jasper");

        try (MockedStatic<JasperCompileManager> jasperMock = mockStatic(JasperCompileManager.class)) {
            var compiler = new LazyReportCompiler();
            Path result = compiler.compileIfNeeded(jrxmlPath);

            jasperMock.verify(() ->
                JasperCompileManager.compileReportToFile(
                    jrxmlPath.toString(),
                    expectedJasper.toString()
                )
            );
            assertEquals(expectedJasper, result);
        }
    }
}
