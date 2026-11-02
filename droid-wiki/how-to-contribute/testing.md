# Testing

Strata has about 1,300 Java test files spread across the eleven library modules and `examples`, all written with JUnit 5 and AssertJ. This page covers the test stack and Surefire setup, where tests and test data live, how to run a subset, and how the Scala modules `collect` and `basics` are tested.

## Test stack

Versions are set in the `<properties>` of `modules/pom.xml`:

| Library | Version | Notes |
| --- | --- | --- |
| JUnit 5 (`junit-bom`) | 5.11.4 | Modules depend on `junit-jupiter-api`, `junit-jupiter-params` and `junit-jupiter-engine` |
| AssertJ (`assertj-core`) | 3.27.2 | Default assertion library |
| Mockito (`mockito-core`) | 4.9.0 | Used sparingly, in about 37 test files |
| `slf4j-nop` | 1.7.36 | Test scope in `modules/math/pom.xml`, silences SLF4J output |

The `strict` profile's enforcer rule bans the old `junit:*` artifacts outside `test` and `provided` scope.

Parameterized tests (`@ParameterizedTest` with `@MethodSource`) appear in about 120 test files and are the usual way to express case tables such as holiday dates or day count fractions.

## Surefire configuration

`modules/pom.xml` configures `maven-surefire-plugin` (version managed by `corporate-parent` 2.9.2) for every library module:

- `forkCount` 2 with `reuseForks` true, so each module's tests run in two forked JVMs and classes that use static state do not share a process with the build.
- `rerunFailingTestsCount` 1, so a failing test is retried once and a pass on retry is recorded as flaky in the XML reports rather than failing the build.
- `usedefaultlisteners` false, to drop the slow HTML reporter.

`examples/pom.xml` uses the same `forkCount` and `reuseForks` but no rerun.

Forked JVMs inherit `JAVA_TOOL_OPTIONS`. CircleCI sets it to `-XX:MaxRAM=3572m` and runs tests with `-T 1` (one module at a time) so that the two forks fit in the container's memory. See [Debugging](debugging.md) if you see out-of-memory errors.

## Test layout

Each module keeps tests in `src/test/java`, in the same package as the class under test, one test class per production class (`CurrencyAmount` has `CurrencyAmountTest`). Test resources live in `src/test/resources`.

| Module | Test classes (approx.) | Test resources |
| --- | --- | --- |
| `modules/collect` | 100 | 27 files: ZIP and text fixtures for `io`, sample and invalid INI files for `ExtendedEnum` and `ResourceConfig` under `META-INF/com/opengamma/strata/config/{base,base1,application}` |
| `modules/basics` | 76 | 8 files: holiday calendar INI fixtures and an old-format `ImmutableHolidayCalendar` JSON |
| `modules/data` | 31 | none |
| `modules/product` | 312 | 2 |
| `modules/math` | 159 | none |
| `modules/market` | 154 | none |
| `modules/loader` | 18 | 130 files: CSV and FpML samples for the loaders |
| `modules/pricer` | 322 | 26 |
| `modules/calc` | 41 | none |
| `modules/measure` | 84 | none |
| `modules/report` | 14 | 3 |
| `examples` | 6 | 21 files: test market data sets, a golden-copy report and a report template |

The counts are file counts from `src/test/java`, which includes a few helper classes.

Test resources under `META-INF/com/opengamma/strata/config/` are picked up by `ResourceConfig` exactly like production configuration, so the `collect` and `basics` tests can add INI files to the `base` and `application` directories to test loading order and invalid input. See [Reference data and configuration](../features/reference-data-and-configuration.md).

### Sharing test code between modules

`modules/pom.xml` configures `maven-jar-plugin` with a `test-jar` goal and declares a `test-jar` dependency for most modules in `dependencyManagement`. Downstream modules use these to reuse fixtures and helpers, most importantly `TestHelper` from `strata-collect`. If a test in another module cannot see `TestHelper`, check that the module's POM declares the `strata-collect` `test-jar` dependency with `test` scope.

## Test helpers

