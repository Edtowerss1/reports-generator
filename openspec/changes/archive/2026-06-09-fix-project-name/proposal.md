# Proposal: Rename project to JasperReport

## Intent

The project name is consistently misspelled as `JaspertReport`. This creates build/runtime drift, confusing package paths, and inconsistent documentation/configuration. The change is a mechanical rename to the correct canonical name: `JasperReport`.

## Scope

### In Scope
- Rename the Java package tree from `com.example.JaspertReport` to `com.example.JasperReport` in `src/main/java` and `src/test/java`.
- Update `pom.xml` (`artifactId`, `name`, `mainClass`) and `spring.application.name` in properties files.
- Fix documentation, Postman collection references, and the archived exploration note.

### Out of Scope
- No behavior changes, feature work, or API changes.
- No spec/model changes beyond the name correction.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- None

## Approach

- Perform a repository-wide mechanical rename.
- Use `git mv` for the package directories so history stays intact.
- Update all remaining textual references in docs/config while keeping the code paths and runtime wiring aligned.
- Finish with a clean rebuild to remove stale `target/` artifacts that still show the old name.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `src/main/java/com/example/JaspertReport/` | Modified | Package rename across 39 source files |
| `src/test/java/com/example/JaspertReport/` | Modified | Package rename across 21 test files |
| `pom.xml` | Modified | Build identity and entrypoint alignment |
| `src/main/resources/*.properties` | Modified | Application name and environment config |
| Docs / Postman / archived note | Modified | Human-facing references and historical notes |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Broken imports after rename | Medium | Rename directories and update package declarations in one pass |
| Mixed old/new runtime identifiers | Medium | Update `spring.application.name` and build metadata together |
| Stale generated artifacts | Medium | Run `mvn clean` before validation |
| Audit-history noise in archived files | Low | Limit edits to the documented reference only |

## Rollback Plan

Revert the rename commit(s), restore the original package tree and identifiers, then rebuild to confirm the old name is back consistently.

## Dependencies

- A clean Maven build environment.

## Success Criteria

- [ ] All source, test, build, config, and documentation references use `JasperReport`.
- [ ] The application compiles and tests pass after a clean rebuild.
- [ ] No residual `JaspertReport` references remain outside intentionally historical artifacts.
