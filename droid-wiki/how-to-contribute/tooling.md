# Tooling

This page lists the build plugins, quality gates, IDE settings, CI setup and agent tooling that a Strata contributor works with, with the versions pinned in the repository. Plugins without a version in the repository take it from the OpenGamma `com.opengamma:corporate-parent` 2.9.2 POM, which is the parent of `pom.xml`, `modules/pom.xml` and `examples/pom.xml` and is not stored here.

## Maven build structure

- `pom.xml` (`strata-root`) aggregates `modules` and `examples`. It is never installed or deployed (`maven.install.skip` and `maven.deploy.skip` are true) and only adds the distribution zip (`-Ddist`) and GitHub release (`-Dghdeploy`) profiles.
- `modules/pom.xml` (`strata-parent`, version 2.12.78-SNAPSHOT) holds the shared build configuration, dependency versions and profiles for the eleven library modules.
- `examples/pom.xml` has `corporate-parent` as its parent rather than `strata-parent`, so it repeats some plugin configuration (Surefire, Spotless, PMD) and imports `strata-parent` for dependency management.
- `modules/collect/pom.xml` and `modules/basics/pom.xml` add the Scala compiler; `modules/collect/pom.xml` also runs the bytecode restorers.

## Plugins by phase

| Plugin | Version | Where | What it does |
| --- | --- | --- | --- |
| `spotless-maven-plugin` | 2.43.0 | `modules/pom.xml`, `examples/pom.xml` | `check` at `validate`: changed Java files (`ratchetFrom main`) must match `eclipse/install/Strata-Eclipse-Formatter-Java.xml` |
| `joda-beans-maven-plugin` | from corporate-parent; Joda-Beans library 2.11.1 | `strict` profile in `modules/pom.xml` | `validate` at `validate` with `stopOnError`: generated bean code must be current |
| `maven-enforcer-plugin` | 3.0.0-M3 (property `maven-enforcer-plugin.version`) | `strict` profile | Bans `commons-logging`, `org.json:json` and non-test `junit:*`, bans duplicate classes, requires plugin versions and Java 1.8 or later |
| `maven-checkstyle-plugin` | Checkstyle 8.45.1, rules from OpenGamma `build-config` 2.8.1 | `modules/pom.xml` | `checkstyle` at `process-sources` with `failsOnError` |
| `maven-pmd-plugin` | 3.21.2 | `modules/pom.xml`, `examples/pom.xml` | `pmd` at `process-sources` with `config/pmd/cyclomatic-complexity.xml` and `config/pmd/dead-code.xml`, writing `target/pmd.xml`. It only reports; the gate is the baseline script |
| `buildnumber-maven-plugin` | corporate-parent | `modules/pom.xml` | Records the git commit at `process-sources` for the jar manifest (`Implementation-Build`) |
| `scala-maven-plugin` | 4.10.0 | `modules/collect/pom.xml`, `modules/basics/pom.xml` | `add-source` and `compile` at `process-resources`; Scala 3.9.0, `release` 17, `compileOrder` Mixed, flags `-deprecation -feature -Xmixin-force-forwarders:false` |
| `maven-compiler-plugin` | corporate-parent | all modules | Compiles Java with `maven.compiler.release` 8 (profile `forcejavaversion`, active on JDK 9 and later) |
| `bridger` (`org.jboss.bridger`) | 1.6.Final with ASM 9.5 | `modules/collect/pom.xml` (managed in `modules/pom.xml`) | `transform` at `process-classes` |
| `maven-antrun-plugin` | 3.1.0 | `modules/collect/pom.xml` | Compiles and runs the bytecode restorers at `process-classes`, and their controls at `test` |
| `forbiddenapis` (`de.thetaphi`) | corporate-parent | `modules/pom.xml` | `check` and `testCheck` with the `jdk-unsafe`, `jdk-deprecated` and `jdk-non-portable` signatures |
| `maven-surefire-plugin` | corporate-parent | `modules/pom.xml`, `examples/pom.xml` | Runs tests in two reused forks, with one rerun of failing tests in the library modules |
| `maven-jar-plugin` | corporate-parent | `modules/pom.xml` | Builds the main jar (with `Automatic-Module-Name`) and a `test-jar` per module |
| `maven-source-plugin`, `maven-javadoc-plugin` | corporate-parent | `-Ddist` profile | Source and Javadoc jars plus an aggregate Javadoc with package groups. `collect` and `basics` skip Javadoc and attach ScalaDoc through `scala-maven-plugin` `doc-jar` instead |
| `maven-shade-plugin`, `maven-assembly-plugin` | corporate-parent | `examples/pom.xml` and `pom.xml` under `-Ddist` | Build `strata-report-tool-<version>.zip` (main class `ReportRunnerTool`) and `strata-dist-<version>.zip` from `src/assembly/dist.xml` |
| `japicmp-maven-plugin` | corporate-parent | `compat` profile (`-Dcompat`) | Compares each module's API with version 2.0.0 at `verify` |
| `github-release-maven-plugin` | 1.0.7 | `pom.xml`, `ghdeploy` profile | Uploads the zips to GitHub Releases |
| `exec-maven-plugin` | 3.1.0 (command line only) | none | Used to run examples: `mvn -f examples/pom.xml org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=...` |

