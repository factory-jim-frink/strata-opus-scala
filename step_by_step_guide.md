# Step-by-step guide: Strata `collect` and `basics` in Scala 3.9.0

Created: 2026-10-23 (second migration run on this machine)

## Goal

Replace the Java production code of Strata's `collect` and `basics` modules
with Scala 3.9.0 LTS while keeping their behaviour and their Java-facing API,
so the other Java modules and external Java users keep working. Maven stays the
build tool. This run reuses every decision of the earlier migration
(`auto_model_migration-records-2026-10-21`), adapted only where this machine
differs.

## Worktree

| Item | Value |
| --- | --- |
| Source repository | `~/repos/strata` (`main`, left untouched) |
| Baseline commit | `fa85830` (`Add current agent readiness report snapshot`) |
| Worktree | `~/repos/auto_model_migration` |
| Branch | `auto_model_migration` (local only, never pushed) |

The worktree was created with:

```sh
git -C ~/repos/strata worktree add -b auto_model_migration ~/repos/auto_model_migration main
git -C ~/repos/auto_model_migration status --short --branch
```

The earlier run's second worktree (`opus55_model_migration`) belonged to a
separate effort and was not recreated. This guide is an untracked file in the
worktree root.

## Scope

- 233 production Java implementation files move to Scala at the same package
  under `src/main/scala`: `collect` 114, `basics` 119.
- Unchanged: the 15 `package-info.java` files, the 176 Java test sources, the 64
  tracked resources, and every other module's production code.
- Also changed: the two module POMs, build-only bytecode restorers under
  `modules/collect/src/build`, and the guidance/CI files `README.md`,
  `.circleci/config.yml`, `AGENTS.md`, `RELEASE.md`,
  `.factory/skills/build-and-test/SKILL.md` and
  `.factory/skills/code-conventions/SKILL.md`.

## Select JDK 17

Scala 3.9.0 needs JDK 17 or newer to compile and run, so the repository's and
its consumers' Java floor is now 17. On this machine OpenJDK 17.0.20.1 is
already the default `java`; nothing was installed and no system setting was
changed. Set it explicitly so the commands are reproducible:

```sh
cd ~/repos/auto_model_migration
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
```

CI keeps JDK 17 (`build`, the job Mergify requires) and JDK 21 (`build21`);
the JDK 8/11 jobs are gone and the release jobs use JDK 17.

## Build and validate

1. Compile, test and package the two converted modules:

   ```sh
   mvn -B -T 2 -Dspotless.check.skip=true -pl modules/collect,modules/basics -am package
   ```

2. Run the full reactor to check the Java consumers:

   ```sh
   mvn -B -T 2 -Dspotless.check.skip=true test
   ```

3. Generate the Scala API documentation:

   ```sh
   mvn -B -Dspotless.check.skip=true -pl modules/collect,modules/basics \
     net.alchim31.maven:scala-maven-plugin:4.10.0:doc
   ```

4. Check the release packaging (the CI release jobs and `RELEASE.md` use
   `-Ddist`):

   ```sh
   mvn -B -T 2 -Dspotless.check.skip=true -Ddist -DskipTests package
   ```

   With `-Ddist`, `collect` and `basics` attach their ScalaDoc as the
   `javadoc` jar (`target/strata-collect-2.12.78-SNAPSHOT-javadoc.jar` and
   the `basics` equivalent). The first run downloads plugins that only the
   release profile uses (for example `maven-shade-plugin` for `examples`).
   The shade plugin also writes an untracked `examples/dependency-reduced-pom.xml`,
   as it does on `main`; delete it afterwards.

Notes:

- `-Dspotless.check.skip=true` is needed only in a linked worktree or a copy
  without `.git`: Spotless uses `ratchetFrom main` and cannot find the
  repository there. In a normal clone, drop the flag; Spotless checks only
  changed Java files.
- `~/.m2` on this machine holds baseline `com.opengamma` 2.12.78-SNAPSHOT jars
  from an earlier `mvn install`. Build `basics` only together with `collect`
  in one reactor (as above), otherwise Maven resolves the old Java `collect`
  jar. This run never ran `mvn install`.
