# Configuration

Strata is a library and has very little runtime configuration: a set of INI and CSV files on the classpath that define named values such as currencies, holiday calendars, indices and conventions, plus one JVM system property that changes which configuration directories are searched. The build is controlled by Maven flags, and the release process reads a handful of environment variables.

For how `ResourceConfig` finds, orders and merges these files, see [Reference data and configuration](../features/reference-data-and-configuration.md). This page lists what exists.

## Classpath configuration files

All files live under `src/main/resources/META-INF/com/opengamma/strata/config/<directory>/` in their module. Three file shapes appear:

- **ExtendedEnum provider files** have a `[providers]` section that maps a class name to `constants` (public static final fields), `lookup` (a `NamedLookup` with a no-args constructor) or `instance` (a static `INSTANCE` field). Many also have an `[alternates]` section of alternate names. They are read by `ExtendedEnum` in `modules/collect/src/main/scala/com/opengamma/strata/collect/named/ExtendedEnum.scala`.
- **CombinedExtendedEnum files** have a `[types]` section listing the enum types to merge into one lookup.
- **Data files** (INI sections or CSV rows) hold the values themselves and are read by a lookup class in the same module.

### basics (`modules/basics/src/main/resources/META-INF/com/opengamma/strata/config/base/`)

| File | Kind | Purpose |
| --- | --- | --- |
| `BusinessDayConvention.ini` | Providers | Registers `StandardBusinessDayConventions` constants. |
| `Currency.ini` | Data | One section per ISO-4217 code with `minorUnitDigits`, `triangulationCurrency` and optional `historic`. |
| `CurrencyData.ini` | Data | `[marketConventionPriority]` ordering used to choose the base currency of a pair not listed in `CurrencyPair.ini`. |
| `CurrencyPair.ini` | Data | Market-convention currency pairs and their `rateDigits`. |
| `DateSequence.ini` | Providers | Registers `StandardDateSequences` constants. |
| `DayCount.ini` | Providers | Registers `StandardDayCounts` constants and the `Business252DayCount` instance. |
| `FloatingRateIndex.ini` | Combined | Combines `IborIndex`, `OvernightIndex` and `PriceIndex`. |
| `FloatingRateName.ini` | Providers | Registers `FloatingRateNameIniLookup`. |
| `FloatingRateNameData.ini` | Data | Maps FpML and market floating rate names (for example `CHF-LIBOR-BBA`) to Strata index names. |
| `FxIndex.ini` | Providers | Registers `FxIndexCsvLookup`. |
| `FxIndexData.csv` | Data | FX index definitions: currencies, fixing calendar, maturity days and calendar. |
| `HolidayCalendar.ini` | Providers | Registers `HolidayCalendars` constants, `HolidayCalendarIniLookup` and the global holiday calendar lookup. |
| `HolidayCalendarData.ini` | Data | Holiday dates and weekend rules for each calendar, such as `GBLO`. |
| `HolidayCalendarDefaultData.ini` | Data | `[defaultByCurrency]` default holiday calendar per currency. |
| `IborIndex.ini` | Providers | Registers `IborIndexCsvLookup`. |
| `IborIndexData.csv` | Data | Ibor index definitions: currency, day count, fixing and offset calendars, tenor, fixing time and zone. |
| `Index.ini` | Combined | Combines `IborIndex`, `OvernightIndex`, `PriceIndex` and `FxIndex`. |
| `OvernightIndex.ini` | Providers | Registers `OvernightIndexCsvLookup`. |
| `OvernightIndexData.csv` | Data | Overnight index definitions (SONIA, SOFR, ESTR and others): day count, calendar, publication and effective offsets. |
| `PeriodAdditionConvention.ini` | Providers | Registers `StandardPeriodAdditionConventions` constants. |
| `PriceIndex.ini` | Providers | Registers `PriceIndexCsvLookup`. |
| `PriceIndexData.csv` | Data | Inflation index definitions: currency, region, active flag, publication frequency. |
| `RateIndex.ini` | Combined | Combines `IborIndex` and `OvernightIndex`. |
| `RollConvention.ini` | Providers | Registers `StandardRollConventions` constants and the `DayRollConventions` lookup. |