### Maven flags

| Flag | Effect |
| --- | --- |
| `-Dstrict` | Enables enforcer and Joda-Beans validation. CI always uses it |
| `-Ddist` | Source, Javadoc and ScalaDoc jars, report tool zip and distribution zip |
| `-Doss.repo` | Deploy target for releases (configured in `corporate-parent`) |
| `-Dghdeploy` | GitHub release upload |
| `-Dcompat` | `japicmp` compatibility report |
| `-Dspotless.check.skip=true` | Skip Spotless, needed in linked worktrees |
| `-Dcheckstyle.skip=true` | Skip Checkstyle; CI uses it only for steps that run after a checked install |

[Configuration](../reference/configuration.md) lists these alongside the runtime system properties.

## Quality gates

### Checkstyle and forbiddenapis

Both run on every build. The Checkstyle ruleset lives in the external `build-config` artifact, so the version properties `build-config.version` and `checkstyle.version` in `modules/pom.xml` are the only local knobs. forbiddenapis has `failOnUnsupportedJava` false so newer JDKs do not stop it.

### PMD baseline

PMD reports two rule groups:

- `config/pmd/cyclomatic-complexity.xml`: `CyclomaticComplexity` with `methodReportLevel` 16 and `classReportLevel` 250
- `config/pmd/dead-code.xml`: `UnusedAssignment`, `UnusedPrivateField`, `UnusedPrivateMethod`

`config/pmd/check-pmd-baseline.py` reads every module's `target/pmd.xml` (plus `examples/target/pmd.xml`) and compares the findings with `config/pmd/pmd-baseline.json`, which currently holds 186 entries. It fails on a new finding, on higher complexity for an existing method, on more dead-code findings for an existing symbol, on a missing report, or when the rules or thresholds in the XML no longer match the baseline. Class-level complexity is reported but not gated. Run it with `--update-baseline` only after reviewing the new findings. CI runs it after `mvn test` and uploads the PMD reports with the test results.

PMD runs on Java sources only, so the Scala code in `collect` and `basics` is not measured.

### Spotless

