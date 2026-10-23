Strata
======

[![Strata Build Status](https://circleci.com/gh/OpenGamma/Strata.svg?style=shield)](https://strata.opengamma.io) [![License](http://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

This repository contains the source code of [Strata](https://strata.opengamma.io),
the open source analytics and market risk library from [OpenGamma](https://opengamma.com/).

Strata is released as Open Source Software under the
[Apache v2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html). 

[![OpenGamma](https://s3-eu-west-1.amazonaws.com/og-public-downloads/og-logo-alpha.png "OpenGamma")](https://opengamma.com/)


Using Strata
------------

Documentation for Strata can be found at the [Strata home page](https://strata.opengamma.io).

To use Strata, Java SE 17 or later is required.
The JAR files are available in [Maven Central](https://search.maven.org/search?q=g:com.opengamma.strata):

```
<dependency>
  <groupId>com.opengamma.strata</groupId>
  <artifactId>strata-measure</artifactId>
  <version>2.12.75</version>
</dependency>
```

The JAR files, along with the command line tool and examples, can also be obtained from
the [Strata Releases](https://github.com/OpenGamma/Strata/releases) page on GitHub.


Building Strata
---------------

The source code can be cloned using [git](http://git-scm.com/) from GitHub:

```
  git clone https://github.com/OpenGamma/Strata.git
```

The projects use [Apache Maven](https://maven.apache.org/) as the build system.
Version 3.5.0 or later is required.
Simply run this command to compile and install the source code locally:

```
  mvn install
```

Running a self-contained example
--------------------------------

The FX pricing example uses bundled sample market data to price example trades
and print a trade report with present value and PV01 results. No login or external
market-data service is required. With Java 17 or later and Maven 3.5 or later,
run these commands from the repository root:

```
  mvn install
  mvn -f examples/pom.xml org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.opengamma.strata.examples.FxPricingExample
```

Strata requires Java SE 17 or later.
The `strata-collect` and `strata-basics` modules are implemented in Scala 3.9.0 LTS,
which requires JDK 17 or later, and keep the same Java API as before.
Maven compiles them with `scala-maven-plugin`, and they depend on the Scala runtime library
(`scala3-library_3`). The other modules remain Java.
Our continuous integration regularly builds on Java 17 and Java 21.
We do not recommend use of non-LTS Java releases.

The Strata examples project includes a GUI based on JavaFX.
OpenJFX is included as a jar file from Maven Central, so the GUI is always compiled.

We recommend builds of OpenJDK from providers other than Oracle, notably
[Amazon Corretto](https://aws.amazon.com/corretto/) and [Adoptium](https://adoptium.net/).

For more information about developing code on Strata
see the [documentation](https://strata.opengamma.io).


Status
------

Strata is well-maintained, tested and functional.
It is used in production as the core of [OpenGamma SaaS Analytics](https://opengamma.com/).
The API will be maintained with backwards compatibility in mind.


Strata modules
--------------

Strata is formed from a number of modules:

* [Examples](examples/README.md)
* [Report](modules/report/README.md)
* [Measure](modules/measure/README.md)
* [Calc](modules/calc/README.md)
* [Loader](modules/loader/README.md)
* [Pricer](modules/pricer/README.md)
* [Market](modules/market/README.md)
* [Product](modules/product/README.md)
* [Data](modules/data/README.md)
* [Basics](modules/basics/README.md)
* [Collect](modules/collect/README.md)
