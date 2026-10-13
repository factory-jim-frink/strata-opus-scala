# Agent Guide

## Architecture

The root Maven build combines `modules/` and `examples/`; the 11 library modules are built together, not deployed independently. `collect` and `basics` provide shared foundations. `data`, `product`, `market`, and `math` provide domain and mathematical models. `pricer` and `calc` provide pricing and calculation capabilities; `measure` combines those APIs. `loader` handles CSV/FpML input, while `report` formats calculation results. `examples` composes the library APIs.

The module POMs define inter-module dependencies. The `impl` packages in `pricer` and `loader` are documented as private implementation details.

## Setup and commands

Use Java 8u40 or later and Maven 3.5 or later. There is no Maven wrapper. The current CircleCI configuration builds with Java 8, 11, 17, and 21.

From the repository root:

```sh
mvn install
```

This builds and tests the multi-module project. To run tests without installing artifacts:

```sh
mvn test
```

To run one test class in the basics module:

```sh
mvn -pl modules/basics -am -Dtest=CurrencyAmountTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Maven runs the configured Checkstyle check during the build. For Eclipse formatting, import `eclipse/install/Strata-Eclipse-Formatter-Java.xml` as described in `eclipse/README.md`. Keep Java source/API compatibility at Java 8.

## Environment variables

Strata reads configuration from the classpath and from JVM system properties, not from a `.env` file. The root `.env.example` lists every environment variable that the build, the tests and the release process use, together with the file that consumes each one. Nothing in it is required to build the project, run the tests, or run the examples.

Two variables matter when reproducing a CI build:

* `JAVA_TOOL_OPTIONS` - CircleCI sets `-XX:MaxRAM=3572m`, because the JVM does not see the container cgroup memory limit. This also applies to the forked test JVMs.
* `MAVEN_OPTS` - CircleCI sets `-Xmx1g` for the Maven process.

The one runtime knob is the JVM system property `com.opengamma.strata.config.directories`, which changes the set of configuration directories searched by `ResourceConfig`. It is documented in `modules/basics/src/main/resources/META-INF/com/opengamma/strata/config/README.md`. Pass it with `-D`, for example through `MAVEN_OPTS`.

CI also drives the build with Maven flags rather than environment variables: `-Dstrict` enables the enforcer and Joda-Beans validation, and the release jobs add `-Ddist`, `-Doss.repo` and `-Dghdeploy`. Running `mvn install -Dstrict` reproduces the main CI build locally.