- Use at most two reactor threads (`-T 2`). No `clean` is needed.
- Step 3 runs the doc goal on its own. It forks a compile that rewrites
  `target/classes` without the `collect` restorers, so run step 1 (or any
  `package`) again before using `target/classes` or the JARs afterwards.

## Migration approach and consequential choices

These are the earlier run's decisions, reused unchanged unless marked "this run".

- Each implementation moves to the same package under `src/main/scala` and its
  Java file is removed. Java-friendly static factories, builders, getters,
  checked exceptions, varargs entry points and Joda-Beans metadata stay.
- Unchanged Java code keeps javac's release-8 setting; the Scala plugin emits
  release-17 bytecode. A global javac release change would alter overload
  resolution in unrelated Java code.
- Scala test compilation stays disabled. The regression suites remain Java, and
  compiling them against the Scala bytecode exercises Java interoperability.
- Joda-Beans metadata, property order and types, validation and serialized
  forms are preserved. Where a Scala field name would clash with a Java API
  method or suppress a static forwarder, explicit persistent fields and
  serialization hooks keep the stream schema unchanged.
- The ScalaDoc tool needs Jackson annotations 2.21 next to its Jackson 3.1
  dependency. This is confined to the Scala plugin's own dependencies; Jackson
  is not added to the published module dependencies.
- The five package-private `Standard*` enums keep their INI registrations.
  `ExtendedEnum` also recognizes public static enum-constant fields, because
  Scala enum fields are not final. Java enum providers behave as before.
- Scala does not emit `@SafeVarargs` on its Java varargs methods, nor the
  `throws IOException` clause javac copies onto covariant bridge methods. The
  `collect` POM therefore runs two build-only steps at `process-classes`, after
  Bridger: `src/build/java/RestoreSafeVarargs.java` restores `@SafeVarargs` on
  exactly eight `Guavate` overloads, and
  `src/build/java/RestoreBridgeExceptions.java` restores `IOException` on
  exactly the two `ArrayByteSource` `InputStream` bridges. Each fails the build
  if its targets are missing or have an unexpected shape. Standalone controls
  in `src/build/test` run in the `test` phase and are skipped with
  `-DskipTests`; the restore steps always run. ASM 9.5 and (this run) the
  Scala 3.9.0 compiler, used by the bridge control's Scala fixture in
  `src/build/test-fixtures`, are Antrun plugin dependencies only; nothing is
  packaged. The first build downloads Antrun 3.1.0 and Ant if not cached.
- This run: the Java floor follows Scala 3.9.0 to JDK 17. CircleCI keeps the
  required `build` job (now JDK 17) and `build21`, drops the JDK 8/11 jobs and
  the duplicate JDK 17 job, and runs the release jobs on JDK 17. The strict
  profile's enforcer rule in `modules/pom.xml` is unchanged, because scalac
  already refuses older JDKs.
- This run: classes implementing Strata's `Named` interfaces declare their
  companion `of(String)` as `@static`, because the static `Named.of` inherited
  from the Scala trait would otherwise suppress the Java static forwarder.
  Utility classes are `final class X private ()` plus a companion object, so
  they keep the single private constructor the tests check.
- This run: `ImmutableHolidayCalendarDeserializer` no longer uses anonymous
  `TypeToken` subclasses; a small private `ParameterizedType` gives Joda-Beans
  the same `Set<LocalDate>` and `Set<DayOfWeek>` types.
- This run: `ResourceLocator` factories and `ResourceConfig.RESOURCE_DIRS_PROPERTY`
  are JVM statics, so caller-sensitive lookups see the Java frame layout and
  reading the constant does not load configuration.
