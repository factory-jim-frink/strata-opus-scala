# Features

Feature pages follow one capability across several modules, from the API a user calls down to the classes that do the work. For the internals of a single module, see the [module pages](../modules/index.md).

| Page | What it covers |
| --- | --- |
| [Pricing a trade](pricing-a-trade.md) | The end-to-end path from a `Trade` through `CalculationRunner`, calculation functions and pricers to a `Results` grid and a trade report, plus direct pricer use without the engine |
| [Curve calibration](curve-calibration.md) | Curve group definitions, curve nodes, CSV curve configuration, `RatesCurveCalibrator`, and the market data functions that calibrate curves inside the engine |
| [Sensitivities](sensitivities.md) | Point sensitivities from pricers, parameter sensitivities on curves, PV01 measures, market quote sensitivities, gamma, and sensitivity CSV files |
| [Scenarios](scenarios.md) | `ScenarioDefinition`, perturbation mappings and filters, curve and quote shifts, scenario market data, and `ScenarioArray` results |
| [Scala and Java interoperability](scala-java-interop.md) | How the Scala `collect` and `basics` modules keep a binary-compatible Java API |
| [Joda-Beans and serialization](joda-beans-and-serialization.md) | Bean metadata, XML/JSON/binary serialization, Joda-Convert string conversion and Java serialization |
| [Reference data and configuration](reference-data-and-configuration.md) | `ReferenceData`, `ResourceConfig` directories (base, library, application), INI files and the `com.opengamma.strata.config.directories` property |

The first four pages describe the analytics path and build on each other: pricing uses calibrated curves, sensitivities come from the same pricers, and scenarios rerun the pricing with perturbed market data. The last three describe infrastructure used by every module.
