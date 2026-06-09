# Delta for Project Rename: JaspertReport → JasperReport

## ADDED Requirements

### Requirement: Java Package and Directory Rename

Java source and test packages MUST use `com.example.JasperReport`. Directory trees under
`src/main/java/com/example/` and `src/test/java/com/example/` MUST match.
No Java file SHALL contain `com.example.JaspertReport`.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| P1 | Source package correct | 39 files in `src/main/java/com/example/JasperReport/` | File inspected | `package com.example.JasperReport` |
| P2 | Test package correct | 21 files in `src/test/java/com/example/JasperReport/` | File inspected | `package com.example.JasperReport` |
| P3 | Directory matches package | Maven standard layout | Directory listing | Trees end at `com/example/JasperReport/` |
| P4 | No legacy package refs | Full `src/` tree | `rg "com\.example\.JaspertReport"` | Zero matches |

### Requirement: Maven Build Identity

`pom.xml` MUST declare `<artifactId>JasperReport</artifactId>`, `<name>JasperReport</name>`,
and `<mainClass>com.example.JasperReport.JasperReportApplication</mainClass>`.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| B1 | Artifact ID | `pom.xml` | Read `<artifactId>` | `JasperReport` |
| B2 | Project name | `pom.xml` | Read `<name>` | `JasperReport` |
| B3 | Entry point | `pom.xml` spring-boot-maven-plugin | Read `<mainClass>` | `com.example.JasperReport.JasperReportApplication` |

### Requirement: Spring Runtime Configuration

All `.properties` files MUST use `spring.application.name=JasperReport` and
`logging.level.com.example.JasperReport=DEBUG`.

DB URLs containing `jaspertreport` SHALL be updated to `jasperreport`.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| C1 | Default profile app name | `application.properties` | Read `spring.application.name` | `JasperReport` |
| C2 | Default profile logging | `application.properties` | Read `logging.level.com.example` | `JasperReport=DEBUG` |
| C3 | Test profile logging (main) | `application-test.properties` | Read `logging.level.com.example` | `JasperReport=DEBUG` |
| C4 | Test profile logging (test) | `src/test/resources/application-test.properties` | Read `logging.level.com.example` | `JasperReport=DEBUG` |
| C5 | DB URL consistency | `application.properties` | Read default datasource URL | Contains `jasperreport`, not `jaspertreport` |

### Requirement: Documentation References

All project documentation SHALL use `JasperReport`. Infrastructure path references
(`/opt/jaspertreport/`, `jaspertreport_dev`, `logs/jaspertreport.log`, JAR filenames)
SHALL be updated.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| D1 | Root docs clean | `README.md`, `DEPLOYMENT.md`, `DEVELOPMENT.md`, `CONTRIBUTING.md`, `PRODUCTION.md` | Scan for `JaspertReport` | Zero matches |
| D2 | Nested doc clean | `src/main/resources/reportes/README.md` | Scan for `JaspertReport` | Zero matches |
| D3 | Path conventions | `DEPLOYMENT.md`, `PRODUCTION.md`, `DEVELOPMENT.md` | Read paths/DB names | Use `jasperreport` (lowercase) |

### Requirement: Postman Collection Update

The Postman collection MUST be renamed to `JasperReport-MultiTenant.postman_collection.json`.
Internal JSON strings SHALL reference `JasperReport`.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| PM1 | File renamed | `postman/` directory | List files | `JasperReport-MultiTenant.postman_collection.json` exists |
| PM2 | Content updated | Collection JSON | Read `name` and `description` | Both use `JasperReport` |

### Requirement: Clean Build and Test Verification

`./mvnw clean test` MUST pass with zero failures. No stale target artifacts SHALL
produce false positives.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| V1 | Compiles cleanly | All rename changes applied; `./mvnw clean` | `./mvnw compile` | Build succeeds; zero errors |
| V2 | Tests pass | Successful compilation | `./mvnw test` | All tests pass; zero failures |
| V3 | No stale artifacts | Old `target/` directories | `./mvnw clean` before build | Old name absent from compiled output |

### Requirement: Residual Reference Elimination

A scan for `JaspertReport` MUST return zero matches outside `openspec/changes/archive/`
and the change's own proposal/explore files.

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| R1 | Source tree clean | `src/` directory | Scan `JaspertReport` | Zero matches |
| R2 | Config and build clean | `pom.xml`, `*.properties`, `*.json` | Scan `JaspertReport` | Zero matches |
| R3 | Docs clean | All `.md` outside `openspec/` | Scan `JaspertReport` | Zero matches |
| R4 | History preserved | `openspec/changes/archive/` | Scan `JaspertReport` | Matches allowed (audit trail) |