- This run: the stack traces that `FailureItem` records leave out the
  forwarder frames that Scala adds and Java does not have. `FailureItem`
  walks the stack with `StackWalker` and compares method descriptors: a
  `@varargs` forwarder (array parameter, calls the `Seq` version) and a
  static forwarder (class `X` calling the same method of the companion
  object `X$`) are dropped, and companion-object frames are printed as
  `X.method`, as in Java. All other frames are kept, including same-name
  overload calls and recursion. Like `Thread.getStackTrace`, it keeps at most
  `-XX:MaxJavaStackTraceDepth` frames. See the residuals for what still differs.
- This run: both modules compile with `-Xmixin-force-forwarders:false`, so
  Scala adds a forwarder for a trait or Java default method only where the JVM
  needs one. Java classes have none, and the forwarders would add stack frames.
  It also drops redundant direct interfaces (for example `Bean` on the bean
  classes), so class shapes and reflection results match the Java baseline.
- This run: `maven-javadoc-plugin` is skipped in `collect` and `basics`
  (it finds no Java classes there). A `dist`-activated profile in each module
  POM attaches the ScalaDoc as the `javadoc`-classifier jar instead. In
  `collect` it runs at `process-classes` before the restorers, because
  `doc-jar` forks a compile. `RELEASE.md` no longer passes the JDK 11-only
  `-DadditionalJOption=--no-module-directories` flag.
- The PMD baseline `config/pmd/pmd-baseline.json` is unchanged. Findings for
  the removed Java files are resolved, which CI accepts.

## Accepted Java API differences

These differences were accepted in advance (carried over from the earlier run):

- Scala may widen protected, package-private and companion-accessed members in
  bytecode. It must not remove or narrow the baseline API.
- The six public enums and five package-private `Standard*` enums are Scala 3
  enums: extra members, abstract/non-final class flags and non-final constant
  fields. Java `switch`, `values`, `valueOf`, `EnumSet`/`EnumMap`,
  serialization identity and Joda-Convert still work.
- Companion classes, accessors, default-method forwarders and Scala `Seq`
  overloads may be added. Raw generic signatures may become unbounded wildcard
  signatures with the same erasure.
- A redundant direct interface may disappear when a superclass still
  implements it, for example `ImmutableBean` on `StringCharSource`,
  `FileByteSource`, `UriByteSource` and `ArrayByteSource`. Assignability stays.
- Only `IborIndex`, `OvernightIndex`, `FxIndex`, `FloatingRateName` and
  `PriceIndex` may omit a direct `Named` entry while inheriting it through a
  parent interface.
- The 47 constants in `StandardSchemes` (24), `FailureAttributeKeys` (18),
  `ResourceLocator` (3), `ResourceConfig` (1) and `FailureItem` (1) keep their
  names, values, types and `public static final` modifiers but lose the JVM
  `ConstantValue` attribute. External callers can no longer use them in
  annotation values or `switch` labels, and no longer inline their values.
- Behaviour amendment: `ExtendedEnum` also registers public static
  enum-constant fields from `constants` providers (see above).

These exceptions do not waive the behavioural tests, Joda-Beans
interoperability, or the remaining API and serialization obligations.

## Validation record

All results below are for commit `7ca24cb` on `auto_model_migration`, built with
OpenJDK 17.0.20.1 and Maven 3.8.7 on 2026-10-23. The baseline is `main` at
`fa85830`, built the same way from a clean copy. Only JDK 17 is installed on
this machine, so the JDK 21 CI job (`build21`) was not run locally.

