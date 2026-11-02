# Strata overview

Strata is OpenGamma's open source library for pricing financial instruments and measuring market risk. It models trades and products, holds market data such as curves and volatility surfaces, and calculates values like present value, PV01 and sensitivities through a calculation engine that can run many trades and scenarios at once. The code is released under the Apache 2.0 license.

This wiki documents the `auto_model_migration` branch of the `factory-jim-frink/strata` fork. On this branch the two foundation modules, `collect` and `basics`, are written in Scala 3.9.0 while every other module stays Java. Their Java API is unchanged, so Java code that uses Strata does not notice the difference.

## Who uses it

- **Application developers** who embed Strata in risk or trading systems. They depend on the Maven artifacts (for example `com.opengamma.strata:strata-measure`) and call the calculation engine.
- **Quants and analysts** who use the pricers and calibration code directly to build curves and value products.
- **Strata contributors** who extend products, pricers and calculation functions.

`README.md` states that Strata is used in production as the core of OpenGamma SaaS Analytics, and that the API is kept backwards compatible.

## What is in the repository

| Area | Location | What it contains |
| --- | --- | --- |
| Library modules | `modules/` | Eleven Maven modules, from `collect` (utilities) up to `report` (output formatting) |
| Examples | `examples/` | Runnable pricing, calibration and scenario examples, sample market data and portfolios |
| Build quality config | `config/pmd/`, `eclipse/` | PMD baseline gate, Eclipse formatter and IDE settings |
| CI and automation | `.circleci/config.yml`, `.github/` | CircleCI build and release jobs, Dependabot, Mergify, CODEOWNERS |
| Agent guidance | `AGENTS.md`, `.factory/skills/` | Build/test and code-convention skills for coding agents |
| Migration guide | `step_by_step_guide.md` | Untracked guide to the Scala migration of `collect` and `basics` |

## A first look at the API

The FX example in `examples/src/main/java/com/opengamma/strata/examples/FxPricingExample.java` shows the whole library working together in about 30 lines. This is a condensed version of its `calculate` method (in the file, the runner is opened in `main` and passed in):

```java
List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2(), createTrade3(), createTrade4());
List<Column> columns = ImmutableList.of(
    Column.of(Measures.PRESENT_VALUE),
    Column.of(Measures.PV01_CALIBRATED_SUM),
    Column.of(Measures.PV01_CALIBRATED_BUCKETED));

ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

CalculationFunctions functions = StandardComponents.calculationFunctions();
CalculationRules rules = CalculationRules.of(functions, marketDataBuilder.ratesLookup(valuationDate));

try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
  Results results = runner.calculate(rules, trades, columns, marketData, ReferenceData.standard());
}
```

The trades come from `product`, the measures and functions from `measure`, the runner from `calc`, the market data types from `data`, and the pricing itself happens in `pricer`.

## Quick links

- [Architecture](architecture.md): how the modules fit together
- [Getting started](getting-started.md): build, test and run an example
- [Glossary](glossary.md): finance and Strata terms
- [Modules](../modules/index.md): one page per Maven module
- [Pricing a trade](../features/pricing-a-trade.md): the end-to-end calculation flow
- [Scala and Java interoperability](../features/scala-java-interop.md): how the Scala modules keep their Java API
- [Patterns and conventions](../how-to-contribute/patterns-and-conventions.md): immutable beans, static factories, validation