Spotless only formats Java, only checks files changed relative to `main`, and needs a real git checkout. See [Development workflow](development-workflow.md#formatting).

## Scala build and bytecode restorers

The two Scala modules compile with `scala-maven-plugin` 4.10.0 and depend on `scala3-library_3` 3.9.0 at runtime. Key settings in `modules/collect/pom.xml` and `modules/basics/pom.xml`:

- `compileOrder` Mixed and `sendJavaToScalac` true, so Java and Scala sources in the same module can refer to each other.
- `-Xmixin-force-forwarders:false`, so Scala only emits the trait forwarders the JVM needs and class files look like the Java originals.
- An `additionalDependency` on `jackson-annotations` 2.21 for ScalaDoc, which otherwise resolves a version that lacks `JsonSerializeAs`.
- Under `-Ddist`, a `scaladoc-jar` profile attaches ScalaDoc as the `javadoc` classifier at `process-classes`, before the restorers, so the forked compile does not overwrite restored classes. `maven.javadoc.skip` is true for these modules.

### Bytecode restorers

Scala 3 does not emit two pieces of metadata that the Java API of `collect` had. Build-only Java programs in `modules/collect/src/build` add them back after compilation:

| File | Role |
| --- | --- |
| `modules/collect/src/build/java/RestoreSafeVarargs.java` | Adds `@SafeVarargs` to eight `Guavate` varargs methods |
| `modules/collect/src/build/java/RestoreBridgeExceptions.java` | Adds `throws IOException` to two `ArrayByteSource` bridge methods |
| `modules/collect/src/build/test/RestoreSafeVarargsControls.java` | Checks `RestoreSafeVarargs` against ASM fixtures and the real `Guavate` class |
| `modules/collect/src/build/test/RestoreBridgeExceptionsControls.java` | Checks `RestoreBridgeExceptions` against ASM fixtures, a Scala fixture and the real `ArrayByteSource` |
| `modules/collect/src/build/test-fixtures/ArrayByteSource.scala` | Scala 3 class with the same bridge shape, compiled during the controls |

`maven-antrun-plugin` 3.1.0 compiles the helpers with `javac release="8"`, `-Xlint:all`, `-Xlint:-options` and `-Werror`, using ASM 9.5 and `scala3-compiler_3` as plugin dependencies only, so nothing from them is packaged. The restorers run at `process-classes` after Bridger, and the controls run at `test` unless `-DskipTests` is set. The `-Xlint:-options` flag came in commit `7caa43e` so the helpers compile on JDK 21. [Scala and Java interoperability](../features/scala-java-interop.md) explains why the metadata matters.

## IDE setup

`eclipse/README.md` describes an Eclipse setup with m2e:

- `eclipse/install/Strata-Eclipse-Preferences.epf`: workspace preferences, imported through File, Import, General, Preferences
- `eclipse/install/Strata-Eclipse-Formatter-Java.xml`: the Java formatter, also the profile Spotless checks against
- `eclipse/install/Strata-Eclipse-CodeTemplates-Java.xml`: code templates
- `eclipse/install/Strata-Eclipse-UpdateSites.xml`: update site list
- `eclipse/launch/`: one "All Strata-<Module>" launch configuration per module to run its tests

The README warns that the source must not be inside the Eclipse workspace directory. It predates the Scala migration, so Eclipse needs a Scala 3 plugin (or a Maven build) to compile `collect` and `basics`. `.gitignore` excludes Eclipse and IntelliJ project files but keeps `.idea/runConfigurations/`.

## CI

`.circleci/config.yml` defines executors for `cimg/openjdk:17.0` and `cimg/openjdk:21.0`, both with `JAVA_TOOL_OPTIONS=-XX:MaxRAM=3572m` and `MAVEN_OPTS=-Xmx1g`. Every Maven call gets `-e -B -DtrimStackTrace=false` and `--settings .circleci/maven-settings.xml` through a generated `.mvn/maven.config`. Dependencies are prefetched with `de.qaware.maven:go-offline-maven-plugin:1.1.0` and cached per job keyed on the root `pom.xml`. The jobs are `build`, `build21`, `release`, `release_part2` and `trigger_release`; [Deployment](../deployment.md) covers them. GitHub automation is Mergify (`.github/mergify.yml`) and Dependabot (`.github/dependabot.yml`), described on [How to contribute](index.md).

## Agent tooling

The repository includes files aimed at coding agents:

| File | Contents |
| --- | --- |
| `AGENTS.md` | Architecture summary, setup and commands, the Scala compatibility contract, worktree and Spotless note, environment variables |
| `.factory/skills/build-and-test/SKILL.md` | Build, focused test, Checkstyle and Joda-Beans failure handling, running examples, module map |
| `.factory/skills/code-conventions/SKILL.md` | Value class and light bean patterns, style, argument validation, tests, public API rules |
| `.factory/agent-readiness-report.json` | Readiness assessment of `main` at commit `39d6e79` (21 October 2026): level 3, 30 of 70 applicable checks passed (42.86%). Failing checks include pre-commit hooks, PR and issue templates, test coverage thresholds, flaky test detection and devcontainer |
| `.env.example` | Every environment variable the build, tests and release use, with the file that reads it. None are needed to build or test; release values are secrets and `.env` is git-ignored |

These were added on `main` by three commits from October 2026 (`967ceb4`, `39d6e79`, `fa85830`) `AGENTS.md`, both skills, `README.md` and `RELEASE.md` were then updated on this branch to describe the Scala modules.

## Related pages

- [Development workflow](development-workflow.md)
- [Debugging](debugging.md)
- [Dependencies](../reference/dependencies.md)
- [Deployment](../deployment.md)
