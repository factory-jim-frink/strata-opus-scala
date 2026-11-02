# Getting started

This page covers what you need installed, how to build and test Strata, and how to run an example. The same commands are collected in `AGENTS.md` and the `build-and-test` skill in `.factory/skills/build-and-test/SKILL.md`.

## Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 17 or later | Required because `collect` and `basics` are Scala 3.9.0, which needs JDK 17 to compile and run. CI builds on JDK 17 and 21 (`.circleci/config.yml`). Non-LTS JDKs are not recommended (`README.md`). |
| Maven | 3.5 or later | There is no Maven wrapper. |
| Git | any recent | Spotless uses `ratchetFrom main`, so it needs a normal clone with a `main` branch. |

Strata does not read environment variables at runtime. `.env.example` lists the variables the build and release use; none is needed for a local build.

## Clone and build

```sh
git clone https://github.com/OpenGamma/Strata.git
cd Strata
mvn install
```

`mvn install` compiles, tests and installs every module into `~/.m2`. To build and test without installing:

```sh
mvn test
```

The first build downloads `scala-maven-plugin` 4.10.0, the Scala 3.9.0 compiler and library, Joda-Beans, Guava and the test libraries.

### Useful build variations

| Goal | Command |
| --- | --- |
| Full CI-style build (enforcer and Joda-Beans validation) | `mvn install -Dstrict` |
| One test class in one module | `mvn -pl modules/basics -am -Dtest=CurrencyAmountTest -Dsurefire.failIfNoSpecifiedTests=false test` |
| Only the Scala modules | `mvn -pl modules/collect,modules/basics -am package` |
| ScalaDoc for the Scala modules | `mvn -pl modules/collect,modules/basics net.alchim31.maven:scala-maven-plugin:4.10.0:doc` |
| Release packaging (javadoc/ScalaDoc jars, assembly) | `mvn package -Ddist -DskipTests` |
| PMD baseline gate | `python3 config/pmd/check-pmd-baseline.py` |

In a linked git worktree or a copy without `.git`, add `-Dspotless.check.skip=true`, because Spotless cannot find the `main` branch there.

Build `basics` together with `collect` in one reactor (`-am`). If `~/.m2` holds an older `strata-collect` snapshot, building `basics` on its own resolves that jar instead of the current one.

## Run an example

The FX example prices four trades against bundled sample market data and prints a report. It needs no network access or credentials.

```sh
mvn install
mvn -f examples/pom.xml org.codehaus.mojo:exec-maven-plugin:3.1.0:java \
  -Dexec.mainClass=com.opengamma.strata.examples.FxPricingExample
```

Other entry points in `examples/src/main/java/com/opengamma/strata/examples/`:

- `SwapPricingExample`, `FraPricingExample`, `DsfPricingExample`, `TermDepositPricingExample`, `StirFuturePricingExample`, `GenericSecurityPricingExample`
- `examples/src/main/java/com/opengamma/strata/examples/finance/CurveScenarioExample.java`, `examples/src/main/java/com/opengamma/strata/examples/finance/HistoricalScenarioExample.java`, `examples/src/main/java/com/opengamma/strata/examples/finance/SwapPricingWithCalibrationExample.java` and the `finance/Calibration*Example` classes
- `examples/src/main/java/com/opengamma/strata/examples/report/ReportRunnerTool.java`, a command line tool that prices a portfolio file with a report template
- `examples/src/main/java/com/opengamma/strata/examples/gui/ScheduleGui.java`, a JavaFX schedule viewer (OpenJFX comes from Maven Central)

See [Examples](../modules/examples.md) for details.

## Use Strata from another project

Depend on the highest module you need; Maven pulls in the rest:

```xml
<dependency>
  <groupId>com.opengamma.strata</groupId>
  <artifactId>strata-measure</artifactId>
  <version>2.12.75</version>
</dependency>
```

On this branch `strata-collect` and `strata-basics` also depend on `org.scala-lang:scala3-library_3:3.9.0`, which Maven adds transitively.

## Where to go next

- [Architecture](architecture.md)
- [Development workflow](../how-to-contribute/development-workflow.md)
- [Testing](../how-to-contribute/testing.md)
- [Pricing a trade](../features/pricing-a-trade.md)