| Check | Result |
| --- | --- |
| `collect` and `basics` tests | 149/149 test classes, 8,190 tests (2,376 + 5,814), 0 failures or errors, the same 6 skipped tests as the baseline (`DecimalTest` x4, `NumberFormatterTest` x2) |
| Full reactor `test` | All 14 projects pass, 17,914 tests, 0 failures or errors, 6 skipped; the baseline's full reactor, run on `fa85830`, has the same 17,914 tests and 6 skips |
| CI strict build (`-Dstrict -DskipTests package`) | All 14 projects pass, including the enforcer and Joda-Beans validation on the Scala modules |
| Release packaging (`-Ddist -DskipTests package`) | All 14 projects pass. The `collect` and `basics` javadoc jars contain the ScalaDoc with an `index.html` and a page for every package; the `collect` jar built this way keeps all 8 `@SafeVarargs` annotations |
| PMD baseline gate (`python3 config/pmd/check-pmd-baseline.py`) | Passes: 0 findings increased, 11 resolved (the removed Java files) |
| Spotless (`spotless:check`) in a fresh clone | Passes. Spotless checks Java files under `src/main/java` and `src/test/java`; the build-only restorers under `src/build` are outside its scope, as in CI |
| Java API of the packaged JARs | 126 `collect` and 138 `basics` exported classes compared with the baseline JARs: 0 blocking differences, only the accepted categories above. Java clients compile with `--release 8 -Xlint:all -Xlint:-classfile -Werror` against both versions |
| Constants | All 47 keep name, type, modifiers and value; only the accepted `ConstantValue` loss |
| `@SafeVarargs` and bridge exceptions | 8/8 `Guavate` methods and 2/2 `ArrayByteSource` bridges match the baseline; no build helper or ASM class is packaged |
| Java serialization | Stream schemas match for 39/39 `collect` and 66/66 `basics` classes; the two anonymous `TypeToken` helpers were removed by design. Cross-version round trips pass for the time series (12/12 sparse, 32/32 dense), `FixedScaleDecimal` (8/8) and `MultiCurrencyAmountArray` (12/12) |
| Behaviour probes, baseline vs Scala | Identical output for XML parsing (10 cases), `UnicodeBom` beans (8), `CsvIterator` close handling (2), `ResourceLocator` caller context (4), `ResourceConfig` start-up (8), holiday calendar generic types (2), the Joda-Beans review cases (9) and `FailureItem` stack traces (29, Strata frames compared by class and method; in 5 cases the Strata lambda method name is ignored) |
| Joda-Beans review | All 62 bean types reviewed. One finding, fixed: `Result` had lost the annotations of its `failure` meta-property; its field is now `_failure` (Java serialization still uses `failure`) |
| ScalaDoc | Builds for both modules with 0 ScalaDoc warnings; every baseline public/protected type (126 + 138) and all 15 packages have pages |

Known residuals:

- `ArgChecker`: in the 7 `isTrue`/`isFalse` failure cases the top stack frame
  names a private helper method instead of `isTrue`/`isFalse`. The class and
  the messages are unchanged, and no Strata test or code depends on the
  method name.
- Stack traces: lambda methods inside Strata code have compiler-chosen names,
  for example `Result.ofOptional$anonfun$2` or
  `LocalDateDoubleTimeSeriesBuilder.$anonfun$1` where Java shows
  `Result.lambda$ofOptional$1` or `LocalDateDoubleTimeSeriesBuilder.lambda$merge$1`.
  The frame is in the same place.
- `Guavate`'s caller-class helper extends the deprecated `SecurityManager`, as
  in the Java code, so the Scala compiler reports one deprecation warning (the
  baseline javac reports the same). The doc goal shows it too, because it
  compiles first; ScalaDoc itself reports no warnings.
- The private enum inside `DenseLocalDateDoubleTimeSeries` is a Scala 3 enum.
  It is not API; serialization of its constants is unchanged.
- Three lookup holders (`FloatingRateNames`, `FxIndices`, `PriceIndices`) now
  build their internal lookup on first use instead of at class load. Results
  are the same.
- After the first successful run, the `-Dstrict` build exposed that the enforcer
  rejects a property as plugin version, so both POMs write
  `scala-maven-plugin` `4.10.0` literally.
- With `-Ddist`, the javadoc of the other (Java) modules no longer links to
  `collect` and `basics` types, because those modules now publish ScalaDoc,
  not javadoc. The type names still appear; only the hyperlinks are missing,
  and the build logs "Error fetching link ... Ignored it".

## Worktree boundaries

Keep migration commits on `auto_model_migration`. Do not alter the `main`
checkout, migrate another module, publish artifacts, or push this branch.
