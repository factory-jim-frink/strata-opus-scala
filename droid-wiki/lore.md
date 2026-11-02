# Lore

This page tells the history of Strata as it appears in the git history of `/home/factory-user/repos/auto_model_migration`: 4,916 commits on `main` since June 2014, about 200 tags, and the 26 commits of the `auto_model_migration` branch. Reasons given for changes are inferred from commit messages and should be read as likely rather than certain. For size and activity figures, see [By the numbers](by-the-numbers.md).

## Eras

### Foundations (June 2014 to June 2015)

Stephen Colebourne made the initial commit on 17 June 2014, containing only `.gitignore` and `README.md`. The first code was utility and value-type code that later became `collect` and `basics`. Issue keys in commit messages changed from `PLT-` to `STR-` in early 2015, which suggests the work moved from a platform team ticket stream to a dedicated Strata project.

- 2014-06-17: initial commit (`b40d079`), followed the same day by the Apache 2.0 licence and the project structure.
- 2014-06-18: `ArgChecker` arrives, at that time as `collect/src/main/java/com/opengamma/collect/validate/ArgumentChecker.java`.
- 2014-07: time series package (`PLT-353`, 2014-07-01), `Currency` (2014-07-14), and the date types `HolidayCalendar`, `DayCount` and `Tenor` (2014-07-21).
- 2014-10 to 2014-11: early top-level directories such as `identifier/`, `source/` and `data-provider/` are deleted.
- 2014-12-23: commit `6794a7b` ("PLT-566 Restructure codebase") creates the `modules/` layout with `modules/collect`, `modules/basics` and `examples/`.
- 2015-01-06: `modules/pricer` appears; `modules/market` follows on 2015-05-18.
- 2015-03-25: the first `STR-` ticket keys appear in commit messages; on 2015-03-31 the copyright headers are tidied (`STR-61`).
- 2015-06-25: first tag, `v0.7.0`.

### Pre-1.0 build-out (July 2015 to July 2016)

This was the busiest period in the history: 1,906 commits in 2015 and 1,070 in 2016, against a few hundred a year afterwards. Several experimental `-beta` modules were created and then folded into the final module set, and the module names settled into today's ten.

- 2015-07-15: Travis CI configuration is added.
- 2015-09 to 2015-10: `function-beta`, `report-beta`, `finance-beta` and `pricer-beta` are removed as their code moves into permanent modules.
- 2015-10-02: `modules/math` and `modules/report` are added; `strata-math` alone touched 576 files.
- 2015-10-30: `modules/loader` is added.
- 2015-11-12: `strata-finance` is renamed to `strata-product` (665 files) and `modules/calc` is added.
- 2016-05-26: `modules/data` is added; on 2016-05-31 `ReferenceData` moves (546 files).
- 2016-06-07: `modules/measure` is added, replacing the older `modules/function`.
- 2016-06: a run of package and class renames (measure packages, calibration package, `TradePortfolio` to `TradeList`), likely a final API clean-up before 1.0.

### Strata 1.x (July 2016 to August 2018)

- 2016-07-15: `v1.0.0` is tagged after the `v0.7.0` to `v0.16.0` pre-releases.
- 2016-10 to 2016-11: bond API (2016-10-04), ISDA standard model for CDS (2016-10-19) and a codebase-wide formatter run (2016-11-23).
- 2017-03-02: "Reformat license" touches 2,679 files, the largest commit in the history.
- 2017-09-21: move to Joda-Beans v2 (522 files), released as `v1.4.0` the same day.
- 2018-02-14: warnings fixed "ready for Java 9".
- 2018-06: rates-specific classes are renamed and configuration resources move, with rename information added for users, in preparation for 2.0.

### Strata 2.x growth (August 2018 to May 2022)

Strata 2.0 kept Java 8 as the minimum; the Maven enforcer rule `[1.8,)` is present from `v0.7.0` through `v2.12.0`, so Java 8 was the baseline for the whole 1.x and 2.x line rather than a 2.x change.

- 2018-08-21: `v2.0.0` is tagged.
- 2018-10-18: the JavaFX example GUI moves to OpenJFX on Java 11.
- 2018-11-05: the first dependabot pull requests are merged.
- 2019-01-24: CircleCI configuration is added; on 2019-07-12 the build moves fully from Travis to CircleCI and `.travis.yml` is deleted.
- 2019-07 to 2019-09: tests move from TestNG to JUnit 5, module by module, finishing with pricer, measure, market and loader on 2019-09-03.
- 2019-10-23: the `EUR-ESTER` index is renamed to `EUR-ESTR`.
- 2019-11-28: auto-merge is replaced with Mergify.
- 2021-04-28: `.github/dependabot.yml` is added.
- 2022-05-16: the build is made to work on Java 17.
- 2022-05-18: `v2.12.0` is tagged; every later release stays on the 2.12 line.

