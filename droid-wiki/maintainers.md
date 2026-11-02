# Maintainers

Strata has a single code owner for the whole repository, and most recent changes come from a small group of OpenGamma developers. The table below combines `.github/CODEOWNERS` with commit history on the `main` branch since 1 January 2022.

## Ownership

`.github/CODEOWNERS` contains one rule, `* @jodastephen`, so every pull request requests review from that account. Dependabot pull requests are also assigned to `jodastephen` (`.github/dependabot.yml`).

| Subsystem | Official owners (CODEOWNERS) | Recent contributors (git history) | Last activity |
| --- | --- | --- | --- |
| [collect](modules/collect.md) (`modules/collect`) | @jodastephen | Stephen Colebourne, James White, Anand | 2026-10-13 |
| [basics](modules/basics.md) (`modules/basics`) | @jodastephen | Stephen Colebourne, Andras Vig, Alexis Skitini | 2026-09-29 |
| [data](modules/data.md) (`modules/data`) | @jodastephen | Yuki Iwashita, Stephen Colebourne | 2023-05-30 |
| [product](modules/product/index.md) (`modules/product`) | @jodastephen | Stephen Colebourne, Brian Weller, James White | 2026-10-05 |
| [math](modules/math.md) (`modules/math`) | @jodastephen | Stephen Colebourne, Alexis Skitini, Guillaume Santoro | 2024-07-10 |
| [market](modules/market.md) (`modules/market`) | @jodastephen | Yuki Iwashita, Stephen Colebourne, Alexis Skitini | 2026-04-27 |
| [loader](modules/loader.md) (`modules/loader`) | @jodastephen | Stephen Colebourne, Alexis Skitini, Brian Weller | 2026-09-22 |
| [pricer](modules/pricer/index.md) (`modules/pricer`) | @jodastephen | Marc Henrard, Yuki Iwashita, Stephen Colebourne | 2026-09-29 |
| [calc](modules/calc.md) (`modules/calc`) | @jodastephen | Stephen Colebourne, Ujjwal Tyagi, Will Nicholson | 2023-06-19 |
| [measure](modules/measure.md) (`modules/measure`) | @jodastephen | Stephen Colebourne, Yuki Iwashita, James White | 2026-08-24 |
| [report](modules/report.md) (`modules/report`) | @jodastephen | Stephen Colebourne | 2022-05-13 |
| [examples](modules/examples.md) (`examples`) | @jodastephen | Stephen Colebourne, ryan-mor, factory-jim-frink | 2026-10-13 |
| Build and CI (`.circleci`, `pom.xml`, `modules/pom.xml`) | @jodastephen | Stephen Colebourne, ryan-mor, Martynas Šateika | 2026-10-13 |

## How the table was built

- Recent contributors are the most active human authors in `git log main --since=2022-01-01 -- <path>`, with authors ending in `[bot]` and `opengammacibot` (the release bot) removed. Names are listed without counts.
- Last activity is the date of the newest non-bot commit on `main` touching that path.
- The 2026-10-13 dates for `collect`, `examples` and build/CI come from factory-jim-frink's agent tooling commit on `main` ("Add agent readiness quality tooling and documentation"), not from library code changes.

## The Scala migration branch

The table reflects `main`. On the `auto_model_migration` branch, the migration of `collect` and `basics` to Scala 3.9.0 adds 26 commits after `main`, all authored by factory-jim-frink with `Co-authored-by: factory-droid[bot]` trailers. Those commits are not counted above. See [Scala migration](background/scala-migration.md) for what they changed.

## Related pages

- [Modules](modules/index.md)
- [How to contribute](how-to-contribute/index.md)
- [Deployment](deployment.md)
