# ADAPT Development Plan

> **Keep this plan and `REVISION_LOG.md` in sync:** after any repo change,
> review and update one or both in the same pass (mark phases/milestones done,
> record decisions, log lessons).

This plan outlines a multi-phase effort to (a) make ADAPT more robust and
maintainable, (b) improve the user experience, and (c) overhaul the
documentation (migrating from the GitHub wiki to ReadTheDocs). It is grounded in
the current state of the codebase as of the plan's writing.

## Maintenance convention

**Every time anything in the codebase is changed, update this
`DEVELOPMENT_PLAN.md` and `REVISION_LOG.md` in the same pass.** Mark phases,
milestones, and decisions as done or deviated in the plan, and add a dated
narrative entry to the revision log. The plan and the log are the single source
of truth for what has actually been done; they must not drift stale behind the
code.

## Current state (context for the plan)

- ADAPT is a Fiji/ImageJ plugin (`net.calm.adapt`) that analyses cell migration,
  membrane protrusions, and correlated fluorescence intensity.
- The heavy lifting — segmentation (`RegionGrower`), curvature computation,
  particle/trajectory tracking (`TrajectoryAnalysis`, `TrajectoryBuilder`),
  Bio-Formats I/O, and CSV writing — lives in two external libraries pulled
  from JitPack: `IAClassLibrary` `2.0.22` and `TrackerLibrary` `v4.0.8` (tagged
  releases). ADAPT also contains substantial in-repo domain logic
  (protrusion/bleb/fluorescence analysis and cell-trajectory extraction), so it
  is not purely orchestration/glue.
- The build is Maven 3 with `org.scijava:pom-scijava:45.1.0` as parent, targeting
  Java 21 (build JDK is Temurin 21). A Maven wrapper (`mvnw`/`mvnw.cmd`, pinned
  to 3.9.16) is committed; the vestigial `mvn_settings.xml` (GitHub Packages)
  has been removed.