### Maintenance and dependency updates (May 2022 to September 2026)

Commit volume drops to between 50 and 175 a year, much of it dependency bumps and the `[maven-release-plugin]` commits for frequent `v2.12.x` releases.

- 2022-05-13: `Money` and `BigMoney` are converted to use `Decimal`.
- 2024-01-29: the build is made to work on Java 21.
- 2024-08-12: the last GitHub workflow (`ogbot.yml`) is deleted, leaving CircleCI as the only CI.
- 2026-09-29: "Bond irregular dates" (#2694), one of the last feature changes on `main`.
- 2026-10-05: `v2.12.77` release preparation commit.

### Agent readiness on the fork (October 2026)

- 2026-10-12: an environment template and agent setup documentation are added.
- 2026-10-13: agent readiness quality tooling (Spotless, PMD baseline and related checks) is added.
- 2026-10-21: an agent readiness report snapshot is committed (`fa85830`), which is also the point where `auto_model_migration` branches from `main`.

### Scala migration of collect and basics (23 October 2026)

All 26 branch commits carry the date 23 October 2026. Details are in [Scala migration](background/scala-migration.md).

- `2042012`: Scala 3.9.0 build support and pilot conversions in `collect`.
- `bfbda0c`: Java 17 becomes the minimum and Java 8 and 11 CI builds are dropped, ending the Java 8 baseline that had held since 2015.
- `628997a` to `2df86f7`: package-by-package conversion of `collect` (functions, arrays, `ArgChecker`/`Guavate`, tuples, results, time series, byte sources, CSV/INI/XML) and then `basics` (root types, currency, holidays, indices, schedules, date conventions).
- `987f24b`: bytecode restorers for `@SafeVarargs` and bridge `IOException` metadata.
- `703ffe9` to `7137a16`: delivery review fixes, including `FailureItem` stack trace handling.
- `7ca24cb`: keep the Java stack depth limit and drop mixin forwarders.
- `d9c689c` and `7caa43e` by factory-jim-frink: use the default stack depth so the forbiddenapis check passes, and let the build helpers compile on JDK 21.

## Longest-standing features

First-introduced dates come from `git log main --follow` on the Java file as it existed on `main` before the Scala migration; change counts are commits on `main` touching that file, including renames.

| Feature | File on `main` | First introduced | Approximate changes |
|---|---|---|---|
| Argument validation (`ArgChecker`) | `modules/collect/src/main/java/com/opengamma/strata/collect/ArgChecker.java` | 2014-06-18 | 38 |
| Guava helpers (`Guavate`) | `modules/collect/src/main/java/com/opengamma/strata/collect/Guavate.java` | 2014-06-24 | 66 |
| `Result` and failures | `modules/collect/src/main/java/com/opengamma/strata/collect/result/Result.java` | 2014-06-26 | 54 |
| Date time series | `modules/collect/src/main/java/com/opengamma/strata/collect/timeseries/LocalDateDoubleTimeSeries.java` | 2014-07-01 | 28 |
| `Currency` | `modules/basics/src/main/java/com/opengamma/strata/basics/currency/Currency.java` | 2014-07-14 | 27 |
| Holiday calendars | `modules/basics/src/main/java/com/opengamma/strata/basics/date/HolidayCalendar.java` | 2014-07-21 | 33 |
| Day counts | `modules/basics/src/main/java/com/opengamma/strata/basics/date/DayCount.java` | 2014-07-21 | 29 |
| Tenors | `modules/basics/src/main/java/com/opengamma/strata/basics/date/Tenor.java` | 2014-07-21 | 30 |
| Periodic schedules | `modules/basics/src/main/java/com/opengamma/strata/basics/schedule/PeriodicSchedule.java` | 2014-09-13 | 63 |
| Swap product | `modules/product/src/main/java/com/opengamma/strata/product/swap/Swap.java` | 2014-10-23 | 51 |
| Discounting swap pricer | `modules/pricer/src/main/java/com/opengamma/strata/pricer/swap/DiscountingSwapProductPricer.java` | 2015-04-17 | 43 |
| Interpolated nodal curve | `modules/market/src/main/java/com/opengamma/strata/market/curve/InterpolatedNodalCurve.java` | 2015-05-28 | 37 |
| `DoubleArray` | `modules/collect/src/main/java/com/opengamma/strata/collect/array/DoubleArray.java` | 2015-10-02 | 24 |
| `CalculationRunner` | `modules/calc/src/main/java/com/opengamma/strata/calc/CalculationRunner.java` | 2016-01-04 | 13 |

On this branch the `collect` and `basics` entries now live as `.scala` files under `modules/<module>/src/main/scala/com/opengamma/strata/...`.

## Deprecated features

Deprecated items still present at HEAD. "Deprecated" is the first commit on `main` that added the annotation to the file.

| Item | Location | Introduced | Deprecated | Replacement |
|---|---|---|---|---|
| `EUR_ESTER` overnight index | `modules/basics/src/main/scala/com/opengamma/strata/basics/index/OvernightIndices.scala` | 2019-03-19 (#1923) | 2019-10-23 (#2101) | `EUR_ESTR` |
| `CHF_TOIS` overnight index | same file | 2015-03-30 or earlier | 2019-01-28 (#1879) | none; noted as not published since 2017-12-29 |
| `FunctionUtils` power helpers | `modules/math/src/main/java/com/opengamma/strata/math/impl/FunctionUtils.java` | 2015-10-02 | 2019-07-15 (#2027) | `MathUtils.pow2` and `MathUtils.pow3` |
| Lightweight position resolver method | `modules/loader/src/main/java/com/opengamma/strata/loader/csv/PositionCsvLoader.java` | 2017-09-15 | 2019-05-30 (#1986) | `LightweightPositionCsvInfoResolver` |
| `ArrayByteSource` hash methods | `modules/collect/src/main/scala/com/opengamma/strata/collect/io/ArrayByteSource.scala` | 2017-06-26 | 2019-12-19 (#2123) | `toHash(HashFunction)` |
| Smile term structure method | `modules/pricer/src/main/java/com/opengamma/strata/pricer/fxopt/InterpolatedStrikeSmileDeltaTermStructure.java` | 2016-07-13 | 2020-06-16 (#2180) | metadata that carries the volatility tenor |
| Ibor future conventions | `modules/product/src/main/java/com/opengamma/strata/product/index/type/IborFutureConvention.java` and related classes | 2015-10-21 | 2020-08-27 (#2209) | `IborFutureContractSpec`, `StandardIborFutureContractSpecs` |
| CSV column name constants | `modules/loader/src/main/java/com/opengamma/strata/loader/csv/CsvLoaderUtils.java` | 2017-10-26 | 2021-02-02 (#2286) | `CsvLoaderColumns` |
| `GBP_FIXED_ZC_GB_HCIP` convention | `modules/product/src/main/java/com/opengamma/strata/product/swap/type/FixedInflationSwapConventions.java` | 2016-04-06 | 2022-01-04 (#2402) | `GBP_FIXED_ZC_GB_HICP` |
| `Money` and `BigMoney` amount accessors | `modules/basics/src/main/scala/com/opengamma/strata/basics/currency/Money.scala`, `BigMoney.scala` | 2017-03-15 (`Money`) | 2022-05-13 (#2461) | `getValue()` and `map(UnaryOperator)` |

The Ibor future and CSV deprecations look like cases where a more general type replaced an older one and the old API was kept for compatibility. `modules/product/src/main/java/com/opengamma/strata/product/Attributes.java` also carries a note to "Remove default in Strata v3", so some deprecated APIs appear to be held back for a future major version.

## Major rewrites

Largest commits on `main` by files changed:

| Date | Commit | Files | Change |
|---|---|---|---|
| 2017-03-02 | `fe1f7ea` | 2,679 | Reformat license headers |
| 2015-11-12 | `0cc67dd` | 665 | Rename `strata-finance` to `strata-product` |
| 2015-10-02 | `cf0c41b` | 576 | Add `strata-math` |
| 2016-05-31 | `265ec29` | 546 | Move `ReferenceData` (#999) |
| 2017-09-21 | `8ebcb1c` | 522 | Joda-Beans v2 (#1564) |
| 2020-03-02 | `bbc75b9` | 436 | Joda-Beans 2.7.1 to 2.8.0 (regenerated bean code) |
| 2015-03-30 | `bad614b` | 393 | "Project Strata - Collect module" |
| 2016-06-02 | `2693dce` | 366 | Group product-specific market data (#1020) |
| 2019-07-30 | `b6ba949` | 306 | Deprecate `assertThrows` in favour of AssertJ (#2045) |
| 2019-08-30 | `730d9bf` | 280 | TestNG to JUnit 5 in `strata-product` (#2066) |

Other structural changes:

- Module consolidation (2015 to 2016): removed directories include `modules/pricer-impl` (2015-04), `modules/curve` (2015-05), the `-beta` modules (2015-09 to 2015-10), `modules/source` and `modules/engine` (2015-11) and `modules/function` (2016-06).
- Array classes renamed and moved (2015-10-16, about 270 files each).
- Rates class renames for 2.0 (2018-06-21 and 2018-07-11).
- CI: Travis CI (2015-07-15 to 2019-07-12), CircleCI (from 2019-01-24), GitHub workflows for a bot and auto-merge (2020-11-03 to 2024-08-12), Mergify (from 2019-11-28) and dependabot (first PRs 2018-11-05, config file 2021-04-28).
- Tests: TestNG to JUnit 5 between 2019-07-29 and 2019-09-03.
- Java: Java 8 minimum from the first tag; builds made to work on Java 17 (2022-05-16) and Java 21 (2024-01-29); Java 17 required on this branch (2026-10-23).
- Language: 233 production files in `collect` and `basics` converted from Java to Scala 3.9.0 on 2026-10-23.

## Growth trajectory

Modules by date of first commit under their current directory (the `modules/` layout dates from 2014-12-23, so earlier code in `collect` and `basics` shows that date):

| Date | Module |
|---|---|
| 2014-12-23 | `collect`, `basics`, `examples` |
| 2015-01-06 | `pricer` |
| 2015-05-18 | `market` |
| 2015-10-02 | `math`, `report` |
| 2015-10-30 | `loader` |
| 2015-11-12 | `product`, `calc` |
| 2016-05-26 | `data` |
| 2016-06-07 | `measure` |

Source files at sampled tags (`.java` and `.scala` files across the repository; "main" counts files under any `src/main/` directory, including resources):

| Tag | Date | Java files | Scala files | Files under `src/main` |
|---|---|---|---|---|
| `v0.7.0` | 2015-06-25 | 1,148 | 0 | 806 |
| `v0.10.0` | 2016-02-29 | 2,393 | 0 | 1,540 |
| `v1.0.0` | 2016-07-15 | 2,566 | 0 | 1,618 |
| `v1.4.0` | 2017-09-21 | 2,775 | 0 | 1,774 |
| `v2.0.0` | 2018-08-21 | 2,921 | 0 | 1,869 |
| `v2.6.9` | 2019-12-19 | 3,023 | 0 | 1,929 |
| `v2.10.20` | 2021-09-29 | 3,087 | 0 | 1,981 |
| `v2.12.16` | 2022-12-09 | 3,187 | 0 | 2,046 |
| `v2.12.61` | 2025-08-13 | 3,258 | 0 | 2,090 |
| `v2.12.76` | 2026-09-30 | 3,267 | 0 | 2,095 |
| HEAD (`7caa43e`) | 2026-10-23 | 3,039 | 234 | 2,095 |

The code base doubled between mid-2015 and early 2016 and has grown slowly since. The HEAD row shows the Scala migration replacing Java files one-for-one without changing the number of files under `src/main`.

Commits and distinct human authors per calendar year on `main` (authors whose name contains "bot" are excluded; totals only):

| Year | Commits | Authors |
|---|---|---|
| 2014 | 353 | 3 |
| 2015 | 1,906 | 11 |
| 2016 | 1,070 | 11 |
| 2017 | 224 | 9 |
| 2018 | 267 | 17 |
| 2019 | 289 | 16 |
| 2020 | 165 | 14 |
| 2021 | 176 | 22 |
| 2022 | 174 | 13 |
| 2023 | 85 | 20 |
| 2024 | 61 | 13 |
| 2025 | 69 | 11 |
| 2026 | 50 | 12 |

The author count rose after 1.0 even as commit volume fell, which suggests a shift from a small core team to a wider set of occasional contributors. The 2026 figures cover `main` only and exclude the 26 Scala migration commits on this branch.

## Related pages

- [Fun facts](fun-facts.md)
- [By the numbers](by-the-numbers.md)
- [Scala migration](background/scala-migration.md)
- [Modules](modules/index.md)
