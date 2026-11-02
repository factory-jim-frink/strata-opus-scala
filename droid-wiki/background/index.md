# Background

These pages explain why Strata is built the way it is and where it is easy to go wrong. They complement the module and feature pages, which describe what the code does.

| Page | What it covers |
| --- | --- |
| [Design decisions](design-decisions.md) | Library-wide choices visible in the code: immutable Joda-Beans, resolve before pricing, `Result` in the engine, the split between calculation engine and pricers, point and parameter sensitivities, `ExtendedEnum` with INI configuration, the Java 8 API level, `impl` packages, `double` versus `BigDecimal` |
| [Scala migration](scala-migration.md) | The migration of `collect` and `basics` to Scala 3.9.0 on the `auto_model_migration` branch: scope, compatibility contract, decisions, recorded validation, and the two fixes made afterwards |
| [Pitfalls](pitfalls.md) | Danger zones: generated code markers, the Scala modules' binary API, function lookup by exact class, configuration loading, holiday data, build checks that behave differently in worktrees or on CI |

For the project's history from 2014 onwards, see [Lore](../lore.md). For the code patterns these decisions lead to, see [Patterns and conventions](../how-to-contribute/patterns-and-conventions.md).
