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
dependencies pulled from JitPack (`IAClassLibrary` `v2.0.23` and `TrackerLibrary`
`v4.0.8`, both under `com.github.djpbarry`, pinned to tagged releases). ADAPT
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
- **Run/debug:** `main-class` is `net.calm.adapt.adapt.Main`. Its `main()` calls
  `Analyse_Movie.initialise()` then `run(null)`, a debug path that opens images
  via dialog (`IJ.openImage()`). This path is **stale** — single-movie
  `analyse()` now reads the active hyperstack window instead of the stacks
  `initialise()` set — and is left for the H4 dead-code sweep. Under Fiji the
  real entry points are the two plugins declared in
  `src/main/resources/plugins.config`.
- **Local Fiji testing:** `bin/install-to-fiji.cmd`, `bin/run-fiji.cmd`, and
  `bin/smoke-test-fiji.cmd` stage/launch/test the plugin in a local Fiji.
  Machine-specific `FIJI_DIR`/`JAVA_HOME` live in `bin/local-env.cmd`
  (gitignored; template is `bin/local-env.cmd.example`).
- **JUnit 5 unit tests** exist (13 tests across `Analyse_BatchReadParamsTest`,
  `CurveMapAnalyserTest`, `FluorescenceDistAnalyserTest`, and
  `CellTableAccumulatorTest`), but there is **no lint/format tooling**.
  Verification is `mvn verify` (compile + tests) plus manual runs in Fiji. Test
  data ships as extracted `.ome.tiff` stacks under `test_data/` (Git
  LFS-tracked); ADAPT output trees are gitignored.
- **Output baseline (H5):** `bin/compute-baseline.cmd` and
  `bin/verify-baseline.cmd` SHA-256 the deterministic text outputs (UTF-8 CSVs
  + `parameters.json`) against `test_data/baselines/*.sha256`.

## Entry points / plugin registration

Fiji discovers commands via `src/main/resources/plugins.config`, not annotations:

```
Plugins>Adapt, "Analyse Movie", net.calm.adapt.adapt.Analyse_Movie
Plugins>Adapt, "Batch Analysis", net.calm.adapt.adapt.Analyse_Batch
```

These two classes implement ImageJ's `ij.plugin.PlugIn` (`run(String arg)`).
To add a new plugin command, add a line to `plugins.config` AND the class must
implement `PlugIn`.

## Architecture and data flow

Package `net.calm.adapt` is split into four sub-packages, matching the class
prefixes you'll see in imports:

- **`adapt/`** — core plugin logic and domain classes.
  - `Analyse_Movie` — main single-movie analysis pipeline. `run()` prompts for
    an output dir, then `analyse()` does segmentation → morphology → velocity/
    signal map building → protrusion analysis, and finally runs
    `TrajectoryAnalysis` and writes `parameters.json` + `README.md` via
    `net.calm.adapt.output.ParameterWriter`. Note: it extends
    `NotificationThread` (not `Thread`).
  - `Analyse_Batch` — extends `Analyse_Movie`; iterates over a directory of
    image files, reusing the same `analyse()` per file. Also contains
    `readParams()` which loads a versioned `params.json` (Jackson) and rejects
    unknown schema versions with a clear message.
  - Domain/helper classes: `Bleb` (extends `Protrusion`), `BlebAnalyser`,
    `CurveMapAnalyser`, `FluorescenceDistAnalyser`,
    `RegionFluorescenceQuantifier`, `Protrusion`, `StaticVariables` (all GUI
    label strings + output column headings + shared `DecimalFormat`s),
    `NotificationThread`/`TaskListener` (observer pattern for thread completion).
- **`output/`** — result writing.
  - `MultiThreadedOutputGenerator extends MultiThreadedProcess`; submits one
    `RunnableOutputGenerator` (a `RunnableProcess`) per cell to a
    `fixedThreadPool(availableProcessors())`. Per cell it builds morphology/
    velocity/signal maps and, if protrusion analysis is on, recurses by
    constructing a *new* `Analyse_Movie` in `protMode`.
  - `RunnableOutputGenerator.buildOutput()` is the longest, most intricate
    method; it writes CSV/visual outputs and handles protrusion/bleb detection.
  - `CsvWriter` writes UTF-8 CSV; `ParameterWriter` writes the `parameters.json`
    + `README.md` manifest (replacing the external `PropertyWriter`);
    `CellTableAccumulator` merges per-cell rows into the tidy `tables/`
    `velocity.csv`/`boundary.csv`/`blebs.csv` with `cell_id`/`bleb_id` columns.
- **`visualisation/`** — `MultiThreadedVisualisationGenerator` + per-frame
  `RunnableVisualisationGenerator`; renders velocity/curvature overlays into
  per-frame `FloatProcessor`s (one task per frame), then assembles them into the
  multi-page `images/velocity_visualisation.tif` + `curvature_visualisation.tif`
  stacks via `BioFormatsImageWriter.saveStack` and writes `labels.zip`.
