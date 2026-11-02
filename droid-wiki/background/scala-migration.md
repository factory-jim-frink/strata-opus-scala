# Scala migration

On the `auto_model_migration` branch the production code of the `collect` and `basics` modules was rewritten from Java to Scala 3.9.0 LTS, while every other module, every test and every external caller stays Java. This page tells the story of that change: what was in scope, the compatibility rules it had to respect, the decisions that shaped it, how it was validated, and what was changed after the validation record was written.

The technical mechanics (static forwarders, mixin forwarders, bytecode restorers, Scala beans) are described in [Scala and Java interoperability](../features/scala-java-interop.md). The primary record is `step_by_step_guide.md` at the repository root, an untracked file written during the work.

## Goal

`step_by_step_guide.md` states the goal as replacing "the Java production code of Strata's `collect` and `basics` modules with Scala 3.9.0 LTS while keeping their behaviour and their Java-facing API, so the other Java modules and external Java users keep working. Maven stays the build tool."

The guide calls this the "second migration run on this machine". It reuses every decision of an earlier run (recorded as `auto_model_migration-records-2026-10-21`) and marks the decisions that are new in this run. The guide does not explain why Scala was chosen; it treats the target language as given.

## Timeline

| Date (2026) | Commit or event | What happened |
| --- | --- | --- |
| 21 Oct | `fa85830` on `main` | Baseline: "Add current agent readiness report snapshot". The branch starts here. An earlier migration run is recorded under this date |
| 23 Oct, 00:10 UTC | `2042012` | "Add Scala 3.9.0 build support and pilot conversions in collect" |
| 23 Oct | `bfbda0c` | "Require Java 17 and drop Java 8/11 CI builds" |
| 23 Oct | `628997a` to `e68d7cd` | `collect` converted package by package (named, arrays, `ArgChecker`/`Guavate`, tuples, result, decimal and time series, byte sources, CSV/INI/XML/resource helpers); `987f24b` adds the `@SafeVarargs` and bridge `IOException` restorers |
| 23 Oct | `4571088` to `2df86f7` | `basics` converted (root, location and value types, currency, holiday calendars, Ibor and overnight indices, schedule, date conventions, remaining indices) |
| 23 Oct | `703ffe9` | Pins `scala-maven-plugin` 4.10.0 literally, because the strict enforcer rejects a property as plugin version |
| 23 Oct | `05d2aaf` to `7137a16` | Review fixes: `Result` failure property metadata, ScalaDoc links, `-Ddist` docs, `FailureItem` stack-trace frames |
| 23 Oct, 03:35 UTC | `7ca24cb` | "Keep the Java stack depth limit and drop mixin forwarders". The guide's validation record and the local tag `scala-migration-final` refer to this commit |
| 23 Oct, 20:42 UTC | `d9c689c` | "Use the default stack depth so the forbiddenapis check passes" |
| 23 Oct, 20:56 UTC | `7caa43e` | "Let the build helpers compile on JDK 21" (branch HEAD) |

All 26 branch commits are by `factory-jim-frink` and carry a `factory-droid[bot]` co-author line. `git diff --stat main...HEAD` reports 383 files changed, 62,013 insertions and 61,435 deletions; most of that is the Java-to-Scala rewrite of the same classes.

## Scope

From `step_by_step_guide.md`:

- 233 production Java implementation files moved to Scala in the same package under `src/main/scala`: 114 in `collect`, 119 in `basics`. Each Java file was removed.
- Unchanged: the 15 `package-info.java` files, the 176 Java test sources, the 64 tracked resources, and all production code of the other nine modules.
- Also changed: `modules/collect/pom.xml` and `modules/basics/pom.xml`, the build-only bytecode restorers under `modules/collect/src/build`, and the guidance and CI files `README.md`, `.circleci/config.yml`, `AGENTS.md`, `RELEASE.md`, `.factory/skills/build-and-test/SKILL.md` and `.factory/skills/code-conventions/SKILL.md`.

The guide's "Worktree boundaries" section rules out altering `main`, migrating another module, publishing artifacts or pushing the branch.

## The compatibility contract

