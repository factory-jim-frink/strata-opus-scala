# Dependencies

Strata keeps its runtime dependency list short: Guava, Joda-Beans and Joda-Convert underpin every module, `math` adds Apache Commons Math, and on this branch `collect` and `basics` add the Scala 3 standard library. Versions are managed in `modules/pom.xml`, and `examples/pom.xml` imports that POM as a BOM.

## Where versions are defined

| POM | Role |
| --- | --- |
| `pom.xml` | `strata-root` 2.12.78-SNAPSHOT. Aggregates `modules` and `examples`, defines the `-Ddist` and `-Dghdeploy` profiles. Declares no dependencies. |
| `modules/pom.xml` | `strata-parent` 2.12.78-SNAPSHOT. `dependencyManagement` for all Strata artifacts, third-party libraries and test libraries, and the version properties listed below. |
| `modules/math/pom.xml` | Defines `commons-math3.version` locally. |
| `modules/collect/pom.xml`, `modules/basics/pom.xml` | Define `scala.version` (3.9.0) and the Scala build plugins. |
| `examples/pom.xml` | Imports `strata-parent` as a BOM and adds `jcommander`, `slf4j-simple` and JavaFX. |
| `com.opengamma:corporate-parent:2.9.2` | External parent of the three top-level POMs. Supplies most Maven plugin versions and the `-Doss.repo` release profile. |

## Runtime dependencies

| Library | Version | Version source | Used directly by |
| --- | --- | --- | --- |
| `com.google.guava:guava` | 33.4.0-jre | `guava.version` in `modules/pom.xml` | `collect`, `calc` (all other modules get it transitively) |
| `org.joda:joda-beans` | 2.11.1 | `joda-beans.version` in `modules/pom.xml` | `collect`, `calc` |
| `org.joda:joda-convert` | 2.2.3 | `joda-convert.version` in `modules/pom.xml` | `collect`, `calc` |
| `org.scala-lang:scala3-library_3` | 3.9.0 | `scala.version` in `modules/collect/pom.xml` and `modules/basics/pom.xml` | `collect`, `basics` |
| `org.apache.commons:commons-math3` | 3.6.1 | `commons-math3.version` in `modules/math/pom.xml` | `math` |
| `org.slf4j:slf4j-api` | 1.7.36 | `slf4j.version` in `modules/pom.xml` | `math`, `calc` |
| `org.slf4j:slf4j-simple` | 1.7.36 | `slf4j.version` | `examples` |
| `com.beust:jcommander` | 1.78 | `jcommander.version` in `modules/pom.xml` | `examples` (command line parsing for `ReportRunnerTool`) |
| `org.openjfx:javafx-controls` | 11.0.2 | Inline in the `openjfx-dep` profile of `examples/pom.xml` | `examples` on JDK 11 and later (`gui` package) |

Guava is declared with exclusions for `error_prone_annotations`, `j2objc-annotations`, `animal-sniffer-annotations`, `checker-qual`, `checker-compat-qual`, `jsr305` and `listenablefuture`. A comment in `modules/pom.xml` notes that the Guava version was raised for a security fix. The `guava-docs.version` property (26.0-jre) only sets the Javadoc link target.

Counts: 9 distinct third-party runtime artifacts are declared directly. The 11 library modules use 6 of them (Guava, Joda-Beans, Joda-Convert, Scala 3 library, Commons Math, SLF4J API); the other 3 are used only by `examples`.

## Test dependencies

| Library | Version | Used by |
| --- | --- | --- |
| `org.junit.jupiter:junit-jupiter-api`, `junit-jupiter-params`, `junit-jupiter-engine` | 5.11.4 (imported through `org.junit:junit-bom`, `junit5.version`) | All 11 modules and `examples` |
| `org.assertj:assertj-core` | 3.27.2 | All 11 modules and `examples` |
| `org.mockito:mockito-core` | 4.9.0 | `collect`, `basics`, `data`, `product`, `market`, `loader`, `pricer`, `calc`, `measure` |
| `org.slf4j:slf4j-nop` | 1.7.36 | `math` |

Count: 6 third-party test artifacts. Modules also depend on each other's `test-jar` artifacts (for example `strata-collect` tests, which contain `TestHelper`), and `pricer` uses `strata-loader` in test scope only.

## Build-time artifacts

These are used only while building and are not shipped.

| Artifact | Version | Where | Purpose |
| --- | --- | --- | --- |
| `org.ow2.asm:asm` | 9.5 | `asm.version` in `modules/pom.xml`; plugin dependency of `bridger` and of `maven-antrun-plugin` in `modules/collect/pom.xml` | Bytecode reading and writing for Bridger and the restorers in `modules/collect/src/build`. |
| `org.scala-lang:scala3-compiler_3` | 3.9.0 | Plugin dependency of `maven-antrun-plugin` in `modules/collect/pom.xml` | Compiles the Scala test fixture used by the restorer control checks. |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.21 | `additionalDependencies` of `scala-maven-plugin` in both Scala modules | Works around a ScalaDoc 3.9.0 classpath problem. |
| Checkstyle | 8.45.1 (`build-config` 2.8.1) | `checkstyle.version` and `build-config.version` in `modules/pom.xml` | Rules for `maven-checkstyle-plugin`. |
| `org.codehaus.mojo:extra-enforcer-rules` | 1.2 | `extra-enforcer-rules.version` in `modules/pom.xml` | Pinned because later versions reject relative paths in `RequireFilesExist`. |

