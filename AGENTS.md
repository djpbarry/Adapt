# AGENTS.md

## Project overview

ADAPT (**A**utomated **D**etection and **A**nalysis of **P**ro**T**rusions) is an
[ImageJ/Fiji](http://fiji.sc/) plugin (Java 11, built with Maven) for automated
detection and analysis of cell migration, membrane protrusions, and associated
fluorescence intensity. It consumes two image stacks per cell (a cytoplasmic
channel used for segmentation and a "signal" channel) and produces
curvature/velocity/signal maps, per-cell and per-protrusion metrics, and
visualisations.

The heavy lifting (segmentation, curvature analysis, trajectory analysis,
Bio-Formats I/O, `UserVariables` parameters) is done by three external
dependencies pulled from JitPack (`IAClassLibrary`, `TrackerLibrary`,
`AdaptDataProcessing` — all under `com.github.djpbarry`, pinned to specific git
commit hashes in `pom.xml`). ADAPT itself is mostly orchestration and I/O glue
around those libraries. When searching for how a step actually works, look at
these libraries first, not this repo.

## Build / test / run

- **Build + verify:** `mvnw verify` (Maven wrapper; on Windows use `mvnw.cmd`).
  - Requires **JDK 17+** on `JAVA_HOME` (Temurin 17 at
    `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` is the local
    setup). Compile target is **Java 11** (parent `pom-scijava:45.1.0`).
  - Dependencies resolve from Maven Central (`central`, declared first), SciJava
    (`maven.scijava.org`), and JitPack (`com.github.djpbarry:*`); no auth needed.
  - `mvn_settings.xml` (GitHub Packages + PAT) is **vestigial** and slated for
    removal — see `DEVELOPMENT_PLAN.md`.
  - `IAClassLibrary` depends on `sc.fiji:TrackMate` transitively; ADAPT pins
    `TrackMate:7.14.0` in `dependencyManagement` as a stopgap (TrackMate 8 needs
    Java 21). See Phase D5 of the plan for the Java 21 + TrackMate 8 goal.
- **CI:** `.github/workflows/maven.yml` runs
  `mvn --batch-mode --update-snapshots -Dinternal.repo.password="$PAT" --settings
  mvn_settings.xml verify` on `ubuntu-latest` with JDK 11 (AdoptOpenJDK). This
  still references the vestigial `mvn_settings.xml`/`PAT` and must be updated to
  use the wrapper + JDK 17 + Java 11 target.
- **Packaging:** the parent POM is `org.scijava:pom-scijava:45.1.0`; the
  `maven-dependency-plugin` copies all dependencies into `target/` on `package`.
- **Run/debug:** `main-class` is `net.calm.adapt.Adapt.Main`. Its `main()` calls
  `Analyse_Movie.initialise()` then `run(null)`, which is a debug path that opens
  images via dialog (`IJ.openImage()`), not the normal plugin entry point. Under
  Fiji the real entry points are the three plugins declared in
  `src/main/resources/plugins.config`.
- **No unit tests exist.** There is no `src/test`, no test framework configured,
  and no lint/format tooling. Verification is compilation plus manual runs in
  Fiji. Test data ships as `test_data/ADAPT_Test_Data.zip`.

## Entry points / plugin registration

Fiji discovers commands via `src/main/resources/plugins.config`, not annotations:

```
Plugins>Adapt, "Analyse Movie", net.calm.adapt.Adapt.Analyse_Movie
Plugins>Adapt, "Batch Analysis", net.calm.adapt.Adapt.Analyse_Batch
Plugins>Adapt, "Bleb Data Analysis", net.calm.adapt.Adapt.Bleb_Data_Analysis
```

These three classes implement ImageJ's `ij.plugin.PlugIn` (`run(String arg)`).
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
  - `Bleb_Data_Analysis` — third plugin; thin wrapper over
    `AdaptDataProcessing.DataFileAverager` for post-hoc averaging.
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
  wired into CSV writers and `DataFileAverager`; changing a string here changes
  output schema.
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
- **Lots of commented-out code** throughout (`Main.java` is almost entirely
  commented experiments; many `//` blocks in `Analyse_Movie`, `BlebAnalyser`,
  `RunnableOutputGenerator`). Treat existing commented lines as historical
  context, not spec — they are frequently stale.
- **License headers are inconsistent** — some files carry the old
  `netbeans`-generated "To change this license header" stub; others have the GPL
  header. Don't try to normalize them unless asked; the project's `pom.xml`
  declares the Simplified BSD license.
- **No `.gitignore`** is present in the repo (build artifacts like `target/`
  or `.idea/` are not excluded).

## Domain terms

- `MorphMap` (external): the upscaled, boundary-length-normalised
  (power-of-two) map storing velocity/signal/curvature per cell over time; a
  core abstraction passed between library and this repo.
- `UserVariables` (external): a bean holding every analysis parameter; cloned
  via `(UserVariables) uv.clone()` when recursing into protrusion analysis.
- `protrusions` vs `blebs`: protrusion analysis can run in two modes — velocity-
  based bleb detection (`isBlebDetect()`) or morphology-based protrusion
  detection (`findProtrusionsBasedOnMorph()`), selected via `UserVariables`.
