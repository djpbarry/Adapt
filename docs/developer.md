# Developer Guide

## Build

ADAPT is a Java 21 Maven project. Build and test with the wrapper:

```bash
./mvnw verify          # Linux / macOS
mvnw.cmd verify        # Windows
```

Requirements:

- **JDK 21+** on `JAVA_HOME`.
- Dependencies resolve from Maven Central, the SciJava repository, and JitPack
  (`com.github.djpbarry:*`) — no authentication.

The heavy lifting lives in two external JitPack dependencies
(`IAClassLibrary` and `TrackerLibrary`), so when tracing how a step works, look
at those libraries first.

## Entry points

Fiji discovers commands via `src/main/resources/plugins.config`:

```
Plugins>Adapt, "Analyse Movie", net.calm.adapt.adapt.Analyse_Movie
Plugins>Adapt, "Batch Analysis", net.calm.adapt.adapt.Analyse_Batch
```

Both classes implement `ij.plugin.PlugIn`. To add a command, add a line to
`plugins.config` **and** implement `PlugIn`.

## Architecture

Package `net.calm.adapt` has four sub-packages:

- **`adapt/`** — core plugin logic and domain classes (`Analyse_Movie`,
  `Analyse_Batch`, `Bleb`, `CurveMapAnalyser`, `FluorescenceDistAnalyser`,
  `StaticVariables`, …).
- **`output/`** — result writing (`MultiThreadedOutputGenerator`,
  `RunnableOutputGenerator`, `CsvWriter`, `ParameterWriter`,
  `CellTableAccumulator`).
- **`visualisation/`** — per-frame overlays assembled into multi-page TIFF stacks
  and `labels.zip`.
- **`ui/`** — the hand-managed `GridBagLayout` dialog (`GUI`).

Control flow:

```
Analyse_Movie.run() / Analyse_Batch.run()
  → analyse() (non-modal GUI + setOnRun callback)
  → runPipeline() → MultiThreadedOutputGenerator
  → (optional) MultiThreadedVisualisationGenerator
```

## Concurrency

Two patterns coexist, each with a distinct job:

- `NotificationThread` (single background thread) — the GUI **preview** path.
- `MultiThreadedProcess` / `RunnableProcess` (executor pool) — the parallel
  output/visualisation generation, one task per cell or frame.

Do not conflate them.

## Conventions

- `StaticVariables` is the single source of truth for GUI labels and output
  column headings.
- `pom.xml` `<version>` is a plain `X.Y.Z` (no `-SNAPSHOT`); bump it after every
  change. Releases are tagged `vX.Y.Z`.
- `readParams()` loads a versioned `params.json` (Jackson); positional
  `params.csv` files are rejected.
- License is GPL-3.0.

## Local Fiji testing

See `bin/` for staging/launch/smoke-test scripts:

- `bin/install-to-fiji.cmd` — build and install the plugin into a local Fiji.
- `bin/run-fiji.cmd` — launch Fiji.
- `bin/smoke-test-fiji.cmd` — headless check that the plugin classes load.