## Maven plugins

Versions marked "parent" come from the properties of `com.opengamma:corporate-parent:2.9.2` and are not set in this repository.

| Plugin | Version | Declared in | Purpose |
| --- | --- | --- | --- |
| `maven-checkstyle-plugin` | 3.1.2 (parent) | `modules/pom.xml` | Checkstyle at `process-sources`, fails on error. |
| `spotless-maven-plugin` | 2.43.0 | `modules/pom.xml`, `examples/pom.xml` | Checks changed Java files against `eclipse/install/Strata-Eclipse-Formatter-Java.xml`, `ratchetFrom main`. |
| `maven-pmd-plugin` | 3.21.2 | `modules/pom.xml`, `examples/pom.xml` | PMD reports with `config/pmd/cyclomatic-complexity.xml` and `config/pmd/dead-code.xml`. |
| `forbiddenapis` | 3.3 (parent) | `modules/pom.xml` | Blocks unsafe, deprecated and non-portable JDK APIs. |
| `maven-surefire-plugin` | 3.0.0-M5 (parent) | `modules/pom.xml`, `examples/pom.xml` | Tests with `forkCount` 2 and, in modules, one rerun of failing tests. |
| `maven-javadoc-plugin` | 3.4.1 (parent) | `modules/pom.xml`, `examples/pom.xml` | Javadoc; skipped in `collect` and `basics`. |
| `maven-jar-plugin` | 3.2.2 (parent) | `modules/pom.xml`, `modules/report/pom.xml` | Jars and test jars with `Automatic-Module-Name` and build number. |
| `buildnumber-maven-plugin` | 3.0.0 (parent) | `modules/pom.xml` | Records the git commit in jar manifests. |
| `maven-site-plugin` | 3.9.1 (parent) | `modules/pom.xml` | Attaches the site descriptor. |
| `bridger` (`org.jboss.bridger`) | 1.6.Final | `modules/pom.xml` (management), `modules/collect/pom.xml` | Bytecode transform at `process-classes`. |
| `scala-maven-plugin` (`net.alchim31.maven`) | 4.10.0 | `modules/collect/pom.xml`, `modules/basics/pom.xml` | Compiles Scala 3.9.0 with `-Xmixin-force-forwarders:false`; ScalaDoc jar under `-Ddist`. |
| `maven-antrun-plugin` | 3.1.0 | `modules/collect/pom.xml`, `compat` profile in `modules/pom.xml` | Runs the bytecode restorers and their control checks; merges japicmp output. |
| `maven-enforcer-plugin` | 3.0.0-M3 (`maven-enforcer-plugin.version` in `modules/pom.xml`) | `strict` profile | Dependency and environment rules. |
| `joda-beans-maven-plugin` | 1.3.2 (parent) | `strict` profile | Validates generated Joda-Beans code. |
| `maven-source-plugin` | 3.2.1 (parent) | `-Ddist` profiles | Source jars. |
| `japicmp-maven-plugin` | 0.15.7 (parent) | `compat` profile | API compatibility report against 2.0.0. |
| `maven-assembly-plugin` | 3.4.2 (parent) | `pom.xml`, `examples/pom.xml` (`-Ddist`) | Distribution and report-tool zips. |
| `maven-shade-plugin` | 3.3.0 (parent) | `examples/pom.xml` (`-Ddist`) | Report tool `jar-with-dependencies`. |
| `maven-dependency-plugin` | 3.1.1 (parent) | `modules/report/pom.xml` (`-Ddist`) | Copies dependencies for the distribution. |
| `maven-compiler-plugin` | 3.10.1 (parent) | `examples/pom.xml` (`javafx-missing` profile) | Excludes the JavaFX GUI sources. |
| `github-release-maven-plugin` (`com.ragedunicorn.tools.maven`) | 1.0.7 | `pom.xml` (`-Dghdeploy`) | Publishes release assets to GitHub. |

Count: 21 plugins are declared in the repository POMs, excluding the Eclipse m2e `lifecycle-mapping` entry, which has no effect on the Maven build.

The release path also uses plugins that are not declared here: `maven-gpg-plugin` 3.0.1 and `central-publishing-maven-plugin` 0.8.0 from the parent's `-Doss.repo` profile, `maven-release-plugin` 2.5.3 (parent) for `mvn release:prepare`, and `de.qaware.maven:go-offline-maven-plugin:1.1.0`, which `.circleci/config.yml` invokes by coordinates to fill the Maven cache. See [Deployment](../deployment.md).

## Dependency updates

`.github/dependabot.yml` configures Dependabot for the `maven` ecosystem at the repository root, weekly at 02:30, with up to 20 open pull requests, rebasing disabled, and `jodastephen` as assignee. It ignores `com.beust:jcommander` from 1.78.a upward and `org.openjfx:javafx-controls` from 12.a upward, which is why those two stay at 1.78 and 11.0.2.

The `-Dstrict` enforcer rules ban `commons-logging`, `org.json:json`, and JUnit 4 outside test or provided scope, and fail on duplicate classes.

## Related pages

- [Modules](../modules/index.md)
- [Configuration](configuration.md)
- [Scala and Java interoperability](../features/scala-java-interop.md)
- [Tooling](../how-to-contribute/tooling.md)
