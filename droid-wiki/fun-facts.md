# Fun facts

A few verified oddities from the history of `/home/factory-user/repos/auto_model_migration`. See [Lore](lore.md) for the full timeline.

## The code says 2009, the repository says 2014

The first commit (`b40d079`, 17 June 2014) holds only `.gitignore` and `README.md`, yet 317 Java and Scala source files carry the header `Copyright (C) 2009 - present by OpenGamma Inc.`. The likely reason is that this code was brought over from OpenGamma's earlier analytics platform. Two examples:

- `ArgChecker` came in on 2014-06-18 as `collect/src/main/java/com/opengamma/collect/validate/ArgumentChecker.java`, already with the 2009 header. Running `git blame main` on `modules/collect/src/main/java/com/opengamma/strata/collect/ArgChecker.java` still attributes 403 lines to that day, which makes them the oldest surviving lines in the repository. On this branch the class is now `modules/collect/src/main/scala/com/opengamma/strata/collect/ArgChecker.scala`.
- The whole `modules/math` module arrived in one commit on 2015-10-02 ("Add strata-math", 576 files), also with 2009 headers, which points to a bulk import of existing numerical code.

## The longest file is a test of one formula

The longest source file is `modules/pricer/src/test/java/com/opengamma/strata/pricer/impl/option/BlackFormulaRepositoryTest.java` at 8,959 lines, followed by its sibling `modules/pricer/src/test/java/com/opengamma/strata/pricer/impl/option/BlackScholesFormulaRepositoryTest.java` at 5,776 lines. Both test closed-form Black and Black-Scholes option formulas.

The longest production file is now Scala: `modules/basics/src/main/scala/com/opengamma/strata/basics/schedule/PeriodicSchedule.scala` at 2,021 lines, down from 2,084 lines in the Java version on `main`. The longest production Java file is `modules/pricer/src/main/java/com/opengamma/strata/pricer/capfloor/SabrIborCapletFloorletVolatilityCalibrationDefinition.java` at 1,579 lines.

## 46 TODOs and no FIXMEs

Java and Scala sources contain 46 `TODO` comments (35 in production code, 11 in tests) and no `FIXME` or `HACK` markers. Twenty-two of the 35 production TODOs are in `modules/math/src/main/java/com/opengamma/strata/math/impl`, for example three "TODO not tested" notes in `UncoupledParameterTransforms.java` and four "need a better seed" notes in the statistics distributions, all from the 2015-10-02 import.

The oldest TODO, by `git blame`, dates from 2015-05-01, in `modules/pricer/src/test/java/com/opengamma/strata/pricer/sensitivity/CurveGammaCalculatorTest.java` line 170: "swap USD standard conventions- TODO: replace by a template when available". Strata gained swap trade templates long ago, but the comment stayed. The oldest production TODO is from 2015-06-03, in `modules/report/src/main/java/com/opengamma/strata/report/framework/format/FormatSettingsProvider.java` line 23: "TODO extensibility - perhaps drive from properties file".

## Dependency archaeology

The core dependencies in `modules/pom.xml` have been the same since the first tag; only their versions changed.

| Library | `v0.7.0` (2015-06) | `v1.0.0` (2016-07) | `v2.0.0` (2018-08) | HEAD |
|---|---|---|---|---|
| Guava | 18.0 | 19.0 | 26.0-jre | 33.4.0-jre |
| Joda-Convert | 1.7 | 1.8.1 | 2.1.1 | 2.2.3 |
| Joda-Beans | 1.5.1 | 1.7 | 2.3 | 2.11.1 |
| AssertJ | 2.0.0 | 3.4.1 | 3.11.0 | 3.27.2 |
| Test framework | TestNG 6.9.4 | TestNG 6.9.10 | TestNG 6.14.2 | JUnit 5 |

A comment next to the Guava version in `modules/pom.xml` says the project "didn't want to go beyond v27 but forced to by security", linking to Guava issue 3320. The Javadoc links still point at Guava 26.0-jre docs through a separate `guava-docs.version` property.

## Index names change too

Strata tracks real-world benchmark changes in its code. The euro short-term rate was added as `EUR_ESTER`, then renamed to `EUR_ESTR` on 2019-10-23 (#2101) when the published name settled; the old constant survives as a deprecated alias in `modules/basics/src/main/scala/com/opengamma/strata/basics/index/OvernightIndices.scala`. In the same file `CHF_TOIS` is deprecated with the note "Not published as of 2017-12-29".

## Related pages

- [Lore](lore.md)
- [By the numbers](by-the-numbers.md)
- [Modules](modules/index.md)
