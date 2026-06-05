package com.example.JaspertReport.services;

import com.example.JaspertReport.exceptions.ReportGenerationException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lazily compiles .jrxml files to .jasper when the source is newer
 * than the compiled output or the compiled file is missing.
 * <p>
 * This matches the existing {@code @PostConstruct compileReports()} pattern
 * in {@code JasperFiller} but defers compilation to first request,
 * avoiding startup contention with N tenants (design decision #4).
 */
@Slf4j
@Component
public class LazyReportCompiler implements ReportCompiler {

    @Override
    public Path compileIfNeeded(Path jrxmlPath) {
        Path jasperPath = jrxmlPath.resolveSibling(
            jrxmlPath.getFileName().toString().replace(".jrxml", ".jasper")
        );

        boolean needsCompile = !Files.exists(jasperPath)
            || jrxmlPath.toFile().lastModified() > jasperPath.toFile().lastModified();

        if (!needsCompile) {
            log.debug("Template up to date: {}", jasperPath);
            return jasperPath;
        }

        try {
            log.info("Compiling {} -> {}", jrxmlPath, jasperPath);
            JasperCompileManager.compileReportToFile(
                jrxmlPath.toString(),
                jasperPath.toString()
            );
        } catch (Exception e) {
            throw new ReportGenerationException(
                "Error compiling report: " + jrxmlPath.getFileName(), e
            );
        }

        return jasperPath;
    }
}
