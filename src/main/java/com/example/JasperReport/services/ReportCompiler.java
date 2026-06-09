package com.example.JasperReport.services;

import java.nio.file.Path;

/**
 * Compiles .jrxml templates to .jasper on demand.
 * <p>
 * Single responsibility: decide whether recompilation is needed
 * and perform it. Decouples compilation strategy (OCP) — lazy,
 * pre-compile, cache-only are all new implementations.
 */
public interface ReportCompiler {

    /**
     * Compiles the given .jrxml file if its compiled .jasper counterpart
     * is missing or stale (jrxml newer than jasper).
     *
     * @param jrxmlPath the path to the .jrxml source file
     * @return the path to the compiled .jasper file
     * @throws com.example.JasperReport.exceptions.ReportGenerationException
     *         if compilation fails
     */
    Path compileIfNeeded(Path jrxmlPath);
}