- **Versioning:** `pom.xml` `<version>` is a plain `X.Y.Z` with **no `-SNAPSHOT`**
  suffix (sibling convention — see `IAClassLibrary`'s `development` branch), and
  releases are tagged `vX.Y.Z` via `maven-release-plugin`. Bump the version as
  development progresses.
- JUnit 5 unit tests exist (12 across three classes); there is **no lint/format
  tooling**. A `.gitignore` (and `.gitattributes`) have been added.
- The GUI is a NetBeans-generated `JDialog` (`ui/GUI.java` + `ui/GUI.form`), with
  parameters held in a single **static** `UserVariables` instance.
- Documentation currently lives in the GitHub wiki and a short `README.md`, with
  screenshots under `content/` and test data (`.ome.tiff` inputs, Git LFS) under
  `test_data/`.

---

## Phase A — Robustness & maintainability

### A1. Modernise the build and CI

1. **Pin and commit a reproducible toolchain** — add a Maven wrapper
   (`mvnw[.cmd]`) so builds don't depend on a system Maven of an unknown version.
2. **Confirm the JDK target** — ✔ Decision 3 was reversed to **Java 21**
   (2026-09-27): `scijava.jvm.version=21` set, `maven.compiler.release=21`
   verified. See Decision 3 and Phase D5.
3. **Harden CI** (`.github/workflows/maven.yml`):
   - Add a matrix over supported JDKs, or at least pin the exact one used.
   - Add caching for Maven dependencies to speed up runs.
   - Split into distinct jobs: `build`, `test` (future), `docs` (see Phase C).
   - ✔ `actions/setup-java@v4` → `@v5` and `actions/checkout@v4` → `@v5` (done in
     M1). `ubuntu-latest` migrates to Ubuntu 26 in Oct 2026 — re-verify then.
4. **Add a `.gitignore`** covering `target/`, `.idea/`, `*.iml`, and OS files.

### A2. Fix the dependency pinning problem

1. ✔ Done (2026-09-27): `IAClassLibrary` → `v2.0.1`, `TrackerLibrary` →
   `v4.0.2` (both tagged, Javadoc-published). ✔ `AdaptDataProcessing` removed
   (deprecated/obsolete — its README recommends Python); the `Bleb_Data_Analysis`
   plugin was retired with it.

### A3. Introduce tests (the single biggest maintainability win)

1. ✔ **Add a test framework** — JUnit 5 (`junit-jupiter-api` + `engine`, version
   managed by `pom-scijava`) added; the parent already wires `maven-surefire-plugin`
   (JUnit Platform) + JaCoCo into `mvn verify`.
2. **Start with the highest-value, lowest-cost targets** — the pure-logic,
   static-method classes already extracted:
   - ✔ `CurveMapAnalyser` — `calcScaledCurveRange` + `isLocalCurvatureExtreme`
     (curvature-extrema detection) tested.
   - ✔ `FluorescenceDistAnalyser` — `calcGlcmStats` (GLCM contrast/energy/
     homogeneity) extracted to a `static` helper and tested.
   - `BlebAnalyser` (boundary/anchoring math) — still deferred (touches
     `ImageProcessor`/`MorphMap`).
   - `Analyse_Batch.readParams()` (JSON params parsing) — ✔ done (2026-10-03):
     replaced the positional CSV parser with a versioned JSON parser (Decision 5).
3. **Add golden-file tests** for CSV output — *deferred*: needs the headless Fiji
   pipeline; do it after the B2a output-structure simplification and H5 baseline
   (still targets the schema-drift risk flagged in `AGENTS.md`).
4. **Make code testable first** — started: `isLocalCurvatureExtreme` made
   package-private, `calcGlcmStats` extracted to a `static` helper. Continue in A4.

### A4. Refactor for clarity and testability

Targets, priority-ordered:

1. **`Analyse_Movie.analyse()`** and **`RunnableOutputGenerator.buildOutput()`**
   — these are the largest, most intertwined methods. Decompose into
   single-responsibility private methods (segmentation, map building, protrusion
   analysis, output writing) and move pure math into package-private/static
   helpers. ✔ `analyse()` decomposed (2026-10-03, M4 step 3) into
   `createOutputDirectories()`, `segmentCells()`, and `generateOutputs()`;
   `buildOutput()` remains for M4 step 5.
2. **`Analyse_Batch.readParams()`** — ✔ per Decision 5 (done 2026-10-03):
   replaced the brittle positional `Scanner` + `br.readLine()` parsing with JSON
   (validated via Jackson), including a `version` field so old files are detected
   and rejected with a clear message. `params.example.json` +
   `params.example.md` (migration note) ship under `src/main/resources/`.
3. **Reduce mutable static/config state** — `GUI.UV` is a genuine static
   singleton (returned by `GUI.getUv()`), so all runs share one `UserVariables`
   instance. `Analyse_Movie` also carries many `protected` *instance* fields that
   are mutated through the class hierarchy and the protrusion-analysis recursion,
   making state hard to reason about. Introduce an explicit per-run "context"
   object passed down instead of relying on statics/field mutation.
4. **Normalise the two concurrency abstractions** — `NotificationThread` (in
   repo) and `MultiThreadedProcess`/`RunnableProcess` (external) are unrelated.
   Consolidate on one, or document/enforce which to use where.
5. **Remove dead code** — ✔ done in M1: the commented-out experiments in
   `Main.java`, `Analyse_Movie`, `BlebAnalyser`, and `RunnableOutputGenerator`
   were stripped (recoverable from git).
6. **Normalise license headers** — ✔ done in M1: `pom.xml` corrected to
   GPL-3.0 (`license.licenseName=gpl_v3`, `license.copyrightOwners=David Barry`),
   the six NetBeans stubs replaced with the GPL header.

### A5. Add static analysis and formatting

1. Add a formatter (e.g. Spotless with a standard Java style) and a linter
   (SpotBugs / PMD as appropriate) wired into `mvn verify`.
2. ✔ Done (2026-10-03): renamed `Adapt`, `Output`, `Visualisation` to lowercase
   (`adapt`, `output`, `visualisation`; `ui` unchanged); updated all imports,
   `plugins.config`, and `pom.xml` `main-class`.

### A6. Error handling & user-facing failure modes

1. ✔ done in M1: replaced the bare `catch (Exception e) { IJ.log(e); }` and
   empty `catch` blocks (e.g. the version-property load in `run()`) with
   structured, actionable messages.
2. Centralise logging on `IJ.log`/`IJ.error` with a small wrapper so severity and
   context are consistent.
3. Add pre-flight validation of input images (size/type/time-series) — the repo
   already began this (`5445fde`, `b634de7`), so continue it comprehensively.

---

## Phase B — User-friendliness

### B1. Known bug to fix — non-deterministic `labels.zip` ROI order

Issue #2 ("ROIs in random order"): `MultiThreadedVisualisationGenerator.run()`
submits one thread per frame, all sharing a single unsynchronized `Overlay`
`labels`. Concurrent `labels.add(...)` in `RunnableVisualisationGenerator.run()`
(line 105) produces non-deterministic ROI ordering in the saved `labels.zip`.
Measurements are unaffected (cell index is baked into each ROI's text; per-cell
CSVs are written independently in cell order), but the fix is still warranted:
collect labels into per-frame slots and assemble the overlay in deterministic
frame order before saving. Include an order-assurance assertion.

### Known bug to fix — curvature extrema detection (unsigned curvature)

`CurveMapAnalyser.isLocalCurvatureExtreme(..., minima=true, threshold=...)`
flags a curvature minimum only when `C0 < -threshold` (i.e. negative curvature).
With `Min Curvature Threshold = 0`, this requires `C0 < 0`. But the library's
`CurveAnalyser.calculateMengerCurvature()` returns **unsigned** Menger curvature
(`1/R`, always ≥ 0), so no point ever qualifies and the extrema list is empty.
This is why the yellow curvature-extrema markers no longer appear in the preview
(and why `cellData.getCurvatureMinima()` is empty).

Fix options: (a) make `CurveAnalyser.calcCurvature()` return signed curvature
(library change), or (b) rework `isLocalCurvatureExtreme()` /
`findAllCurvatureExtrema()` to use the correct signed convention. Bleb detection
itself is unaffected (it uses velocity ROIs, not curvature).

### B1. Rework the GUI

1. **Move previews to `Overlay`s, keep input images untouched** — the current
   dialog embeds preview segmentations directly into the image pixel data, which
   is destructive and blocks interaction with the source `ImageWindow`. Instead,
   keep the inputs in their original `ImageWindow` containers and render preview
   segmentations as ImageJ `Overlay`s / `RoiManager` ROIs, refreshed live as
   parameters change. This mirrors TrackMate and GIANI and makes the preview
   non-destructive, live, and reversible. This is a dedicated reconfiguration
   and the centrepiece of M5 (M4 steps 7–8 are already done, so it lands on top
   of a non-static `GUI.UV` and the documented concurrency model).
2. **Replace the NetBeans `.form` coupling** — the hand-versus-generator split
   between `GUI.java` and `GUI.form` is risky to edit. Migrate to a hand-managed
   layout (GridBag/GroupLayout written by hand) so the UI is version-controllable
   and diffable.
3. **Eliminate the static `UserVariables` singleton** — pass a `UserVariables`
   instance explicitly; this fixes a class of bugs from stale/shared state across
   sessions. *(Done in M4 step 7.)*
4. **Group parameters into collapsible sections** mirroring the *Simple /
   Advanced / Protrusions* screenshots already in `content/`, and add tooltips
   or inline help for every parameter (wording drawn from `StaticVariables`).
5. **Add validation and sane defaults** at the UI layer: numeric ranges, required
   fields, and a "load/save parameter preset" feature (the raw material exists in
   `Analyse_Batch.readParams()`).
6. **Add cancellation & progress** — the GUI has a Cancel button, but it only
   `dispose()`s the setup dialog; the background analysis threads
   (`MultiThreaded*` generators) have no cancellation path. Wire the existing
   `NotificationThread`/`TaskListener` and `MultiThreadedProcess` mechanisms to a
   progress dialog whose Cancel actually interrupts the running analysis.

#### B1 execution plan (M5)

Ordered by dependency and risk; each step bumps the version, runs `mvn test`,
and is verified interactively in Fiji. GUI-only steps must not move the H5
baseline.

1. **Non-modal dialog + run lifecycle.** Make `GUI` non-modal and change
   `Analyse_Movie`/`Analyse_Batch` to start analysis on an explicit "Run" event
   rather than blocking on `setVisible(true)`.
   — **done (2026-10-05): `GUI` is non-modal and exposes `setOnRun(Runnable)`;
   `Analyse_Movie.analyse()` split into `runPipeline()` + `finishAnalysis()`;
   `Analyse_Batch` uses a `runBatch()` loop started from the callback.**
2. **Overlay-based preview.** Replace the embedded `cytoImp`/`sigImp` preview
   canvases with `Overlay`/`RoiManager` rendering on the original `ImageWindow`s
   (input stacks stay untouched); refresh the overlay on the EDT.
   — **done (2026-10-05): `Analyse_Movie.generatePreview()` now builds a
   per-slice `Overlay` (boundary/centre/cortex/bleb ROIs); `GUI` sets it on the
   original `ImagePlus`es via `setOverlay()`/`updateAndDraw()`. Batch preview is
   a temporary regression (deferred until the layout rewrite).**
3. **Hand-managed layout.** Rewrite `GUI.java` programmatically (GridBagLayout)
   and drop the `GUI.form` coupling.
4. **Collapsible sections + tooltips.** Group controls into Simple/Advanced/
   Protrusions; add tooltips from `StaticVariables`; refresh `content/UI_*.PNG`.
5. **Validation, defaults, presets.** Numeric ranges, required fields, and
   save/load presets reusing `params.json`.
6. **Cancellation & progress.** Wire a progress dialog whose Cancel interrupts
   the `MultiThreaded*` executors.

### B2. Onboarding & output UX

1. **First-run / help flow** — link the tutorial and docs directly from the GUI.
2. **Output organisation** — *simplify* the on-disk structure (revised from
   "preserve" after the 2026-10-02 output review — see `REVISION_LOG.md`): tidy
   long-format CSVs under one `tables/` dir, multi-page TIFF stacks + maps under
   `images/`, JSON params + a schema manifest, deterministic ROIs. Must stay
   importable in Fiji (TIFF stacks + ROI overlay) *and* in Python (UTF-8 tidy
   CSVs + JSON). The `Output_Folder_Structure.PNG` screenshot must be regenerated.
3. **Consistent exit messaging** — unify the `IJ.showStatus`/`IJ.log` completion
   messages and always report elapsed time and output location.

### B2a. Output structure — simplified target

Proposed on-disk layout (from the 2026-10-02 review — see `REVISION_LOG.md`). The
guiding principle is three disjoint concerns — **tables** (measurements),
**images** (pixel data / visualisations), **metadata** (params + manifest) — with
every table in tidy long-format (one observation per row, a `cell_id` column on
every row-bearing table):

```
<name>_Output/
├── parameters.json          # run params (machine-readable; replaces properties.xml)
├── README.md                # schema + file manifest (in-product docs)
├── tables/                  # all tabular data, tidy long-format, UTF-8
│   ├── cells.csv            # cell_id, ... (one row per cell)
│   ├── trajectories.csv     # cell_id, frame, time_s, x_um, y_um
│   ├── morphology.csv       # cell_id, frame, area, ... (ImageJ measures)
│   ├── velocity.csv         # cell_id, frame, %_protruding, ...
│   ├── fluorescence.csv     # cell_id, frame, contrast, ... (GLCM)
│   ├── boundary.csv         # cell_id, frame, x, y
│   └── blebs.csv            # cell_id, bleb_id, time_s, v_um_s, signal, ...
├── images/                  # maps + visualisations, Fiji-friendly
│   ├── cell_000/
│   │   ├── signal_map.tif
│   │   ├── curvature_map.tif
│   │   ├── velocity_map.tif
│   │   ├── change_in_signal_map.tif
│   │   ├── velocity_visualisation.tif    # one multi-page stack (was NNN.tiff)
│   │   ├── curvature_visualisation.tif   # one multi-page stack
│   │   └── bleb_detection.tif            # bleb mode
│   └── …
└── labels.zip               # ImageJ ROI overlay (deterministic order)
```

Old → new mapping:

| Old | New |
|---|---|
| `Population_Data_Output/*.csv` | `tables/*.csv` (add `cell_id` where missing) |
| `Individual_Cell_Data_Output/0_Output/*.csv` | `tables/*.csv` (flatten across cells) |
| `Individual_Cell_Data_Output/0_Output/Bleb_Data_Files/bleb_data_N.csv` | `tables/blebs.csv` (add `bleb_id`) |
| `Individual_Cell_Data_Output/0_Output/*.tif` maps | `images/cell_000/*.tif` |
| `Visualisations_Output/Velocity_Visualisation/NNN.tiff` | `images/cell_000/velocity_visualisation.tif` |
| `Visualisations_Output/Curvature_Visualisation/NNN.tiff` | `images/cell_000/curvature_visualisation.tif` |
| `Visualisations_Output/labels.zip` | `labels.zip` (deterministic order) |
| `properties.xml` | `parameters.json` |

Checklist:

- [x] Write all CSVs as **UTF-8** (fix the Latin-1 `µ` in `Trajectories.csv`); prefer
      ASCII column names (`x_um`, `time_s`, `v_um_s`) to remove the encoding bug class.
      — UTF-8 + ASCII names done (2026-10-03).
- [x] Add `cell_id` (and `bleb_id`) columns; merge per-cell / per-bleb files into one
      tidy table each.
      — done (2026-10-03): `net.calm.adapt.output.CellTableAccumulator` merges the
      parallel per-cell writers into `tables/velocity.csv`, `tables/boundary.csv`,
      and `tables/blebs.csv` (with `cell_id`/`bleb_id`); population tables renamed to
      snake_case and `Cell_ID`/`Cell ID` → `cell_id`.
- [x] Emit time-series visualisations as **multi-page TIFF stacks** (one file, not
      `NNN.tiff`).
      — done (2026-10-05): `RunnableVisualisationGenerator` collects per-frame
      `FloatProcessor`s and `MultiThreadedVisualisationGenerator` writes
      `images/velocity_visualisation.tif` + `images/curvature_visualisation.tif`
      via `BioFormatsImageWriter.saveStack`.
- [x] Emit `parameters.json` and a `README.md` manifest.
      — done (2026-10-03): `net.calm.adapt.output.ParameterWriter` writes a sorted,
      pretty-printed UTF-8 `parameters.json` from the GUI `Properties` plus a short
      `README.md`; `Analyse_Movie.run()` no longer calls the external
      `PropertyWriter`.
- [x] Make `labels.zip` ROI order deterministic (B1 fix).
      — done (2026-10-05): `RunnableVisualisationGenerator` writes its `TextRoi`
      labels into per-frame `List<Roi>` slots and `MultiThreadedVisualisationGenerator`
      assembles the `Overlay` in frame/cell order after `terminate()` (also removes a
      cross-thread race on the shared `Overlay`).
- [x] Remove the spurious first line and trailing comma in `bleb_data_*.csv`.
      — done (2026-10-03); blank `Skew`/`Kurt` left as ImageJ "undefined" (`" "`).
- [x] Regenerate `content/Output_Folder_Structure.PNG` and update the docs.
      — done (2026-10-05): regenerated to reflect the new `tables/` + `images/`
      tree (programmatic text-tree render).

Trade-off: this is a breaking change, so the H5 output baseline and the
`Output_Folder_Structure.PNG` screenshot must be regenerated; do it before the
`v4.0.0` tag. Consider doing the writer changes alongside the M4
`RunnableOutputGenerator.buildOutput()` decomposition so the schema lands once.

### B3. Package & distribute more cleanly

1. Ensure the plugin is discoverable via a Fiji update site (this is the *de
   facto* distribution channel for ImageJ plugins) so users get updates.
2. Version under semantic versioning and surface the version prominently (today
   the version is injected into `project.properties` from `pom.xml` at build time).

---

## Phase C — Documentation (GitHub wiki → ReadTheDocs)

### C1. Choose and scaffold the docs toolchain

1. Adopt **Sphinx + MyST (Markdown)** hosted on **ReadTheDocs**, keeping the
   source under `docs/` in this repo so docs and code version together.
2. Add an RTD build job to CI and a `readthedocs.yaml` config.
3. Redirect the GitHub wiki to the new site (a stub page pointing to RTD), and
   add a prominent link in `README.md`.

### C2. Migrate and restructure content

Sections to establish (migrating wiki content → docs):

- **Getting Started**: installation via update site, test-data tutorial (linked
  YouTube video + `test_data/`).
- **User Guide**: explain each parameter (draw from `StaticVariables` labels);
  the Simple / Advanced / Protrusion Analysis tabs; the output folder structure.
- **Concepts / Method**: plain-English explanation of the analysis pipeline —
  segmentation, curvature/velocity/signal maps, protrusion vs bleb detection —
  with the DOI cited.
- **Troubleshooting / FAQ**.
- **Developer Guide**: build instructions (from `AGENTS.md`), architecture,
  entry points (`plugins.config`), and contribution workflow.

### C3. Automate doc quality

1. Add a `make linkcheck` / docs-build to CI to catch broken links and RST/MD
   errors.
2. Optionally generate API reference from Javadoc and reference it from RTD.
3. Keep screenshots (currently `content/*.png`) in `docs/_static/`, and update
   them as the GUI changes in Phase B.

---

## Phase D — TrackMate interoperability

TrackMate (`sc.fiji:TrackMate`, package `fiji.plugin.trackmate`) is the de facto
standard single-particle/cell tracker in the ImageJ ecosystem. ADAPT and
TrackMate are complementary rather than competing: TrackMate detects + links
objects into tracks; ADAPT analyses membrane/protrusion/bleb morphodynamics.
The goal is a practical interop bridge, not a wholesale replacement.

### D0. Correct mental model (avoids a common misconception)

ADAPT's `TrajectoryAnalysis` (called in `Analyse_Movie.run()`) is **not** the
tracker — it is a *downstream* analysis step that reads an already-written
`Trajectories.csv` and computes cell-migration statistics (speed, directionality,
persistence, etc.).

Cell identity across frames is established earlier, during segmentation:
`RegionGrower.initialiseROIs()`/`watershedRegions()` (external `IAClassLibrary`)
produce a per-frame `Region` for each seeded cell, associating cells across
frames by seed-following segmentation (there is no LAP-style linking pass).
`Analyse_Movie.generateCellTrajectories()` (in-repo) then extracts each cell's
centroid per frame and writes `Trajectories.csv`.

Separately, `CurveMapAnalyser` uses `TrackerLibrary`'s `TrajectoryBuilder` /
`ParticleTrajectory` to track **curvature minima** (protrusions/blebs) around the
cell boundary — that is protrusion-level tracking, distinct from cell migration.

This matters because it maps cleanly onto TrackMate's architecture:

| ADAPT concern | TrackMate counterpart | Overlap |
|---|---|---|
| Cell detection + frame-to-frame association | `SpotDetectorFactory` + `SpotTrackerFactory` (LAP) | ADAPT uses seed-following segmentation (no LAP pass); TrackMate's LAP is the field-standard alternative |
| `TrajectoryAnalysis` migration metrics | `TrackAnalyzer` modules (`TrackSpeedStatisticsAnalyzer`, `TrackDurationAnalyzer`, …) | Overlapping downstream analysis |
| Protrusion/bleb/membrane analysis | (nothing — this is ADAPT's unique value) | No overlap |

### D1. Stage 1 — interop bridge (low risk, high value, do first)

Interoperate at the **data boundary** rather than coupling codebases:

1. **Import TrackMate tracks into ADAPT.** Parse a TrackMate XML session
   (`TmXmlReader`, JDOM2, no runtime TrackMate dependency) and feed the resulting
   per-cell tracks into ADAPT's protrusion/bleb analysis. This lets users do
   detection + LAP tracking in TrackMate, then use ADAPT for the membrane
   dynamics. The conversion maps TrackMate `Spot`s (v7 `Spot` supports a `SpotRoi`
   contour) onto ADAPT's per-cell regions.
2. **Export ADAPT detections to TrackMate** (optional reverse direction): write
   ADAPT's cell centroids + contours as TrackMate XML/`Spot`s so users can track
   them inside TrackMate.

Because this is XML parse/write only, it sidesteps the Java-version conflict
(see D3 constraints) and needs no compile-time TrackMate dependency.

### D2. Stage 2 — ADAPT as a TrackMate module (long-term, high effort)

Ship ADAPT's membrane/protrusion/bleb analysis as a TrackMate `TrackAnalyzer`
(and optionally ADAPT segmentation as a `SpotDetectorFactory`), so ADAPT metrics
appear directly in TrackMate's tables/plots. This is the "first-class citizen in
the field standard" outcome, but it requires re-plumbing ADAPT's analysis to
consume TrackMate's `Spot`/`SpotRoi` + `TrackModel` instead of its internal
`CellData`/`MorphMap`. Defer until **after** M4's refactor produces a
model-agnostic analysis core.

### D3. Explicitly not recommended (near-term)

Pulling TrackMate's LAP tracker into ADAPT purely to replace ADAPT's homegrown
tracking. It is the highest-effort/lowest-value option: tracking is not ADAPT's
core value, and it forces an immediate dependency + Java-target decision.
TrackMate ≥ 8 requires **Java 21**, which ADAPT now targets (Decision 3
reversed), so the Java-version objection is moot — but the recommendation against
pulling LAP in as a *replacement for homegrown tracking* still stands.

### D4. Constraints to confirm before any compile-time dependency

1. **Java target** — TrackMate ≥ 8 needs Java 21; v7 targets older JVMs. Stage 1
   (XML-only) avoids this entirely.
2. **License** — ADAPT is GPL-3.0 (Decision 1). Verify TrackMate's license is
   compatible before adding `sc.fiji:TrackMate` as a compile dependency.

### D5. Current state & TrackMate 8 goal (discovered during build)

ADAPT depends on TrackMate **transitively** via `IAClassLibrary` (unversioned
`sc.fiji:TrackMate`), so TrackMate is already on ADAPT's runtime classpath — the
interop surface is closer than Phase D1 assumes.

✔ Done (2026-09-27): ADAPT now targets **Java 21** and resolves **TrackMate
8.0.0** via the parent (the earlier `TrackMate:7.14.0` stopgap pin was removed).
Remaining TrackMate work is the actual interop (Phase D1/D2), not version
alignment.

---

## Phase E — Segmentation interoperability (Cellpose / StarDist)

Cellpose and StarDist are the field-standard deep-learning cell-segmentation
tools; they target ADAPT's **segmentation** step (`RegionGrower` watershed) the
same way TrackMate (Phase D) targets its **tracking** step. ADAPT's core value is
downstream (membrane/protrusion/bleb), so these are complementary front-ends, not
replacements.

### E0. Mental model

ADAPT currently segments via `RegionGrower.initialiseROIs()`/`watershedRegions()`
(external `IAClassLibrary`), which requires a uniform cytoplasmic channel and
manual thresholding. Cellpose's `cyto`/`cyto2`/`cyto3` models are trained for
exactly this "cytoplasm" input, and StarDist is a Fiji-native (Java/TensorFlow)
2D alternative. Letting ADAPT consume externally-produced masks replaces its
weakest, most parameter-sensitive step.

### E1. Stage 1 — accept externally-segmented masks/ROIs (low risk, do first)

If a user supplies segmentation masks (e.g. cellpose output, or ROIs in the Fiji
RoiManager), ADAPT skips its `RegionGrower` watershed and builds `CellData` /
`Region` directly from the masks. This is format-based, needs no Python and no
compile-time dependency, and sidesteps the Java-version conflict entirely (like
TrackMate Stage 1).

### E2. Stage 2 — combined pipeline via TrackMate-Cellpose (zero ADAPT-side Python)

The `TrackMate-Cellpose` detector (`sc.fiji:TrackMate-Cellpose`) already runs
cellpose segmentation + LAP tracking inside TrackMate. Users can therefore run
`cellpose → TrackMate → ADAPT` and import the resulting tracks via Phase D1 —
full deep-learning segmentation and tracking with no Python managed by ADAPT.

### E3. Stage 3 — in-UI cellpose (optional, heavier)

Call cellpose directly from ADAPT's UI via the Fiji-Cellpose/Appose library
(`imglib2-cellpose`, package `net.imglib2.cellpose`). This adds a real
dependency and requires **Java 21**, reopening Decision 3 — so defer it unless
Stage 1/2 prove insufficient.

### E4. Constraints

1. **Runtime** — cellpose is Python + PyTorch, so it cannot be a Java Maven
   dependency; integrate via Fiji-Cellpose (Appose) or TrackMate-Cellpose
   (conda), or by accepting pre-produced masks.
2. **Java target** — `imglib2-cellpose` / Fiji-Cellpose require Java 21 (same
   tension as TrackMate v8). Stage 1 (mask import) avoids this.
3. **License** — cellpose *code* is BSD-3-Clause (compatible with ADAPT's
   GPL-3.0), but its **pretrained weights are CC-BY-NC (non-commercial)**. This
   only matters if ADAPT redistributed the weights, not if it merely invokes
   cellpose — but it is worth surfacing to commercial users.
4. **StarDist** is the Fiji-native alternative (Java/TensorFlow, no Python env),
   2D-only — recommend it for users who want a zero-setup segmentation front-end.

---

## Phase F — Track visualization/sharing export (inTRACKtive)

inTRACKtive (`royerlab/inTRACKtive`) is a browser-based tool (TypeScript +
Python, MIT license) for exploring and sharing already-computed cell-tracking
data. It is **downstream** of ADAPT, not a front-end into it: the pipeline is
`cellpose → TrackMate → ADAPT → inTRACKtive` (segment → track → analyse →
visualize/share). This is an output/export concern, lower priority than Phases
D/E and closer in spirit to the Phase B output-UX work.

### F0. Mental model

inTRACKtive is *not* a Fiji plugin and is *not* a detector/tracker/segmenter; it
consumes tracking data and renders it in the browser. It accepts CSV (in Ultrack
format: `track_id, t, z, y, x, parent_track_id`), Parquet, GEFF, or a napari
Tracks layer, converting to a Zarr bundle. No Java, Maven, or license coupling.

### F1. Stage 1 — export Ultrack-format CSV (do first)

Add an exporter that writes ADAPT's cell trajectories (the centroids produced by
`Analyse_Movie.generateCellTrajectories()`, already serialised to
`Trajectories.csv`) in the Ultrack CSV shape inTRACKtive consumes. Users then run
`intracktive convert`/`open` to view and share their ADAPT results in the
browser.

### F2. Stage 2 (optional) — GEFF export

Emit GEFF (General Exchange Format) as an alternative for tools that prefer it
over Ultrack CSV. Only if Ultrack CSV proves insufficient.

### F3. Constraints

1. **License** — MIT, fully compatible with ADAPT's GPL-3.
2. **No dependency** — data-export only; inTRACKtive is a separate Python/browser
   tool, so no Java-target or Maven impact.

---

## Phase G — Upstream dependency hygiene (do first)

> **Status (2026-09-28): complete.** `IAClassLibrary` (`v2.0.1`) and
> `TrackerLibrary` (`v4.0.2`) are modernised and tagged (GPL-3.0-or-later, Java
> 21, Maven wrapper, Javadoc published). `AdaptDataProcessing` was **removed**
> (deprecated/obsolete — its README recommends Python); the `Bleb_Data_Analysis`
> plugin and the inline `DataFileAverager` call were retired with it. See
> `REVISION_LOG.md`.

ADAPT's core logic actually lives in the two remaining JitPack dependencies
(`IAClassLibrary`, `TrackerLibrary`). They share ADAPT's exact problems, so they
must be addressed **before** (or in lockstep with) ADAPT's own modernization —
they block A2 / Decision 2 (pin-to-tags). `AdaptDataProcessing` was removed
(deprecated/obsolete).

### G0. Verified current state

| Library | License (pom / LICENSE) | Java | Tags | ADAPT pins |
|---|---|---|---|---|
| IAClassLibrary | BSD-2 (pom) | 8 | `v1.032` (Jul 2024) | `fe92f24c6e` (after tag, untagged) |
| TrackerLibrary | **BSD-2 (pom) / GPL-3 (LICENSE)** | 8 | none | `99584ec579` (untagged) |
| AdaptDataProcessing | **BSD-2 (pom) / GPL-3 (LICENSE)** | 8 | none | `95d31fcec8` (untagged) |

All three are single-maintainer, maintenance-mode, with no CI and no tests.

### G1. Fix license metadata (mirrors Decision 1)

`TrackerLibrary` and `AdaptDataProcessing` carry GPL-3 `LICENSE` files but declare
BSD-2 in their poms — the same contradiction ADAPT just resolved. Correct each
pom to GPL-3 (or make the `LICENSE` file match the intended BSD). `IAClassLibrary`
declares BSD-2; verify its `LICENSE` file agrees (BSD-2 is GPL-3-compatible, so
there is no ADAPT licensing blocker either way).

### G2. Tag all three (unblocks Decision 2)

- Tag each library with semver. `IAClassLibrary`'s pinned commit `fe92f24c6e`
  is *after* its only tag `v1.032` (and bumped the version to 1.0.37); tag the
  current `master` as the next release.
- Repoint the cross-dependency pins: `TrackerLibrary` and `AdaptDataProcessing`
  currently depend on `IAClassLibrary` at `37a1be016a`; update them to the new
  IAClassLibrary tag.

### G3. Resolve the TrackMate version web

`IAClassLibrary` → `sc.fiji:TrackMate` (unversioned, parent-managed);
`TrackerLibrary` → `TrackMate:7.10.0` (explicit); ADAPT pins `7.14.0`. Adopt a
single TrackMate version policy across all three + ADAPT (7.x for now; the
8.x / Java 21 move is coordinated later via Phase D5).

### G4. Update ADAPT's pins to tags

Once tagged, change ADAPT's three dependency versions from commit hashes to the
new tags (completing A2 / Decision 2).

### G5. Consider consolidation (revisit Decision 2)

One maintainer, three repos, no tests/CI, and cross-repo pin skew. If the
multi-repo overhead is too high, revisit "no vendoring" (Decision 2) and consider
merging the three libraries into one repo (or into ADAPT) to make dependency
management tractable.

---

## Phase H — Interactive Fiji run & smoke-test harness

ADAPT is a Fiji plugin: a green `mvn verify` only proves it compiles. From this
point on, every change must also be verified by running the plugin
**interactively inside Fiji** against the bundled test data. Headless
build/run/test is a necessary floor, not a substitute. This phase wires that
capability into the repo as a repeatable, scripted step rather than an ad-hoc
manual one.

### H0. Plugin framework decision (2026-09-27)

ADAPT **stays an ImageJ 1.x plugin** for now (`plugins.config` +
`ij.plugin.PlugIn`); the authoritative reference for this model is
https://imagej.net/develop/ij1-plugins. A modern SciJava reimplementation
(`@Plugin(type = Command.class)` + `@Parameter`, auto-GUI/scripting/headless) is
**noted as a future option, not planned** — see the beginner's guide
(https://imagej.net/develop/beginners-guide) and
https://imagej.net/develop/plugins. If it is ever revisited, the preferred route
is thin `Command` wrappers over the existing `PlugIn` core (after M4), not a full
rewrite: the analysis core lives in ImageJ-1.x-era external libraries and is not
worth re-plumbing.

### H1. Local Fiji staging & launch

Local Fiji (this machine) is at `C:\Users\barryd\fiji-latest-portable-nojava\Fiji`
— the **latest** Fiji, a **nojava** build (no bundled JRE), requiring **Java 21+**.
The jaunch launcher (`fiji-windows-x64.exe`) finds Java 21 via `JAVA_HOME` (no
bundled JRE or junction needed).

**Device-specific paths are not committed.** `Fiji` and `JAVA_HOME` live in
`bin/local-env.cmd` (gitignored; copy `bin/local-env.cmd.example`). The scripts
`call` it and fall back to a command-line argument. The paths above are the
maintainer's current machine.

Scripts committed under `bin/`:

- `bin/install-to-fiji.cmd [Fiji]` — `mvnw clean package` (clean first so stale
  versioned jars aren't copied), removes any old `adapt-*.jar`, copies
  `target/adapt-<version>.jar` into `Fiji/plugins/`, and copies the two sibling
  libraries (`IAClassLibrary`, `TrackerLibrary`) into `Fiji/jars/`.
- `bin/run-fiji.cmd [Fiji]` — launches the bundled `fiji-windows-x64.exe`.
- `bin/smoke-test-fiji.cmd [Fiji]` — headless check that the JDK runs ImageJ and
  the two ADAPT plugin classes (plus sibling deps) are loadable.

### H2. Interactive smoke test (per change, before merge)

A short checklist run against the `test_data/` inputs (`blebbing_cell`,
`migrating_cell`):

1. `Plugins>Adapt>Analyse Movie` — GUI opens, run completes, output tree written.
2. `Plugins>Adapt>Batch Analysis` — the `params.json` / directory flow completes.
3. Confirm the produced CSVs/TIFFs match the documented
   `Output_Folder_Structure` screenshot.

### H3. Make the harness visible & repeatable

- Record the smoke test as a script that can run once a Fiji install is present
  (headless `--headless` launch where possible), and at minimum document the
  manual gate in `AGENTS.md` and `README.md`.
- Track the outcome in `REVISION_LOG.md` alongside each change.

### H4. Entry-point hygiene (prerequisite)

The current `Main.main()` debug path opens images via `IJ.openImage()` dialogs —
it is *not* the real plugin entry point. Confirm the two `plugins.config`
commands are the authoritative interactive entry points, and either document the
`Main` path as debug-only or remove it in M4's dead-code sweep.

### H5. Output-baseline comparison (quantitative, deferred)

Beyond the visual check, establish a quantitative gate: run the pipeline on the
benchmark inputs under `test_data/` and compare every output file to the
stored outputs by **SHA-256**, with normalization for volatile content:

- strip the timestamp line in `*.properties`;
- sort the ROIs inside `labels.zip` (its ordering is non-deterministic — Issue #2);
- compare TIFF/PNG pixel payload rather than the raw file (to ignore metadata).

The CSVs are the strongest signal and should be byte-identical while the code is
behaviour-preserving.

**Baseline rule:** any change must reproduce the benchmark outputs *exactly*
until the modernised dependencies are swapped in — at which point output
differences are expected (upstream bug fixes) and must be reviewed deliberately,
not silently accepted.

---

## Suggested sequencing & milestones

1. **M1 — Foundations (low risk, high value):** `.gitignore`, Maven wrapper, CI
   hardening, license-header consistency, delete dead code. (Phase A1, A2, A4.5,
   A4.6, A6) — **done (2026-09-27).**
2. **M2 — Test harness:** JUnit 5 + a couple of unit tests + golden-file output
   test. (Phase A3) — **unit tests landed (2026-10-02); golden-file test deferred
   to H5.**
3. **M3 — Docs migration:** stand up Sphinx/RTD, migrate wiki content. (Phase C)
4. **M4 — Refactor core:** decompose `analyse()`/`buildOutput()`, replace
   `readParams()` with JSON, remove static state, rename packages. (Phase A4,
   A5.2) — **done (2026-10-05): all 9 steps complete (package rename, JSON
   `readParams()`, `analyse()`/`buildOutput()` decomposition, output anomalies,
   B2a output restructure, static `GUI.UV` removal, concurrency normalisation,
   H5 baseline).**
5. **M5 — GUI & UX:** non-modal dialog, overlay-based non-destructive previews,
   hand-managed layout, parameter presets, progress/cancel. (Phase B1 — see the
   "B1 execution plan (M5)" above.)
6. **M6 — Distribution:** update site, semver, in-product help links. (Phase B3)
7. **M7 — TrackMate interop:** Stage-1 XML import/export bridge (Phase D1);
   Stage-2 `TrackAnalyzer` module only after M4 lands.
8. **M8 — Segmentation interop:** Stage-1 external-mask import (Phase E1); the
   cellpose → TrackMate → ADAPT route (Phase E2) rides on M7; in-UI cellpose
   (Phase E3) only after the Java-target question is revisited.
9. **M9 — Track export:** Stage-1 Ultrack-CSV export for inTRACKtive (Phase F1);
   GEFF (Phase F2) only if needed.
10. **M10 — Upstream dependency hygiene (do first):** license fixes, tagging,
    TrackMate-version web, cross-pin repointing, then ADAPT pin-to-tags
    (Phase G). Blocks A2 / Decision 2. — **done (2026-09-27).**
11. **M11 — Interactive Fiji run & smoke-test harness:** stage the built plugin
    into a local Fiji, launch it, and run the two plugins against test data
    (Phase H). Becomes a per-change gate once landed. — **Fiji wired + live smoke
    test done (2026-10-02); output baseline (H5) pending.**

Each milestone is independently shippable and testable; M1–M3 can proceed in
parallel. Package renaming (Q4) should be done early in M4 before it cascades
into other work. M7's Stage-1 bridge is independent of the Java-target decision
and can be tackled earlier if desired; Stage-2 depends on M4's model-agnostic
refactor. M8's Stage-1 (external-mask import) is likewise Java-target
independent. M9 is a self-contained exporter and can land at any point.

**M10 (Phase G) is done.** The two upstream libraries are tagged
(`IAClassLibrary v2.0.1`, `TrackerLibrary v4.0.2`) and re-pointed; the obsolete
`AdaptDataProcessing` was dropped (along with the `Bleb_Data_Analysis` plugin).
Java 21 + TrackMate 8 are in place.

**M11 (Phase H) is now the immediate gate for every change** — headless
build/test is no longer sufficient; each change must be exercised interactively
in Fiji before it is considered done. The plugin is installed and the H2 live
smoke test has passed; the only remaining M11 work is the H5 output baseline
(deferred until after the B2a output-structure simplification).

## M4 + B2a — ordered work breakdown

Each step is independently committable with its own verification gate.

1. **Package rename → lowercase** (Decision 4 / A5.2). `Adapt`→`adapt`,
   `Output`→`output`, `Visualisation`→`visualisation` (`ui` stays). Update all
   imports, `plugins.config` FQNs, and `pom.xml` `main-class`/`package-name`.
   First because it cascades into every later step. Gate: `mvn verify` + Fiji
   smoke test. — **done (2026-10-03).**
2. **`readParams()` CSV → JSON** (Decision 5 / A4.2). Add Jackson (or confirm
   transitive), define a versioned schema, rewrite the parser, ship a
   `params.json` example + migration note for old `params.csv`. Self-contained.
   Gate: unit test + build. — **done (2026-10-03): Jackson 2.19.2 added,
   5 unit tests green (`Analyse_BatchReadParamsTest`), `params.example.json` +
   `params.example.md` shipped.**
3. **Decompose `Analyse_Movie.analyse()`** (A4.1). Extract segmentation /
   map-building / protrusion-analysis / output-writing into single-responsibility
   methods; move pure math to static/package-private helpers with tests. No
   behaviour change. Gate: build + existing tests still green. — **done
   (2026-10-03): `analyse()` now delegates to `createOutputDirectories()`,
   `segmentCells()`, and `generateOutputs()`; 12/12 tests green.**
4. **Fix output anomalies** (B2a, low-risk half). UTF-8 everywhere (fix Latin-1
   `µ`), drop the `bleb_data_*.csv` spurious first line + trailing comma, decide
   on blank `Skew`/`Kurt`. Still the old layout — just make output correct/
   deterministic first. Gate: re-run in Fiji, eyeball the CSVs. — **done
   (2026-10-03): added UTF-8 `CsvWriter` to replace `DataWriter` (ISO-8859-1),
   switched `PrintWriter` sites to UTF-8, removed the spurious line + trailing
   comma; blank `Skew`/`Kurt` left as ImageJ "undefined" (`" "`).**
5. **Decompose `RunnableOutputGenerator.buildOutput()`** (A4.1). Refactor the god
   method into single-responsibility writers. Do this before/with step 6 so the
   schema lands once. Gate: build + Fiji run (outputs unchanged so far). — **done
   (2026-10-03): extracted `saveCellMapImages(...)` as the image/boundary writer.**
6. **Tidy output restructure** (B2a, breaking half). `tables/*.csv` (add
   `cell_id`/`bleb_id`, merge per-cell files, ASCII column names),
   `images/cell_NNN/*.tif` multi-page stacks, `parameters.json` + `README.md`
   manifest, deterministic `labels.zip` (B1 fix). Gate: Fiji run + review the new
   tree. — **done (2026-10-05): ASCII column names; `tables/` + `images/cell_NNN/`
   restructure; `parameters.json` + `README.md` manifest; `cell_id`/`bleb_id`
   columns + merged `velocity.csv`/`boundary.csv`/`blebs.csv`; multi-page TIFF
   visualisation stacks; deterministic `labels.zip` ROI order; `Output_Folder_Structure.PNG`
   regenerated.**
7. **Remove static `GUI.UV`** (A4.3). Pass a `UserVariables` per run / introduce a
   run-context object instead of the static singleton. Touches GUI +
   `Analyse_Movie`/`Analyse_Batch`. Gate: build + batch/single GUI runs.
   *(Pair with the B1.1 overlay-based preview reconfiguration so the GUI is
   restructured once rather than twice.)*
   — **done (2026-10-05): `GUI.UV` is now an instance field; `getUv()` is an
   instance method; `Analyse_Movie`/`Analyse_Batch` call `gui.getUv()`.**
8. **Normalise concurrency** (A4.4). Document (or consolidate) `NotificationThread`
   vs `MultiThreadedProcess`. Low-risk: document + enforce; consolidate only if
   warranted. Gate: docs.
   — **done (2026-10-05): documented the two patterns and their distinct jobs in
   `AGENTS.md` (`NotificationThread` = single-thread preview callback;
   `MultiThreadedProcess` = parallel output/visualisation pool); cleaned up the
   `NotificationThread` Javadoc (`doRun` → `doWork`). Consolidation decided
   against.**
9. **Regenerate baseline + docs.** Run the H5 SHA-256 baseline against the new
   structure, regenerate `Output_Folder_Structure.PNG`, update the plan/wiki.
   Gate: baseline passes.
   — **done (2026-10-05): added `bin/BaselineTool.java` + `bin/compute-baseline.cmd`
   + `bin/verify-baseline.cmd`; generated `test_data/baselines/blebbing_cell.sha256`
   (15 deterministic text outputs, `BASELINE_PASS`); regenerated
   `content/Output_Folder_Structure.PNG` version-agnostic. Binary-output and
   `labels.zip` normalisation remain deferred (pixel/ROI comparison).**

Steps 2 and 3 are independent of each other and of 4–6; step 4 is a safe
precursor to 6; 6 must precede 9.

## Decisions (resolved open questions)

These were resolved with the maintainer on 2024-09-24 and are the authoritative
input for the phases above.

1. **License — GPL-3.0.** The root `LICENSE` and the majority of source headers
   already declare GPL-3.0; `pom.xml`'s `Simplified BSD` (and the
   `license.licenseName=bsd_2` property) is stale metadata and must be corrected
   to GPL-3.0. Replace the six NetBeans "change this header" stubs with the GPL
   header. `license.copyrightOwners` is **David Barry** (development predates
   the Francis Crick Institute, so the Crick cannot claim copyright).
   *(Applied in M1 — pom.xml and headers corrected.)*
2. **Dependencies — stay on JitPack, pin to tags.** Do not vendor. Do not use
   GitHub Packages. Tag `IAClassLibrary` and `TrackerLibrary` with releases in
   their own repos; JitPack resolves tagged versions auth-free. Update the
   `pom.xml` versions from commit hashes to those tags.
   *(Applied: `IAClassLibrary` → `v2.0.1`, `TrackerLibrary` → `v4.0.2`.
   `AdaptDataProcessing` was **removed** as deprecated/obsolete — its README
   recommends a Python rewrite; the `Bleb_Data_Analysis` plugin was retired
   with it.)*
   - **`mvn_settings.xml` / GitHub Packages is vestigial.** Investigation shows
     only the CI workflow references it; no `pom.xml` dependency resolves from
     `maven.pkg.github.com`. Remove `mvn_settings.xml` and the `PAT`/`--settings`
     wiring from `maven.yml` (verify with one clean CI run before deleting).
3. **Target JDK — Java 21 (resolved 2026-09-27).** Move everything to Java 21 to
   avoid cross-project compatibility issues: this aligns ADAPT with the siblings
   (IAClassLibrary, TrackerLibrary), Fiji's runtime, and TrackMate ≥ 8. This
   *reverses* the earlier Java 11 target.
   - ✔ `scijava.jvm.version=21` set; `maven.compiler.release=21` verified; clean
     compile of all 17 sources at `release 21` passes.
   - ✔ `TrackMate:7.14.0` pin removed — the parent now resolves TrackMate 8.0.0.
   - ✔ `IAClassLibrary` → `v2.0.1`, `TrackerLibrary` → `v4.0.2` (clean compile).
   - ✔ `AdaptDataProcessing` removed (deprecated/obsolete) and `Bleb_Data_Analysis`
     retired.
4. **Package names — rename to lowercase.** ✔ Done (2026-10-03): `Adapt`→`adapt`,
   `Output`→`output`, `Visualisation`→`visualisation` (`ui` unchanged); all
   imports, `plugins.config`, and `pom.xml` `main-class` updated.
5. **Params file — JSON.** Replace the positional CSV parsing in
   `Analyse_Batch.readParams()` with JSON (validated via Jackson, adding it as a
   dependency if it is not already transitive via Bio-Formats/SciJava),
   including a schema/version for forward-compatibility.
   *(Applied in M4 step 2 — `readParams()` now loads `version: 1` JSON via
   Jackson; old `params.csv` files are rejected; `params.example.json` +
   `params.example.md` ship in `src/main/resources/`.)*
6. **Plugin framework — stay ImageJ 1.x (resolved 2026-09-27).** ADAPT remains a
   `plugins.config` + `ij.plugin.PlugIn` plugin. A modern SciJava
   reimplementation (`@Plugin` + `@Parameter`) is recorded as a future option,
   not planned work; if revisited, prefer thin `Command` wrappers over the
   existing core (after M4) rather than a full rewrite. See Phase H0.
7. **Versioning — plain `X.Y.Z` on `development`, tag `vX.Y.Z` (resolved
   2026-10-03).** ADAPT follows the sibling convention (see `IAClassLibrary`'s
   `development` branch): `pom.xml` `<version>` is a plain `X.Y.Z` (no
   `-SNAPSHOT`), and `maven-release-plugin` tags releases as
   `v@{project.version}` (e.g. `v4.0.1`). The `v4.0.1` tag is created only once
   the modernisation work lands.
