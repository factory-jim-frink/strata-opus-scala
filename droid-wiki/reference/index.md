# Reference

This section collects lookup material for Strata: configuration files and switches, the main domain types, and the third-party libraries and Maven plugins the build uses. Each page lists file paths from the repository root so you can open the source directly.

## Pages

| Page | What it covers |
| --- | --- |
| [Configuration](configuration.md) | Every INI and CSV file under `META-INF/com/opengamma/strata/config/`, the one runtime system property, Maven flags and profiles, and the environment variables from `.env.example`. |
| [Data models](data-models.md) | The core types (trades, products, reference data, market data, curves, sensitivities, calculation results, reports), where each one lives, and how they relate. |
| [Dependencies](dependencies.md) | Third-party libraries and versions, test libraries, Maven plugins, which module uses what, and the Dependabot setup. |

## Related pages

- [Modules](../modules/index.md)
- [Reference data and configuration](../features/reference-data-and-configuration.md) explains how configuration is loaded and merged.
- [Glossary](../overview/glossary.md)
