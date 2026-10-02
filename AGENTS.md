# AGENTS.md

## Project overview

ADAPT (**A**utomated **D**etection and **A**nalysis of **P**ro**T**rusions) is an
[ImageJ/Fiji](http://fiji.sc/) plugin (Java 21, built with Maven) for automated
detection and analysis of cell migration, membrane protrusions, and associated
fluorescence intensity. It consumes two image stacks per cell (a cytoplasmic
channel used for segmentation and a "signal" channel) and produces
curvature/velocity/signal maps, per-cell and per-protrusion metrics, and
visualisations.

The heavy lifting (segmentation, curvature analysis, trajectory analysis,
Bio-Formats I/O, `UserVariables` parameters) is done by two external
dependencies pulled from JitPack (`IAClassLibrary` `v2.0.1` and `TrackerLibrary`
`v4.0.2`, both under `com.github.djpbarry`, pinned to tagged releases). ADAPT
also contains substantial in-repo domain logic, so it is not purely
orchestration/glue. When searching for how a step actually works, look at these
libraries first, not this repo.

## Build / test / run

- **Build + verify:** `mvnw verify` (Maven wrapper; on Windows use `mvnw.cmd`).
  - Requires **JDK 21+** on `JAVA_HOME` (Temurin 21 at
    `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` is the local
    setup). Compile target is **Java 21** (`scijava.jvm.version=21`, parent
    `pom-scijava:45.1.0`).
  - Local Maven install (only needed if bypassing the wrapper):
    `C:\Program Files\apache-maven-3.9.16`.
  - Dependencies resolve from Maven Central (`central`, declared first), SciJava
    (`maven.scijava.org`), and JitPack (`com.github.djpbarry:*`); no auth needed.
  - `mvn_settings.xml` (GitHub Packages + PAT) has been **removed**.
  - `IAClassLibrary` depends on `sc.fiji:TrackMate` transitively; ADAPT resolves
    TrackMate 8.0.0 via the parent (no explicit pin).
- **CI:** `.github/workflows/maven.yml` runs
  `./mvnw --batch-mode --no-transfer-progress verify` on `ubuntu-latest` with
  JDK 21 (Temurin), Maven caching, and `actions/checkout@v5` /
  `actions/setup-java@v5`.
- **Packaging:** the parent POM is `org.scijava:pom-scijava:45.1.0`; the
  `maven-dependency-plugin` copies all dependencies into `target/` on `package`.
- **Run/debug:** `main-class` is `net.calm.adapt.Adapt.Main`. Its `main()` calls
  `Analyse_Movie.initialise()` then `run(null)`, which is a debug path that opens
  images via dialog (`IJ.openImage()`), not the normal plugin entry point. Under
  Fiji the real entry points are the two plugins declared in
  `src/main/resources/plugins.config`.
- **Local Fiji testing:** `bin/install-to-fiji.cmd`, `bin/run-fiji.cmd`, and
  `bin/smoke-test-fiji.cmd` stage/launch/test the plugin in a local Fiji.
  Machine-specific `FIJI_DIR`/`JAVA_HOME` live in `bin/local-env.cmd`
  (gitignored; template is `bin/local-env.cmd.example`).
- **No unit tests exist.** There is no `src/test`, no test framework configured,
  and no lint/format tooling. Verification is compilation plus manual runs in
  Fiji. Test data ships as extracted `.ome.tiff` stacks under `test_data/`
  (Git LFS-tracked); ADAPT output trees are gitignored.

## Entry points / plugin registration

Fiji discovers commands via `src/main/resources/plugins.config`, not annotations:

```
Plugins>Adapt, "Analyse Movie", net.calm.adapt.Adapt.Analyse_Movie
Plugins>Adapt, "Batch Analysis", net.calm.adapt.Adapt.Analyse_Batch
```

These two classes implement ImageJ's `ij.plugin.PlugIn` (`run(String arg)`).
To add a new plugin command, add a line to `plugins.config` AND the class must
implement `PlugIn`.

## Architecture and data flow

Package `net.calm.adapt` is split into four sub-packages, matching the class
prefixes you'll see in imports:

- **`Adapt/`** — core plugin logic and domain classes.
  - `Analyse_Movie` — main single-movie analysis pipeline. `run()` prompts for
    an output dir, then `analyse()` does segmentation → morphology → velocity/
    signal map building → protrusion analysis, and finally runs
    `TrajectoryAnalysis` and saves a properties file. Note: it extends
    `NotificationThread` (not `Thread`).
  - `Analyse_Batch` — extends `Analyse_Movie`; iterates over a directory of
    image files, reusing the same `analyse()` per file. Also contains
    `readParams()` which parses a `params.csv` line-by-line with a positional
    `Scanner` — this is brittle and order-sensitive.
  - Domain/helper classes: `Bleb` (extends `Protrusion`), `BlebAnalyser`,
    `CurveMapAnalyser`, `FluorescenceDistAnalyser`,
    `RegionFluorescenceQuantifier`, `Protrusion`, `StaticVariables` (all GUI
    label strings + output column headings + shared `DecimalFormat`s),
    `NotificationThread`/`TaskListener` (observer pattern for thread completion).
- **`Output/`** — result writing.
  - `MultiThreadedOutputGenerator extends MultiThreadedProcess`; submits one
    `RunnableOutputGenerator` (a `RunnableProcess`) per cell to a
    `fixedThreadPool(availableProcessors())`. Per cell it builds morphology/
    velocity/signal maps and, if protrusion analysis is on, recurses by
    constructing a *new* `Analyse_Movie` in `protMode`.
  - `RunnableOutputGenerator.buildOutput()` is the longest, most intricate
    method; it writes CSV/visual outputs and handles protrusion/bleb detection.
- **`Visualisation/`** — `MultiThreadedVisualisationGenerator` + per-frame
  `RunnableVisualisationGenerator`; renders velocity/curvature overlays as TIFF
  via `BioFormatsImageWriter`, one task per frame.
- **`ui/`** — `GUI` (a `javax.swing.JDialog` netbeans-generated form; source and
  layout defined jointly by `GUI.java` + `GUI.form`). Holds a single **static**
  `UserVariables` instance (`UV`) populated when the user OKs the dialog;
  `Analyse_Batch` reads it back via `GUI.getUv()`.

Control flow: `Analyse_Movie.run()` / `Analyse_Batch.run()` → `analyse()` →
`MultiThreadedOutputGenerator` → (optional) `MultiThreadedVisualisationGenerator`.

## Concurrency model

- Two patterns coexist. `NotificationThread` (in `Adapt/`) is an
  observer/callback wrapper: subclasses implement `doWork()`; it notifies
  registered `TaskListener`s on completion. The `MultiThreaded*` generators (in
  `Output/`/`Visualisation/`) instead extend the IAClassLibrary
  `MultiThreadedProcess`/`RunnableProcess` base classes and manage their own
  `ExecutorService` with `terminate(msg)`.
- Thread pools are always sized to `Runtime.getRuntime().availableProcessors()`.
- Do not confuse the two custom bases: `RunnableProcess` (external lib) and
  `NotificationThread` (in repo) are unrelated.

## Conventions and gotchas

- **Package names map to directories with mixed case** (`Adapt`, `Output`,
  `Visualisation`, `ui`) — unusual for Java but intentional; keep it consistent.
- **`StaticVariables` is the single source of truth** for GUI labels and output
  column names (e.g. `TIME`, `VELOCITY`, `TOTAL_SIGNAL`). Column headings are
  wired into CSV writers; changing a string here changes output schema.
- **Versioning:** the reported title is `Adapt_v<version>` where `<version>` is
  read at runtime from `project.properties`, which Maven filters from
  `${project.version}` (`pom.xml` sets `<filtering>true</filtering>`). Bump the
  `pom.xml` version; do not edit `project.properties` by hand.
- **`readParams()`** in `Analyse_Batch` parses `params.csv` with hard-coded
  `br.readLine()` skips and positional `Scanner` calls; any change to parameter
  ordering in the file breaks it silently (caught as a generic `Exception`).
- **Directory delimiter:** code uses `GenUtils.getDelimiter()` (in
  `Analyse_Movie`) rather than `File.separator` in some paths; `Analyse_Batch`
  also does `directory.getAbsolutePath() + delimiter + ".."` for the parent.
- **Statics on `GUI`:** `UserVariables UV` and the `VERSION`-style labels are
  static/shared; the GUI dialog is modal and populated once per batch run.
- **Dead code was removed in M1.** The commented-out experiments in `Main.java`,
  `Analyse_Movie`, `BlebAnalyser`, and `RunnableOutputGenerator` were stripped
  (recoverable from git history); remaining comments are legitimate
  documentation, not stale experiments.
- **License is GPL-3.0** (aligned in M1): `pom.xml` (`license.licenseName=gpl_v3`)
  and source headers now agree. `license.copyrightOwners` is **David Barry**
  (development predates the Francis Crick Institute, so the Crick cannot claim
  copyright).
- A `.gitignore` and `.gitattributes` are present (`target/`, IDE files, and OS
  files excluded; `mvnw` is LF and tracked executable).

## Domain terms

- `MorphMap` (external): the upscaled, boundary-length-normalised
  (power-of-two) map storing velocity/signal/curvature per cell over time; a
  core abstraction passed between library and this repo.
- `UserVariables` (external): a bean holding every analysis parameter; cloned
  via `(UserVariables) uv.clone()` when recursing into protrusion analysis.
- `protrusions` vs `blebs`: protrusion analysis can run in two modes — velocity-
  based bleb detection (`isBlebDetect()`) or morphology-based protrusion
  detection (`findProtrusionsBasedOnMorph()`), selected via `UserVariables`.
