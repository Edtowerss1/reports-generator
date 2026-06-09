# Exploration: fix-project-name

### Summary
- 72 repository files reference `JaspertReport` (excluding generated `target/` artifacts).
- All Java files under `src/main/java/com/example/JaspertReport/` and `src/test/java/com/example/JaspertReport/` use the old package path and must be renamed with the directory move.
- No hardcoded Java string literals containing `JaspertReport` were found in `src/main/java` or `src/test/java`.

### File Inventory

### Directory Renames
- `src/main/java/com/example/JaspertReport/` → `src/main/java/com/example/JasperReport/`
- `src/test/java/com/example/JaspertReport/` → `src/test/java/com/example/JasperReport/`

#### pom.xml (1)
- `pom.xml`

#### Java Source (39)
- `src/main/java/com/example/JaspertReport/config/WebConfig.java`
- `src/main/java/com/example/JaspertReport/config/TenantProperties.java`
- `src/main/java/com/example/JaspertReport/tenant/TenantContextInitializer.java`
- `src/main/java/com/example/JaspertReport/tenant/TokenValidator.java`
- `src/main/java/com/example/JaspertReport/tenant/DataSourceManager.java`
- `src/main/java/com/example/JaspertReport/services/JasperFiller.java`
- `src/main/java/com/example/JaspertReport/controllers/ReportController.java`
- `src/main/java/com/example/JaspertReport/tenant/TenantResolver.java`
- `src/main/java/com/example/JaspertReport/tenant/DataSourceProvider.java`
- `src/main/java/com/example/JaspertReport/tenant/ConfigBasedTenantResolver.java`
- `src/main/java/com/example/JaspertReport/services/TenantScopedTemplateResolver.java`
- `src/main/java/com/example/JaspertReport/services/TemplateResolver.java`
- `src/main/java/com/example/JaspertReport/services/ReportOrchestrator.java`
- `src/main/java/com/example/JaspertReport/services/ReportCompiler.java`
- `src/main/java/com/example/JaspertReport/services/ReportAllowlistService.java`
- `src/main/java/com/example/JaspertReport/services/LazyReportCompiler.java`
- `src/main/java/com/example/JaspertReport/services/QueryExecutor.java`
- `src/main/java/com/example/JaspertReport/services/ConfigBasedAllowlistService.java`
- `src/main/java/com/example/JaspertReport/exceptions/GlobalExceptionHandler.java`
- `src/main/java/com/example/JaspertReport/JaspertReportApplication.java`
- `src/main/java/com/example/JaspertReport/tenant/TenantContext.java`
- `src/main/java/com/example/JaspertReport/tenant/Tenant.java`
- `src/main/java/com/example/JaspertReport/exceptions/ReportNotAllowedException.java`
- `src/main/java/com/example/JaspertReport/exceptions/TenantResolutionException.java`
- `src/main/java/com/example/JaspertReport/dtos/QueryParamDTO.java`
- `src/main/java/com/example/JaspertReport/services/ReportPrintService.java`
- `src/main/java/com/example/JaspertReport/services/exporters/ReportExporter.java`
- `src/main/java/com/example/JaspertReport/services/exporters/PdfReportExporter.java`
- `src/main/java/com/example/JaspertReport/services/exporters/ExporterRegistry.java`
- `src/main/java/com/example/JaspertReport/dtos/ReportResult.java`
- `src/main/java/com/example/JaspertReport/dtos/PrintRequestDTO.java`
- `src/main/java/com/example/JaspertReport/dtos/ReportRequestDTO.java`
- `src/main/java/com/example/JaspertReport/exceptions/ReportNotFoundException.java`
- `src/main/java/com/example/JaspertReport/exceptions/PrinterNotFoundException.java`
- `src/main/java/com/example/JaspertReport/exceptions/ReportGenerationException.java`
- `src/main/java/com/example/JaspertReport/exceptions/InvalidFormatException.java`
- `src/main/java/com/example/JaspertReport/exceptions/ReportPrintException.java`
- `src/main/java/com/example/JaspertReport/services/exporters/HtmlReportExporter.java`
- `src/main/java/com/example/JaspertReport/services/exporters/DocxReportExporter.java`

#### Java Test (21)
- `src/test/java/com/example/JaspertReport/services/JasperFillerTest.java`
- `src/test/java/com/example/JaspertReport/services/TenantScopedTemplateResolverTest.java`
- `src/test/java/com/example/JaspertReport/services/ReportOrchestratorTest.java`
- `src/test/java/com/example/JaspertReport/services/LazyReportCompilerTest.java`
- `src/test/java/com/example/JaspertReport/services/ConfigBasedAllowlistServiceTest.java`
- `src/test/java/com/example/JaspertReport/controllers/ReportControllerTest.java`
- `src/test/java/com/example/JaspertReport/JaspertReportApplicationTests.java`
- `src/test/java/com/example/JaspertReport/config/WebConfigTest.java`
- `src/test/java/com/example/JaspertReport/config/TenantPropertiesTest.java`
- `src/test/java/com/example/JaspertReport/tenant/DataSourceManagerTest.java`
- `src/test/java/com/example/JaspertReport/tenant/ConfigBasedTenantResolverTest.java`
- `src/test/java/com/example/JaspertReport/tenant/TenantContextTest.java`
- `src/test/java/com/example/JaspertReport/tenant/TenantTest.java`
- `src/test/java/com/example/JaspertReport/exceptions/ReportNotAllowedExceptionTest.java`
- `src/test/java/com/example/JaspertReport/exceptions/TenantResolutionExceptionTest.java`
- `src/test/java/com/example/JaspertReport/services/QueryExecutorTest.java`
- `src/test/java/com/example/JaspertReport/exceptions/GlobalExceptionHandlerTest.java`
- `src/test/java/com/example/JaspertReport/TenantInterceptorIntegrationTest.java`
- `src/test/java/com/example/JaspertReport/MultiTenantE2ETest.java`
- `src/test/java/com/example/JaspertReport/tenant/TokenValidatorTest.java`
- `src/test/java/com/example/JaspertReport/tenant/TenantContextInitializerTest.java`

#### Docs (6)
- `README.md`
- `DEPLOYMENT.md`
- `DEVELOPMENT.md`
- `CONTRIBUTING.md`
- `PRODUCTION.md`
- `src/main/resources/reportes/README.md`

#### Config (3)
- `src/main/resources/application.properties`
- `src/main/resources/application-test.properties`
- `src/test/resources/application-test.properties`

#### Other (2)
- `postman/JaspertReport-MultiTenant.postman_collection.json`
- `openspec/changes/archive/2026-06-05-multi-tenant-engine/explore.md`

### String Literal Occurrences
- None found in `src/main/java` or `src/test/java` when searching for quoted `JaspertReport` string literals.
- Non-Java text/config references that still need attention: `pom.xml`, the three `application*.properties` files, the five top-level docs, `src/main/resources/reportes/README.md`, the Postman collection, and the archived OpenSpec exploration note.

### Risk Assessment
- Package/path rename should be done with `git mv` so history stays readable and IDEs do not get confused by a pure content rewrite.
- `pom.xml` must keep `artifactId`, `name`, and `mainClass` aligned with the renamed package or the build/startup will drift.
- `spring.application.name` and logging categories still use `JaspertReport`; leaving them unchanged will produce mixed runtime identifiers.
- Generated `target/` reports already contain the old name and will keep showing it until a clean rebuild; do not treat them as source.
- Archived OpenSpec artifacts intentionally preserve historical names; changing them would rewrite audit history.