`AGENTS.md` on the branch states the contract: the Java-facing API of `collect` and `basics` is "a compatibility contract for the Java modules and external Java users: keep class and member names, descriptors, static entry points, checked exceptions and Joda-Beans metadata unchanged."

In practice that covers three things:

1. **Binary Java API.** Java code compiled against the old jars must link, and Java source written against the old API must compile. The guide lists differences accepted in advance: wider visibility of some non-public members, Scala 3 enums with extra members and non-final constant fields, added companion classes and `Seq` overloads, a few redundant direct interfaces dropped where a superclass still provides them, and 47 constants that lose the JVM `ConstantValue` attribute (so external code can no longer use them in annotations or `switch` labels).
2. **Joda-Beans metadata.** Property names, order, types, validation and builders stay as the Java generator produced them. The Scala beans are hand-maintained copies of the generated code.
3. **Serialization.** Java serialization stream schemas and Joda-Beans serialized forms stay the same. Where a Scala field name would clash with an API method or suppress a static forwarder, explicit persistent fields and serialization hooks keep the stream unchanged.

## Key decisions and reasons

| Decision | Reason given in the guide or code |
| --- | --- |
| Raise the build and runtime floor to JDK 17 | Scala 3.9.0 needs JDK 17 to compile and run. CircleCI keeps `build` (now JDK 17, the check Mergify requires) and `build21`; JDK 8/11 jobs are removed |
| Keep the other Java modules at `--release 8` | A global javac release change would alter overload resolution in unrelated Java code. The Scala plugin emits release 17 bytecode |
| Keep tests in Java, Scala test compilation disabled | The Java tests compiled against the Scala bytecode exercise Java interoperability directly |
| `@static` companion members for Java static entry points | The static `Named.of` inherited from the Scala trait would otherwise suppress the Java static forwarder; `ResourceLocator` factories and `ResourceConfig.RESOURCE_DIRS_PROPERTY` need real JVM statics so caller-sensitive lookups see the Java frame layout and reading the constant does not load configuration |
| Utility classes as `final class X private ()` plus a companion object | They keep the single private constructor that `assertUtilityClass` checks |
| `-Xmixin-force-forwarders:false` | Forwarders only where the JVM needs them; Java classes have none, and extra forwarders add stack frames and change class shapes and reflection results |
| Bytecode restorers in `modules/collect/src/build` | Scala does not emit `@SafeVarargs` (8 `Guavate` methods) or the `throws IOException` that javac copies onto covariant bridges (2 `ArrayByteSource` bridges). Each restorer fails the build if its targets are missing or have an unexpected shape |
| `ExtendedEnum` accepts public static enum-constant fields | Scala 3 enum constant fields are not final; Java enum providers behave as before |
| `FailureItem` filters Scala forwarder frames | Recorded stack traces should look like the Java ones; `StackWalker` frames are compared by method descriptor to drop `@varargs` and static forwarder frames |
| Skip `maven-javadoc-plugin` in the two modules and attach ScalaDoc under `-Ddist` | Javadoc finds no Java classes there. In `collect` the ScalaDoc jar is built at `process-classes` before the restorers, because `doc-jar` forks a compile |
| Jackson annotations 2.21 only as a ScalaDoc plugin dependency | The ScalaDoc tool needs it; Jackson is not added to the published dependencies |
| `ImmutableHolidayCalendarDeserializer` uses a private `ParameterizedType` instead of anonymous `TypeToken` subclasses | Gives Joda-Beans the same `Set<LocalDate>` and `Set<DayOfWeek>` types |
| PMD baseline left unchanged | Findings for the removed Java files show as resolved, which the gate accepts |

## Validation

The guide's validation record applies to commit `7ca24cb`, built with OpenJDK 17.0.20.1 and Maven 3.8.7 on 23 Oct 2026, against `main` at `fa85830` built the same way. As recorded there:

