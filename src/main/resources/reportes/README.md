# Example Report Templates

These are **example templates** for the JaspertReport multi-tenant engine.
They demonstrate the two main report patterns:

| Template | Type | Description |
|----------|------|-------------|
| `SimpleReport.jrxml` | Single datasource | Basic tabular report with header, detail rows, and page footer |
| `MasterReport.jrxml` | Master with subreport | Report with company header (`DS_EMPRESA`) and a subreport call to `DetailReport.jasper` |
| `DetailReport.jrxml` | Subreport | Detail lines with ID, name, quantity, price, and calculated total |

## Parameter Conventions

- `DS_*` — `JRDataSource` parameters fed by SQL query results from the engine
- `DS_EMPRESA` — `JRMapCollectionDataSource` for single-row company/header info
- `SUBREPORT_DIR` — `String` parameter set automatically per tenant by the engine

## Usage

Place `.jrxml` files in the tenant's configured `reportes-ruta` directory.
The engine compiles them to `.jasper` automatically on first request.
