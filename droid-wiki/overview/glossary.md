# Glossary

Terms used across the Strata code and this wiki. Finance terms are defined the way Strata uses them; class names point to where the concept lives.

## Finance terms

| Term | Meaning in Strata |
| --- | --- |
| Present value (PV) | Value today of a trade's future cash flows, as a `CurrencyAmount` or `MultiCurrencyAmount`. Measure `Measures.PRESENT_VALUE`. |
| PV01 | Change in present value for a one basis point (0.0001) shift. Strata offers calibrated (shift the market quotes and recalibrate) and market-quote variants, summed or bucketed by curve node, for example `Measures.PV01_CALIBRATED_SUM` and `PV01_CALIBRATED_BUCKETED`. |
| Basis point (bp) | One hundredth of a percent, 0.0001. |
| Ibor index | A term interbank rate such as GBP-LIBOR-3M or EUR-EURIBOR-6M; `IborIndex` in `modules/basics/src/main/scala/com/opengamma/strata/basics/index/IborIndex.scala`. |
| Overnight index | A daily rate such as SOFR, SONIA or ESTR; `OvernightIndex` in the same package. |
| Price index | An inflation index such as US-CPI-U; `PriceIndex`. |
| FX index | A published FX fixing; `FxIndex`. |
| Floating rate name | The ISDA-style name of a floating rate family, resolved to a specific index; `FloatingRateName`. |
| FRA | Forward rate agreement; `modules/product/src/main/java/com/opengamma/strata/product/fra/Fra.java`. |
| Swap | Exchange of cash flow legs (fixed, Ibor, overnight, inflation, FX reset); `modules/product/src/main/java/com/opengamma/strata/product/swap`. |
| Swaption | Option to enter a swap; `modules/product/src/main/java/com/opengamma/strata/product/swaption`. |
| Cap/floor | Strip of options on an Ibor or overnight rate; `modules/product/src/main/java/com/opengamma/strata/product/capfloor`. |
| CMS | Constant maturity swap product; `modules/product/src/main/java/com/opengamma/strata/product/cms`. |
| DSF | Deliverable swap future; `modules/product/src/main/java/com/opengamma/strata/product/dsf`. |
| CDS | Credit default swap, single name or index; `modules/product/src/main/java/com/opengamma/strata/product/credit`. |
| NDF | Non-deliverable FX forward; `FxNdf` in `modules/product/src/main/java/com/opengamma/strata/product/fx`. |
| ETD | Exchange traded derivative (futures and options on listed contracts); `modules/product/src/main/java/com/opengamma/strata/product/etd`. |
| Term deposit | Fixed-rate deposit; `modules/product/src/main/java/com/opengamma/strata/product/deposit`. |
| Bullet payment | A single fixed payment; `modules/product/src/main/java/com/opengamma/strata/product/payment`. |
| Curve calibration | Solving for curve parameters so that a set of instruments reprices to their market quotes; `RatesCurveCalibrator` in `modules/pricer/src/main/java/com/opengamma/strata/pricer/curve/RatesCurveCalibrator.java`. |
| Discount curve / forward curve | Curves used to discount cash flows and to project floating rates. |
| SABR, Black, Hull-White | Option pricing models used by the swaption, cap/floor and futures pricers in `pricer`. |
| Scenario | One of several alternative sets of market data, for example shifted curves for stress testing. |

## Date and schedule terms

| Term | Meaning |
| --- | --- |
| Holiday calendar | Set of non-business days for a location, identified by `HolidayCalendarId` (for example `GBLO`, `USNY`); `HolidayCalendar`. |
| Business day convention | Rule to move a date that falls on a holiday, for example `MODIFIED_FOLLOWING`; `BusinessDayConvention`. |
| Day count | Rule to compute the year fraction between two dates, for example `ACT/360`; `DayCount`. |
| Tenor / frequency | A period such as 3M used for maturities (`Tenor`) or payment frequency (`Frequency`). |
| Roll convention | Rule for the day of month that schedule dates fall on; `RollConvention`. |
| Stub | An irregular first or last period in a schedule; `StubConvention`. |
| Periodic schedule | Definition of regular accrual periods between a start and end date; `PeriodicSchedule`. |

## Strata terms

| Term | Meaning |
| --- | --- |
| Product | The economic terms of an instrument, for example a `Fra` or `Swap`. |
| Trade | A product plus trade details (`TradeInfo`: id, counterparty, trade and settlement date, attributes); `Trade`. |
| Position | A quantity of a security, as opposed to a trade with a trade date. |
| Security | A listed instrument looked up in reference data by `SecurityId`. |
| Resolved trade | A trade with dates adjusted and indices looked up using `ReferenceData`, ready for pricing; `ResolvedTrade`. |
| Reference data | Static data such as holiday calendars and securities, provided through `ReferenceData` (`modules/basics/src/main/scala/com/opengamma/strata/basics/ReferenceData.scala`). |
| Market data | Observable and derived values (quotes, FX rates, curves, surfaces) keyed by `MarketDataId`; `MarketData` in `modules/data`. |
| Scenario market data | Market data with one value per scenario; `ScenarioMarketData`. |
| Rates provider | The pricer-side view of discount factors, forward rates, FX rates and fixings; `RatesProvider`. |
| Point sensitivity / parameter sensitivity | Sensitivity to individual rates at points in time (`PointSensitivities`), converted to sensitivity to curve parameters (`CurrencyParameterSensitivities`). |
| Measure | A named thing to calculate, such as present value; `Measure` in `calc`, constants in `Measures`. |
| Column | A measure plus optional reporting currency in the result grid; `Column`. |
| Calculation function | Code that calculates measures for one trade type; `CalculationFunction`, implemented per product in `measure`. |
| Calculation runner | The engine entry point; `CalculationRunner`. |
| Result / failure | A success value or a `Failure` with a `FailureReason`; `Result`. |
| Joda-Beans | Library that generates immutable bean code (getters, builders, meta-beans) from annotations. |
| Joda-Convert | Library that converts objects to and from strings via `@FromString` / `@ToString`. |
| Named / ExtendedEnum | Strata's extensible enum mechanism. Values are looked up by name from providers configured in INI files. |
| ResourceConfig | Loads and merges configuration files from `META-INF/com/opengamma/strata/config/{base,library,application}`. |
| Restorer | Build-only bytecode step in `modules/collect/src/build/` that adds back annotations or `throws` clauses scalac does not emit. |