| Check | Recorded result |
| --- | --- |
| `collect` and `basics` tests | 149 test classes, 8,190 tests, 0 failures, the same 6 skips as the baseline |
| Full reactor `test` | 14 projects, 17,914 tests, 0 failures, 6 skipped; the baseline has the same counts |
| Strict build (`-Dstrict -DskipTests package`) | Passes, including the enforcer and Joda-Beans validation |
| Release packaging (`-Ddist -DskipTests package`) | Passes; ScalaDoc javadoc jars produced; the `collect` jar keeps all 8 `@SafeVarargs` |
| PMD baseline gate | Passes: 0 increased, 11 resolved |
| Spotless in a fresh clone | Passes |
| Java API of packaged jars | 126 `collect` and 138 `basics` classes compared with the baseline: 0 blocking differences. Java clients compile with `--release 8 -Xlint:all -Xlint:-classfile -Werror` against both |
| Constants, `@SafeVarargs`, bridge exceptions | All match except the accepted `ConstantValue` loss |
| Java serialization | Stream schemas match for 39/39 `collect` and 66/66 `basics` classes; cross-version round trips pass for the sampled types |
| Behaviour probes | Identical output for XML parsing, `UnicodeBom`, `CsvIterator`, `ResourceLocator`, `ResourceConfig` start-up, holiday calendar types, Joda-Beans cases and `FailureItem` stack traces |
| Joda-Beans review | 62 bean types reviewed; one finding (`Result` lost its `failure` meta-property annotations), fixed |
| ScalaDoc | 0 warnings; every baseline public or protected type and all 15 packages have pages |

Not run: only JDK 17 was installed, so the `build21` CI job was not run locally. The record also does not list a `mvn verify` run; the later commit `d9c689c` reports that `mvn verify` failed in `strata-collect` on a forbiddenapis violation.

## Known residuals

The guide lists differences that remain after validation:

- In the 7 `ArgChecker.isTrue`/`isFalse` failure cases, the top stack frame names a private helper instead of `isTrue`/`isFalse`.
- Lambda frames in Strata code have Scala compiler names, such as `Result.ofOptional$anonfun$2` instead of `Result.lambda$ofOptional$1`.
- `Guavate`'s caller-class helper extends the deprecated `SecurityManager`, which produces one deprecation warning, as javac did.
- The private enum inside `DenseLocalDateDoubleTimeSeries` is a Scala 3 enum; its serialization is unchanged.
- `FloatingRateNames`, `FxIndices` and `PriceIndices` build their internal lookup on first use rather than at class load.
- With `-Ddist`, Javadoc of the Java modules no longer hyperlinks to `collect` and `basics` types.

## Changes after the recorded validation

Two commits were made on the evening of 23 Oct 2026, after `7ca24cb`, the commit the guide and the `scala-migration-final` tag describe. Both fix build failures that the recorded checks did not exercise.

- **`d9c689c` "Use the default stack depth so the forbiddenapis check passes".** `FailureItem.maxStackTraceDepth()` read `-XX:MaxJavaStackTraceDepth` through `com.sun.management.HotSpotDiagnosticMXBean`. The forbiddenapis `jdk-non-portable` signatures configured in `modules/pom.xml` ban that API, so `mvn verify` failed in `strata-collect`. The method in `modules/collect/src/main/scala/com/opengamma/strata/collect/result/FailureItem.scala` now returns the JVM default, 1024. A JVM started with a different `MaxJavaStackTraceDepth` is no longer reflected in the filtered trace length. The commit message reports `clean verify` passing for 14 of 14 projects.
- **`7caa43e` "Let the build helpers compile on JDK 21".** The restorers and their controls compile with `release="8"`, `-Xlint:all` and `-Werror`. JDK 21 javac warns that source and target 8 are obsolete, and `-Werror` turned that into an error in the `build21` job. `modules/collect/pom.xml` now adds `-Xlint:-options` to both `javac` tasks. The commit message reports `clean verify` on JDK 17 passing; it does not report a JDK 21 run.

`step_by_step_guide.md` has not been updated for these two commits.

## Status

The branch is local. The guide says it was "never pushed", and the `scala-migration-final` tag message says "local tag, not pushed". Nothing in the repository records a CircleCI run of the Scala build or a published release containing it. See [Deployment](../deployment.md) for how a release would normally be produced.

## Related pages

- [Scala and Java interoperability](../features/scala-java-interop.md)
- [collect](../modules/collect.md) and [basics](../modules/basics.md)
- [Pitfalls](pitfalls.md)
- [Lore](../lore.md)
- [Deployment](../deployment.md)