- **`ui/`** — `GUI` (a `javax.swing.JDialog` with a hand-managed GridBagLayout in
  `GUI.java`; the NetBeans `GUI.form` was removed in M5 step 3). Holds a per-instance
  `UserVariables` (`UV`) populated when the user clicks Run; `Analyse_Movie`
  and `Analyse_Batch` read it back via `gui.getUv()` and start analysis through
  the `gui.setOnRun(Runnable)` callback (non-modal). In single-movie mode it
  exposes cytosol/signal channel dropdowns over the active hyperstack (they may
  be the same channel); `Analyse_Movie.extractChannel(...)` turns a selection
  into a 2D+time stack. Segmentation previews are rendered as `Overlay`s on the
  original `ImageWindow`s (not embedded in the dialog).

Control flow: `Analyse_Movie.run()` / `Analyse_Batch.run()` → `analyse()`
(non-modal GUI + `setOnRun` callback; single-movie reads the active hyperstack
and extracts the selected cytosol/signal channels) → `runPipeline()`/
`finishAnalysis()` → `MultiThreadedOutputGenerator` → (optional)
`MultiThreadedVisualisationGenerator`.

## Concurrency model

- Two patterns coexist, each with a distinct job. Do not conflate them.
  - `NotificationThread` (in `adapt/`) is a single-background-thread
    observer/callback wrapper: subclasses implement `doWork()`, and `run()`
    calls `doWork()` then notifies registered `TaskListener`s. It is used only
    for the GUI **preview** path — `GUI` creates a `new Analyse_Movie(...)`,
    calls `preparePreview(...)`, adds a `TaskListener`, and starts it via
    `new Thread(previewAnalyser).start()`.
  - `MultiThreadedProcess`/`RunnableProcess` (external IAClassLibrary) is the
    executor-pool pattern for parallel work. `MultiThreadedOutputGenerator`
    submits one `RunnableOutputGenerator` per cell and
    `MultiThreadedVisualisationGenerator` submits one
    `RunnableVisualisationGenerator` per frame; both finish with
    `terminate(msg)`.
- Thread pools are always sized to `Runtime.getRuntime().availableProcessors()`.
- Consolidation of the two patterns is intentionally **not** done: a
  single-thread preview callback and a parallel executor pool serve different
  needs, and merging them would obscure both.
- **Cancellation is cooperative** (M5 step 8): `net.calm.adapt.ui.ProgressMonitor`
  shows a non-modal progress bar + Cancel; workers poll an `AtomicBoolean`
  `isCancelled()` flag at checkpoints rather than being interrupted
  (`shutdownNow()`/`Thread.interrupt()` broke Swing ops). `Analyse_Movie` and
  `Analyse_Batch` propagate the flag to abort the pipeline/batch loop.

## Conventions and gotchas

- **Package names map to directories with lowercase** (`adapt`, `output`,
  `visualisation`, `ui`); keep it consistent.
- **`StaticVariables` is the single source of truth** for GUI labels and output
  column names (e.g. `TIME`, `VELOCITY`, `TOTAL_SIGNAL`). Column headings are
  wired into CSV writers; changing a string here changes output schema.
- **Versioning:** the reported title is `Adapt_v<version>` where `<version>` is
  read at runtime from `project.properties`, which Maven filters from
  `${project.version}` (`pom.xml` sets `<filtering>true</filtering>`); do not edit
  `project.properties` by hand. **`pom.xml` `<version>` is a plain `X.Y.Z` with no
  `-SNAPSHOT` suffix (sibling convention — see `IAClassLibrary`'s `development`
  branch), and releases are tagged `vX.Y.Z` via `maven-release-plugin`.** Bump the
  `pom.xml` version as development progresses. **Bump the `pom.xml` `<version>`
  (`X.Y.Z`, no `-SNAPSHOT`) after each change** — this is the sibling convention
  in `IAClassLibrary` and `TrackerLibrary` and must be followed here too, so the
  runtime title and jar name advance with every committed change.
- **`readParams()`** in `Analyse_Batch` loads `params.json` (a versioned JSON
  object, `version: 1`) via Jackson and applies each field through typed
  `reqBool`/`reqInt`/`reqDouble`/`reqText` helpers. Old positional `params.csv`
  files are rejected (see `src/main/resources/params.example.md` for the
  CSV→JSON migration).
- **Directory delimiter:** code uses `GenUtils.getDelimiter()` (in
  `Analyse_Movie`) rather than `File.separator` in some paths; `Analyse_Batch`
  also does `directory.getAbsolutePath() + delimiter + ".."` for the parent.
- **Statics on `GUI`:** the `VERSION`-style labels are static/shared;
  `UserVariables UV` is now a per-instance field (the static singleton was removed
  in M4 step 7). The GUI dialog is non-modal and starts analysis via a
  `setOnRun` callback (M5 step 1).
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