`modules/collect/src/test/java/com/opengamma/strata/collect/TestHelper.java` is the shared helper class. It covers bean coverage (`coverImmutableBean`, `coverBeanEquals`), round trips (`assertSerialization`, `assertJodaConvert`, `assertJodaSerialization`), utility class checks (`assertUtilityClass`) and fixtures (`date`, `list`). The full table is on [Patterns and conventions](patterns-and-conventions.md#tests).

Other helpers in the same directory:

- `CollectProjectAssertions` extends AssertJ `Assertions` with `assertThat(Result<?>)`, backed by `ResultAssert`, for fluent checks on `Result` values.
- `AssertRunnable`, `SampleType` and `SampleValidatedType` are small fixtures used by the `collect` tests themselves.

A typical bean test calls `coverImmutableBean(sut())` and `coverBeanEquals(sut(), sut2())`, then `assertSerialization(sut())`.

## Example and regression tests

`examples/src/test/java/com/opengamma/strata/examples/` contains end-to-end tests:

- `ExamplesTest` runs each example (DSF, FRA, FX, generic security, STIR future, swap, term deposit and others) both through its `main` method and through `ReportRunnerTool`, captures standard output and checks that it is a valid ASCII report table.
- `FxPricingIntegrationTest` runs the FX pricing example.
- `examples/src/test/java/com/opengamma/strata/examples/regression/SwapReportRegressionTest.java` produces a swap report with the template `examples/src/test/resources/example-reports/swap-report-regression-test-template.ini` and compares it to `examples/src/test/resources/goldencopy/swap-report.txt`.
- `examples/src/test/java/com/opengamma/strata/examples/marketdata/ExampleMarketDataBuilderTest.java` loads the market data sets under `examples/src/test/resources/test-marketdata-*`, including one directory whose name contains spaces and several deliberately invalid CSV files.

If you change pricing or report formatting in a way that changes numbers or layout, the golden copy must be updated deliberately in the same change.

## Running tests

| Goal | Command |
| --- | --- |
| All tests | `mvn test` |
| All tests the way CI runs them | `mvn install -T 4 -DskipTests -Dstrict` then `mvn test -T 1 -Dcheckstyle.skip=true` |
| One module (after `mvn install -DskipTests`) | `mvn -pl modules/market test` |
| One class | `mvn -pl modules/basics -am -Dtest=CurrencyAmountTest -Dsurefire.failIfNoSpecifiedTests=false test` |
| One method | `mvn -pl modules/basics -am -Dtest=CurrencyAmountTest#test_of_Currency -Dsurefire.failIfNoSpecifiedTests=false test` |
| An example test | `mvn -pl examples -am -Dtest=FxPricingIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` |

Add `-Dspotless.check.skip=true` in a linked worktree. Keep `-am` on the class and method commands so that tests use the current `collect` and `basics` classes from the reactor rather than jars from `~/.m2`. Running one module without `-am` is only safe right after installing the whole reactor.

Reports are written to `target/surefire-reports/` in each module. CI copies all `*.xml` reports into one folder and uploads them, together with the PMD reports, as a `test-results` artifact.

## Coverage

No coverage tool is configured. Neither the root `pom.xml`, `modules/pom.xml`, the module POMs nor `examples/pom.xml` mention JaCoCo or another coverage plugin. The `TestHelper.cover*` methods exist to exercise generated bean code so that it does not show up as untested when coverage is measured in an IDE.

## Tests for the Scala modules

The `collect` and `basics` production code was converted to Scala on this branch, but the tests were left as Java and were not changed: `git diff --stat main...HEAD -- '*/src/test/*'` is empty. Because the tests are Java, they compile against the Java-facing API of the Scala classes and fail if a static method, getter, checked exception or bean property goes missing.

The migration did add build-time checks that live outside `src/test`:

- `modules/collect/src/build/test/RestoreSafeVarargsControls.java` runs `RestoreSafeVarargs` against ASM-generated fixtures (including methods that must not be annotated, such as the Scala `Seq` overloads) and checks the real, already processed `Guavate` class in `target/classes`.
- `modules/collect/src/build/test/RestoreBridgeExceptionsControls.java` does the same for `RestoreBridgeExceptions`, using ASM fixtures plus a real Scala 3 class compiled from `modules/collect/src/build/test-fixtures/ArrayByteSource.scala`, and checks the processed `ArrayByteSource` bridges declare `IOException`.

Both run through `maven-antrun-plugin` in the `test` phase of `modules/collect/pom.xml` and are skipped with `-DskipTests`. See [Tooling](tooling.md#bytecode-restorers).

Some existing Java tests are sensitive to the Scala implementation. `FailureItemTest` in `modules/collect/src/test/java/com/opengamma/strata/collect/result/` checks the recorded stack trace text (for example that it starts with the `FailureItem` message and does not contain `FailureItem.of(` frames), which is why `FailureItem.scala` filters Scala forwarder frames. The `basics` tests use the `TestHelper` bean helpers on the hand-ported Scala beans, which catches mistakes in their meta-bean code.

## Related pages

- [Patterns and conventions](patterns-and-conventions.md)
- [Debugging](debugging.md)
- [Development workflow](development-workflow.md)
- [Scala and Java interoperability](../features/scala-java-interop.md)