The same directory's parent holds `modules/basics/src/main/resources/META-INF/com/opengamma/strata/config/README.md`, summarised below.

### product (`modules/product/src/main/resources/META-INF/com/opengamma/strata/config/`)

| File | Kind | Purpose |
| --- | --- | --- |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/CdsConvention.ini` | Providers | Registers `StandardCdsConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/FixedFloatSwapConvention.ini` | Combined | Combines `FixedIborSwapConvention` and `FixedOvernightSwapConvention`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/FixedIborSwapConvention.ini` | Providers | Registers `StandardFixedIborSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/FixedInflationSwapConvention.ini` | Providers | Registers `StandardFixedInflationSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/FixedOvernightSwapConvention.ini` | Providers | Registers `StandardFixedOvernightSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/FraConvention.ini` | Providers | Registers the `FraConventionLookup` instance. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/FxSwapConvention.ini` | Providers | Registers `StandardFxSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/IborFixingDepositConvention.ini` | Providers | Registers the `IborFixingDepositConventionLookup` instance. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/IborFutureContractSpec.ini` | Providers | Registers `StandardIborFutureContractSpecs`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/IborFutureConvention.ini` | Providers | Registers `StandardIborFutureConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/IborIborSwapConvention.ini` | Providers | Registers `StandardIborIborSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/IsdaYieldCurveConvention.ini` | Providers | Registers `StandardIsdaYieldCurveConventions` (credit curves). |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/OvernightFutureContractSpec.ini` | Providers | Registers `StandardOvernightFutureContractSpecs`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/OvernightIborSwapConvention.ini` | Providers | Registers `StandardOvernightIborSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/OvernightOvernightSwapConvention.ini` | Providers | Registers `StandardOvernightOvernightSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/SwapIndex.ini` | Providers | Registers the `SwapIndexCsvLookup` instance. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/SwapIndexData.csv` | Data | Swap index definitions: convention, tenor, fixing time and zone. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/TermDepositConvention.ini` | Providers | Registers `StandardTermDepositConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/ThreeLegBasisSwapConvention.ini` | Providers | Registers `StandardThreeLegBasisSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/XCcyIborIborSwapConvention.ini` | Providers | Registers `StandardXCcyIborIborSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base/XCcyOvernightOvernightSwapConvention.ini` | Providers | Registers `StandardXCcyOvernightOvernightSwapConventions`. |
| `modules/product/src/main/resources/META-INF/com/opengamma/strata/config/base1/Index.ini` | Combined | Adds `SwapIndex` to the `Index` lookup. It sits in `base1` so it merges over the `basics` copy of `Index.ini`. |

### market (`modules/market/src/main/resources/META-INF/com/opengamma/strata/config/base/`)

| File | Kind | Purpose |
| --- | --- | --- |
| `CurveExtrapolator.ini` | Providers | Registers `StandardCurveExtrapolators`. |
| `CurveInterpolator.ini` | Providers | Registers `StandardCurveInterpolators`. |

### loader (`modules/loader/src/main/resources/META-INF/com/opengamma/strata/config/base/`)

| File | Kind | Purpose |
| --- | --- | --- |
| `FpmlParserPlugin.ini` | Providers | Registers the FpML parser plugins in `com.opengamma.strata.loader.impl.fpml`. |
| `PositionCsvParserPlugin.ini` | Providers | Registers position CSV plugins (ETD futures, ETD options, securities, and a "smart" plugin). |
| `TradeCsvParserPlugin.ini` | Providers | Registers trade CSV parser plugins, one per product type. |
| `TradeCsvWriterPlugin.ini` | Providers | Registers trade CSV writer plugins. |

### calc and measure

| File | Kind | Purpose |
| --- | --- | --- |
| `modules/calc/src/main/resources/META-INF/com/opengamma/strata/config/base/Measure.ini` | Providers | Declares the `Measure` enum with an empty `[providers]` section and the alternate-name section. |
| `modules/measure/src/main/resources/META-INF/com/opengamma/strata/config/base2/Measure.ini` | Providers | Registers `StandardMeasures` and `StandardCreditMeasures`. It sits in `base2` so it merges over the `calc` file. |

Test-only configuration (sample and invalid INI files, chained files in `base`, `base1` and `application`) lives under `modules/collect/src/test/resources/META-INF/com/opengamma/strata/config/` and `modules/basics/src/test/resources/META-INF/com/opengamma/strata/config/base/`. It is not packaged.

## The configuration README

`modules/basics/src/main/resources/META-INF/com/opengamma/strata/config/README.md` states:

- The standard location is `META-INF/com/opengamma/strata/config`.
- Three subdirectories are recognised by default: `base` (supplied by Strata), `library` (supplied by libraries built on Strata) and `application` (supplied by applications).
- When a resource such as `Foo.ini` is requested, every copy found in those directories is loaded and merged, and sections and properties from `application` take precedence.
- The system property `com.opengamma.strata.config.directories` changes the set of directories, for example to `base,library,application,user`.

The implementation in `modules/collect/src/main/scala/com/opengamma/strata/collect/io/ResourceConfig.scala` also searches numbered variants (`base1`..`base9`, `library1`..`library9`, `application1`..`application9`), which is why `product` and `measure` ship files in `base1` and `base2`.

## JVM system properties

A search for `System.getProperty`, `Boolean.getBoolean`, `Integer.getInteger`, `Long.getLong` and `System.getenv` across all main sources found one property that Strata reads at runtime.

| Property | Read in | Effect |
| --- | --- | --- |
| `com.opengamma.strata.config.directories` | `modules/collect/src/main/scala/com/opengamma/strata/collect/io/ResourceConfig.scala` (line 134) | Comma-separated list of configuration directories to search, lowest priority first. |

The build-only helpers `modules/collect/src/build/test/RestoreSafeVarargsControls.java` and `modules/collect/src/build/test/RestoreBridgeExceptionsControls.java` read the standard `java.home` and `java.class.path` properties to start a child JVM. They are not part of the library.

## Maven flags and profiles

Profiles are activated by property flags on the command line. The table lists every flag-activated profile in the repository POMs, plus the one inherited from the `com.opengamma:corporate-parent:2.9.2` parent POM.

| Flag | Profile and POM | What it does |
| --- | --- | --- |
| `-Dstrict` | `strict` in `modules/pom.xml` | Runs `maven-enforcer-plugin` (banned dependencies such as `commons-logging` and `org.json:json`, duplicate classes, Java 8+, explicit plugin versions) and `joda-beans-maven-plugin:validate` with `stopOnError`. CI runs `mvn install -T 4 -DskipTests -Dstrict`. |
| `-Ddist` | `build-dist-zip` in `pom.xml` | Builds `target/strata-dist-<version>.zip` from `src/assembly/dist.xml`. |
| `-Ddist` | `build-dist-javadoc-and-jars` in `modules/pom.xml` | Attaches source and javadoc jars and builds the aggregate Javadoc. |
| `-Ddist` | `scaladoc-jar` in `modules/collect/pom.xml` and `modules/basics/pom.xml` | Publishes ScalaDoc as the module's javadoc jar, since `maven.javadoc.skip` is set for these two modules. |
| `-Ddist` | `build-report-tool` in `modules/report/pom.xml` | Copies report dependencies for the distribution zip. |
| `-Ddist` | `build-report-tool` in `examples/pom.xml` | Builds a source jar, a shaded `jar-with-dependencies` with `ReportRunnerTool` as main class, and `strata-report-tool-<version>.zip`. |
| `-Dghdeploy` | `ghdeploy` in `pom.xml` | Configures `github-release-maven-plugin` 1.0.7 to publish the dist and report-tool zips to a GitHub release tagged `v<version>`. |
| `-Doss.repo` | `release-artifacts` in the corporate parent POM | Requires `LICENSE.txt`, `NOTICE.txt` and release environment variables, signs artifacts with `maven-gpg-plugin`, and publishes with `central-publishing-maven-plugin`. |
| `-Dcompat` | `compat` in `modules/pom.xml` | Runs `japicmp-maven-plugin` against version 2.0.0 and concatenates the per-module diffs. |

Profiles activated by JDK version rather than a flag:

| Profile | POM | Activation | Effect |
| --- | --- | --- | --- |
| `forcejavaversion` | `modules/pom.xml` | JDK 9 or later | Sets `maven.compiler.release` to 8. |
| `javafx-missing` | `examples/pom.xml` | JDK 8 to 10 without `jfxrt.jar` | Excludes `com/opengamma/strata/examples/gui/*.java`. |
| `openjfx-dep` | `examples/pom.xml` | JDK 11 or later | Adds `org.openjfx:javafx-controls` 11.0.2. |

Other flags used in this repository:

| Flag | Where used | Effect |
| --- | --- | --- |
| `-Dspotless.check.skip=true` | `AGENTS.md` | Skips the Spotless check, which uses `ratchetFrom main` and needs a normal git checkout. Use it in linked worktrees or copies without `.git`. |
| `-Dcheckstyle.skip=true` | `.circleci/config.yml` | Skips Checkstyle in the test and release steps. |
| `-DskipTests` | `.circleci/config.yml` | Skips tests. In `modules/collect/pom.xml` it also skips the bytecode restorer control checks. |
| `-DtrimStackTrace=false` | `.circleci/config.yml` (written to `.mvn/maven.config`) | Keeps full stack traces in Surefire output. |
| `-Dtest=<Class> -Dsurefire.failIfNoSpecifiedTests=false` | `AGENTS.md` | Runs a single test class together with `-pl <module> -am`. |

## Environment variables

Strata does not read a `.env` file. `.env.example` documents the variables that the build, the tests and the release process consume.

| Variable | Group | Purpose | Consumed by |
| --- | --- | --- | --- |
| `JAVA_TOOL_OPTIONS` | Build and test | JVM options for every JVM, including forked Surefire JVMs. CI sets `-XX:MaxRAM=3572m` because the JVM does not see the container memory limit. Also a convenient way to pass `-Dcom.opengamma.strata.config.directories`. | `.circleci/config.yml` executors |
| `MAVEN_OPTS` | Build and test | JVM options for Maven itself. CI sets `-Xmx1g`. | `.circleci/config.yml` executors |
| `OSS_SONATYPE_USER`, `OSS_SONATYPE_PASSWORD` | Release (secret) | Credentials for the `central` server used by `mvn deploy -Doss.repo`. | `.circleci/maven-settings.xml` |
| `ARTIFACTORY_USER`, `ARTIFACTORY_PASSWORD` | Release (secret) | Credentials for the `opengamma-repo` server. | `.circleci/maven-settings.xml` |
| `GITHUB_TOKEN` | Release (secret) | Signing key passphrase and credential for `github-release-maven-plugin`. | `.circleci/maven-settings.xml` |
| `SIGNING_PASSWORD` | Release (secret) | Decrypts `.circleci/codesigning.asc.gpg`. | `setup_gpg` in `.circleci/config.yml` |
| `CIRCLE_TAG`, `CIRCLE_JOB`, `CACHE_VERSION` | CircleCI only | Release tag, job name and Maven cache prefix. Do not set locally. | `.circleci/config.yml` |

The `release-artifacts` profile in the corporate parent POM also checks for `CI`, `GPG_KEY_NAME` and `GPG_PASSPHRASE` with `requireEnvironmentVariable`. These three are not listed in `.env.example`.

## Related pages

- [Reference data and configuration](../features/reference-data-and-configuration.md)
- [Dependencies](dependencies.md)
- [Getting started](../overview/getting-started.md)
- [basics](../modules/basics.md) and [collect](../modules/collect.md)
