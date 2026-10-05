# Revision Log

A chronological record of significant development decisions and changes to
ADAPT, and — more importantly — the mistakes made and lessons learned, so that
the modernisation of this repository does not repeat the missteps already
documented for its two upstream siblings
([`IAClassLibrary`](https://github.com/djpbarry/IAClassLibrary/blob/development/REVISION_LOG.md)
and
[`TrackerLibrary`](https://github.com/djpbarry/TrackerLibrary/blob/development/REVISION_LOG.md)).

ADAPT is the **end consumer** of the chain (`IAClassLibrary` →
`TrackerLibrary` → ADAPT): it is a Fiji/ImageJ plugin,
not a library, so the "public API" concerns that dominate the sibling logs
apply only lightly here. Its modernisation is instead about robustness,
testability, and UX, as laid out in `DEVELOPMENT_PLAN.md`.

Git commit messages provide the fine-grained record; this file is the distilled,
dated narrative plus the "what not to do again" notes.

## Conventions

- Entries are dated and reference commits and/or `DEVELOPMENT_PLAN.md` phases.
- "Lesson" entries state what went wrong and the rule to apply next time.
- Lessons marked *(inherited)* are carried forward from the sibling
  `REVISION_LOG.md` files, whose modernisation explicitly lists ADAPT as the
  final downstream consumer and intends these rules to guide it.

---

## 2026-10-05 — Overlay-based segmentation preview (M5 step 2)

Replaced the embedded preview with non-destructive overlays:

- `Analyse_Movie.generatePreview()` now builds a per-slice `Overlay`
  (red boundary `PolygonRoi`, blue centre `OvalRoi`, green cortex band, yellow
  bleb markers) instead of drawing into pixel data; `getPreviewImages()` became
  `getPreviewOverlay()`.
- `GUI` now takes the original `ImagePlus`es and, on preview completion, applies
  the overlay via `setOverlay()`/`updateAndDraw()`, so the input `ImageStack`s
  stay untouched and the source `ImageWindow`s remain interactive.
- Batch preview is a temporary regression (no open windows); it will be restored
  when the layout is rewritten in M5 step 3/4.

`mvn test` green (13/13); version bumped to `4.0.10` (preview now auto-detects
seed points itself via a new `detectSeedPoints()` helper, avoiding the external
`RegionGrower.hideWindows()` side effect that was hiding the source windows; the
original `ImagePlus` is navigated to the preview slice via `setSlice()`, and the
toggle button became a single-click "Preview" button).

---

## 2026-10-05 — Non-modal GUI + run lifecycle (M5 step 1)

Started the M5 GUI rework:

- `GUI` is now non-modal and exposes `setOnRun(Runnable)`, invoked by the Run
  button after `setVariables()` succeeds (the dialog disposes and the callback
  starts the analysis on a background thread).
- `Analyse_Movie.analyse()` was split: setup + (non-modal GUI + callback) for the
  interactive path, and `runPipeline()` for the batch/protrusion paths. The
  single-movie finalisation (`TrajectoryAnalysis` + `parameters.json`/`README.md`)
  moved into `finishAnalysis()`.
- `Analyse_Batch` now shows the GUI once and runs its file loop from the callback
  (`runBatch()`), preserving the existing per-file synchronous `analyse()` path.

`mvn test` green (13/13); version bumped to `4.0.6`. The overlay-based preview is
the next M5 step.

---

## 2026-10-05 — M5 GUI rework broken into an ordered plan (planning only)

Recorded a six-step M5 execution plan in `DEVELOPMENT_PLAN.md` (under "B1
execution plan (M5)"): non-modal dialog + run lifecycle → overlay-based preview →
hand-managed layout → collapsible sections + tooltips → validation/presets →
cancellation/progress. Ordering is dependency-driven (non-modal first, overlay
second, layout before sections/presets). Planning only — no code changed.

---

## 2026-10-05 — H5 output baseline established (M4 step 9)

Added a minimal quantitative output-baseline gate and closed out M4:

- `bin/BaselineTool.java` + `bin/compute-baseline.cmd` + `bin/verify-baseline.cmd`
  hash the deterministic text outputs (UTF-8 CSVs + `parameters.json`) by SHA-256
  and compare against a committed manifest.
- Generated `test_data/baselines/blebbing_cell.sha256` (15 files) from the
  verified `Adapt_v4.0.3` output; `verify-baseline` reports `BASELINE_PASS`.
- Regenerated `content/Output_Folder_Structure.PNG` with a version-agnostic root.

Binary outputs (multi-page TIFF/PNG) and `labels.zip` remain out of the v1
baseline (they need pixel-payload / sorted-ROI normalisation, not raw hashes).
All nine M4 steps are now complete; version bumped to `4.0.5`.

---

## 2026-10-05 — Concurrency normalised (M4 step 8 / A4.4)

Documented the two coexisting concurrency patterns and their distinct jobs in
`AGENTS.md`, and cleaned up the misleading `NotificationThread` Javadoc:

- `NotificationThread` (in-repo) is a single-background-thread observer/callback
  wrapper used only for the GUI preview path (`new Thread(previewAnalyser)`).
- `MultiThreadedProcess`/`RunnableProcess` (external) is the executor-pool
  pattern used by `MultiThreadedOutputGenerator` (one task per cell) and
  `MultiThreadedVisualisationGenerator` (one task per frame), finishing via
  `terminate(msg)`.

Consolidation was decided against — a single-thread preview callback and a
parallel executor pool serve different needs. `mvn test` green (13/13); version
bumped to `4.0.4`.

---

## 2026-10-05 — Removed static `GUI.UV` (M4 step 7 / A4.3)

Eliminated the shared `UserVariables` singleton on `GUI`:

- `GUI.UV` is now a per-instance `final UserVariables` field (was `static final`).
- `GUI.getUv()` is now an instance method returning that field.
- `Analyse_Movie` and `Analyse_Batch` now read `gui.getUv()` on their local
  `GUI` instance instead of `GUI.getUv()`.

This removes cross-session stale/shared state (each dialog now starts from fresh
`UserVariables` defaults). `mvn test` green (13/13); version bumped to `4.0.3`.

---

## 2026-10-05 — GUI preview redesign planned (overlay-based, deferred to M5)

Recorded in `DEVELOPMENT_PLAN.md` (Phase B1): rework ADAPT's preview model so the
input images stay in their original `ImageWindow` containers and segmentation
previews are rendered as ImageJ `Overlay`s / `RoiManager` ROIs instead of being
embedded into the pixel data. This matches TrackMate and GIANI, is non-destructive
and live, and will be paired with M4 step 7 (static `GUI.UV` removal) and step 8
(concurrency normalisation) so the GUI is restructured once. Planning only — no
code changed.

---

## 2026-10-05 — Deterministic `labels.zip` + step-6 completion (M4 step 6 / B2a)

Made the `labels.zip` ROI order deterministic and closed out the step-6 output
restructure:

- `RunnableVisualisationGenerator` now writes its `TextRoi` labels into a
  per-frame `List<Roi>` slot instead of appending to a shared `Overlay`, removing
  a cross-thread race; `MultiThreadedVisualisationGenerator` assembles the
  `Overlay` in frame/cell order after `terminate()`.
- Two back-to-back Fiji runs produced identical `labels.zip` entry order and
  identical `.roi` file contents (the zip bytes differ only in embedded
  timestamps).
- Regenerated `content/Output_Folder_Structure.PNG` to show the new `tables/` +
  `images/` tree.

Step 6 (B2a tidy output restructure) is now complete. Remaining M4 steps: remove
static `GUI.UV` (7), normalise concurrency (8), regenerate H5 baseline + docs (9).

---

## 2026-10-05 — Version bump convention enforced (`4.0.1` → `4.0.2`)

Following the sibling convention, `pom.xml` `<version>` is now bumped on every
change (plain `X.Y.Z`, no `-SNAPSHOT`). Bumped `4.0.1` → `4.0.2` to cover the
step-6 output restructure (manifest, merged tables, multi-page TIFF stacks,
deterministic `labels.zip`) and the dependency re-pin. This convention is now
stated explicitly in `AGENTS.md`.

---

## 2026-10-05 — Dependency pins bumped (IAClassLibrary `2.0.22`, TrackerLibrary `v4.0.8`)

The sibling libraries were modernised again and their latest release tags
re-pointed ADAPT. `mvn clean test` (13/13) confirms the APIs are unchanged for
ADAPT's use.

- `IAClassLibrary` `v2.0.1` → `2.0.22`
- `TrackerLibrary` `v4.0.2` → `v4.0.8`

JitPack quirk: for `IAClassLibrary` the `v`-prefixed coordinate (`v2.0.22`)
still reports a cached build error on JitPack, while the tag-without-`v`
coordinate (`2.0.22`) builds cleanly. ADAPT therefore pins `2.0.22` (no `v`)
for `IAClassLibrary` but `v4.0.8` for `TrackerLibrary`. `install-to-fiji.cmd`
now ships `IAClassLibrary-2.0.22.jar` and `TrackerLibrary-v4.0.8.jar`.

---

## 2026-10-05 — Multi-page TIFF visualisation stacks (M4 step 6 / B2a)

Replaced the per-frame `NNN.tiff` visualisation output with single multi-page
stacks:

- `RunnableVisualisationGenerator` now renders each frame into shared
  `FloatProcessor[]` slots instead of writing `NNN.tiff`.
- `MultiThreadedVisualisationGenerator` assembles those frames after
  `terminate()` and writes `images/velocity_visualisation.tif` +
  `images/curvature_visualisation.tif` via `BioFormatsImageWriter.saveStack`.
- `labels.zip` moved to the output root (was `Visualisations_Output/labels.zip`),
  and the unconditional `Visualisations_Output` directory is gone (it is now
  created only on the segmentation-visualisation path).

Fiji re-run confirmed the two `.tif` stacks under `images/`, `labels.zip` at the
root, and no `Visualisations_Output` tree. ROI order in `labels.zip` remains
non-deterministic and is the next step-6 item.

---

## 2026-10-03 — Version aligned with sibling convention (dropped `-SNAPSHOT`)

`IAClassLibrary`'s `development` branch uses a plain `<version>X.Y.Z</version>`
(no `-SNAPSHOT`) and tags releases as `v@{project.version}`. ADAPT's `pom.xml`
was still `4.0.0-SNAPSHOT`, so it now follows the same convention:

- `<version>4.0.1</version>` (bumped from `4.0.0-SNAPSHOT`; dropped the suffix).
- Added `maven-release-plugin` with
  `<tagNameFormat>v@{project.version}</tagNameFormat>` so future releases tag as
  `v4.0.0`, `v4.0.1`, …

The `v4.0.0` tag is still only created once the modernisation work lands.

---

## 2026-10-03 — Output restructure started (M4 step 6 / B2a)

First slice of the breaking output restructure:

- **ASCII column names:** converted the output CSV headings to lowercase
  snake_case ASCII — `StaticVariables` (`time_s`, `zeroed_time_s`, `v_um_s`,
  `total_signal_au`, `length_um`, `mean_signal`, `normalised_length`), the
  trajectory headings (`frame,time_s,cell_id,x_um,y_um`), and the velocity
  headings (`frame,%_protruding,%_retracting,mean_protrusion_velocity_um_min,
  mean_retraction_velocity_um_min`). GUI labels (with `µ`) are untouched.
- **Directory restructure (partial):** `Population_Data` → `tables`,
  `Individual_Cell_Data` → `images`, and per-cell dirs `0` → `cell_000`
  (zero-padded). The `Visualisations` dir and multi-page stacks are not yet done.
  (`openResultsDirectory` initially appended `_Output` to these child dirs; that
  was dropped by switching to `createDirectory` so the names are clean
  `tables/`, `images/cell_000/`.)

`mvn test` green (12/12). Remaining for step 6: multi-page TIFF stacks and
deterministic `labels.zip`.

---

## 2026-10-03 — `parameters.json` + `README.md` manifest (M4 step 6 / B2a)

Replaced the external `PropertyWriter` (which wrote a `properties.xml`) with a
new `net.calm.adapt.output.ParameterWriter`:

- `saveParameters(Properties, File)` converts the GUI `Properties` to a sorted
  `TreeMap`, then writes a pretty-printed UTF-8 `parameters.json` with Jackson
  (`ObjectMapper` + `SerializationFeature.INDENT_OUTPUT`).
- `saveReadme(File, String)` writes a short `README.md` describing the
  `parameters.json` / `tables/` / `images/` / `labels.zip` layout and noting the
  snake_case ASCII column names.

`Analyse_Movie.run()` now calls both methods instead of
`PropertyWriter.saveProperties(...)`; the now-unused `PropertyWriter` import was
removed. Keys are the GUI labels (so `parameters.json` contains e.g. `"Spatial
Resolution (µm/pixel)"`); no timestamp fields are captured. `mvn test` green
(12/12), and a Fiji re-run produced `parameters.json` + `README.md` with no
`properties.xml` at the output root.

---

## 2026-10-03 — `cell_id`/`bleb_id` columns + merged tidy tables (M4 step 6 / B2a)

Added a `net.calm.adapt.output.CellTableAccumulator` that the parallel
`RunnableOutputGenerator` tasks feed with their per-cell rows, then a single
post-`terminate()` write merges them into tidy long-format tables:

- `tables/velocity.csv` — `cell_id,frame,%_protruding,%_retracting,mean_protrusion_velocity_um_min,mean_retraction_velocity_um_min`.
- `tables/boundary.csv` — `cell_id,frame,x,y`.
- `tables/blebs.csv` — `cell_id,bleb_id,time_s,v_um_s,total_signal_au,mean_signal,length_um,normalised_length`.

This replaces the per-cell `VelocityAnalysis.csv`, `cell_boundary.csv`, and
`Bleb_Data_Files/bleb_data_N.csv`. The already-merged population tables were
renamed to snake_case (`Trajectories.csv` → `trajectories.csv`, `Morphology.csv`
→ `morphology.csv`, `Fluorescence.csv` → `fluorescence.csv`) and their cell-id
headings normalised (`Cell_ID`/`Cell ID` → `cell_id`). Added a
`CellTableAccumulatorTest` unit test (13/13 green), and a Fiji re-run confirmed
the new `tables/*.csv` files and the removal of the old per-cell CSVs.

---

## 2026-10-03 — Decomposed `RunnableOutputGenerator.buildOutput()` (M4 step 5 / A4.1)

Extracted the image/boundary writing from `buildOutput()` into a single-
responsibility `saveCellMapImages(sigMap, greyVelMap, greyCurvMap, greySigMap,
boundaryPoints)` method. `buildOutput()` now reads: curve-map setup →
`buildCurveMap` → vel/sig map building → smoothing/grey-map generation →
`generateMaps` → `saveCellMapImages`. No behaviour change (the `sigStack != null`
guard became the equivalent `sigMap != null`). `mvn test` green (12/12).

This isolates the writer ahead of the B2a output restructure (step 6). The
duplicate `Analyse_Movie.buildOutput()` (preview path) is deliberately left for
step 6, when both writers will be updated together.

---

## 2026-10-03 — Fixed output anomalies (M4 step 4 / B2a)

Unified all CSV writers on **UTF-8** and removed two `bleb_data_*.csv` defects:

- Added `net.calm.adapt.output.CsvWriter`, a UTF-8 replacement for the external
  `DataWriter` (which hardcodes `GenVariables.ISO` = ISO-8859-1, so `µ` came out
  as Latin-1 `0xB5`). Swapped the four `DataWriter.saveValues`/`saveResultsTable`
  call sites (Morphology, Trajectories, Fluorescence, cell_boundary) to it.
- Switched the remaining `new PrintWriter(new FileOutputStream(...))` sites
  (VelocityAnalysis, FilopodiaVersusTime, cell_boundary_points) to an explicit
  UTF-8 `OutputStreamWriter`.
- Removed the spurious first line (`directory + "_" + count`) written before the
  `bleb_data_*.csv` header, and fixed the header to drop the trailing comma
  (now `String.join(",", headings)`). Fixed a latent trailing-space in the
  `cell_boundary.csv` filename.
- **Decision:** blank `Skew`/`Kurt` in `Morphology.csv` are left as ImageJ's
  "undefined" (`" "`, NaN → space); pandas treats this as missing, so no change.

`mvn clean test` green (12/12). The Fiji interactive re-run (eyeball the CSVs) is
the remaining gate.

---

## 2026-10-03 — Decomposed `Analyse_Movie.analyse()` (M4 step 3 / A4.1)

Split the ~225-line `analyse()` into three single-responsibility private methods
so the pipeline reads as a sequence instead of a wall of code, with no behaviour
change:

- `createOutputDirectories(cytoImp, imageName)` — derives the parent/cells/
  population/visualisation directories.
- `segmentCells(cytoStack, width, height, cytoSize)` — the full per-frame
  segmentation loop, region→cell assignment, and grey-threshold wiring.
- `generateOutputs()` — the morphology/velocity/signal output-vs-simple-visual
  dispatch.

`analyse()` now reads: prepare inputs → create dirs → convert to 8-bit → show GUI
→ segment → filter → generate outputs → morphology → trajectories. `mvn clean
verify` passes with 12/12 tests green.

Lesson: the extracted blocks were already cohesive (they operated on a narrow set
of fields) — the obstacle to extraction was that they *mutated* shared fields
(`cellData`, `roi`, `minLength`, the `*Dir` fields). Extracting them as `private`
methods that mutate those fields kept the diff mechanical and behaviour-neutral,
which is the right first move before the larger A4.3 context-object refactor.

---

## 2026-10-03 — `readParams()` CSV → JSON (M4 step 2 / Decision 5)

Replaced the positional `Scanner` + `br.readLine()` CSV parser in
`Analyse_Batch.readParams()` with a versioned JSON parser. Jackson
(`jackson-databind` 2.19.2, previously only transitive) is now an explicit
dependency. The parser validates the top-level object, rejects any `version`
other than `1`, and reads every field through typed `reqBool`/`reqInt`/
`reqDouble`/`reqText` helpers so missing/ill-typed fields fail loudly instead of
silently mis-parsing.

`src/main/resources/params.example.json` (canonical example) and
`src/main/resources/params.example.md` (CSV→JSON migration note) are shipped so
existing `params.csv` users can migrate. Added 5 unit tests
(`Analyse_BatchReadParamsTest`): applies-all-fields, rejects unsupported/missing
version, rejects missing field, rejects non-object. `mvn clean verify` green —
12/12 tests.

Lesson: the old parser's silent positional coupling (3 header lines skipped, then
26 ordered `Scanner` reads) meant a single added/removed line broke every
subsequent field with only a generic `Exception` to show for it. Versioned,
named-key JSON makes the failure mode explicit.

---

## 2026-10-03 — Fiji harness fixes (M11 follow-up)

Two `bin/` scripts had gone stale/buggy against the post-rename plugin:

- `smoke-test-fiji.cmd` still checked the old mixed-case classes
  (`net.calm.adapt.Adapt.*`); updated to `net.calm.adapt.adapt.*` so the
  headless load-check matches the lowercase package names from M4 step 1.
- `install-to-fiji.cmd` copied `target/adapt-*-tests.jar` into Fiji `plugins/`
  (its filter only excluded `sources`/`javadoc`). Added `tests` to the filter so
  the JUnit test jar never ships to a live Fiji.

`install-to-fiji.cmd && smoke-test-fiji.cmd` now installs exactly the plugin +
the two sibling libraries and reports `ALL_ADAPT_PLUGINS_LOADABLE`.

---

## 2026-10-03 — Package rename to lowercase (M4 step 1 / Decision 4)

Renamed the three mixed-case packages `Adapt`→`adapt`, `Output`→`output`,
`Visualisation`→`visualisation` (`ui` unchanged). Updated every `package`
declaration and import, `plugins.config` FQNs, and `pom.xml` `main-class` (the
GitHub repo URLs were left untouched). Directories were renamed via a two-step
`git mv` (case-insensitive filesystem). `mvn clean verify` passes: 17 main + 2
test sources, 7/7 tests green under `net.calm.adapt.adapt`.

Used `pixi` for Python to do the bulk string replacement (no `sed`/`perl` in the
shell); the disposable project lived under `target/` (gitignored).

---

## 2026-10-02 — M4 + B2a broken into an ordered work breakdown

Recorded a 9-step dependency-ordered sequence in `DEVELOPMENT_PLAN.md` to split
the coupled M4 refactor and B2a output restructure into independently
committable steps: package rename → JSON params → decompose `analyse()` → fix
output anomalies → decompose `buildOutput()` → tidy output restructure → remove
static `GUI.UV` → normalise concurrency → regenerate baseline + docs.

Planning only — no code changed.

---

## 2026-10-02 — M2: JUnit 5 test harness landed (first unit tests)

Added JUnit 5 (`junit-jupiter-api` + `engine`, version managed by `pom-scijava`;
the parent already wires `maven-surefire-plugin` + JUnit Platform + JaCoCo) and
wrote the first 7 unit tests against the pure-logic parts of two classes:

- `CurveMapAnalyser.calcScaledCurveRange()` and `isLocalCurvatureExtreme()` —
  the curvature-extrema detection logic. `isLocalCurvatureExtreme` was changed
  from `private` to package-private so it can be tested directly.
- `FluorescenceDistAnalyser.calcGlcmStats()` — the GLCM contrast/energy/
  homogeneity computation, extracted to a `static` helper taking the GLCM matrix
  (the instance method now delegates to it).

`mvn verify` passes with 7/7 tests green. The golden-file output test (A3.3) is
deliberately deferred: it needs the headless Fiji pipeline and is best done after
the B2a output-structure simplification and the H5 baseline land.

---

## 2026-10-02 — Test data moved to Git LFS

Replaced the single committed `test_data/ADAPT_Test_Data.zip` (~91 MB) with the
two extracted input stacks, committed via **Git LFS**:

- `test_data/migrating_cell/migrating_cell.ome.tiff` (~107 MB — over GitHub's
  100 MB regular-file limit, so LFS was mandatory)
- `test_data/blebbing_cell/blebbing_cell.ome.tiff` (~75 MB)

`.gitattributes` now LFS-tracks image/binary formats (`*.tiff`, `*.tif`, `*.jpg`,
`*.jpeg`, `*.png`, `*.zip`); `.gitignore` excludes the regenerable
`test_data/**/Adapt_*/` output trees (the H5 golden baseline will be committed
separately after the B2a output-structure simplification lands). The old zip
remains in git history until a `git filter-repo`/BFG sweep.

---

## 2026-10-02 — Live Fiji run succeeded; test-data output reviewed

The plugin now runs end-to-end in the local Fiji against `test_data/` (both
`migrating_cell` and `blebbing_cell`). Two things came out of this.

**Dependency story (update sites).** The `NoClassDefFoundError:
inra.ijpb…FloodFillComponentsLabeling` is **MorphoLibJ**, which ships on the
**IJPB-plugins** update site, not base Fiji. ADAPT's canonical install needs
CALM (plugin + siblings + `commons-csv`) + IJPB-plugins (MorphoLibJ) + 3D ImageJ
Suite (`mcib3d-core`). The modernised `IAClassLibrary v2.0.1` also pulls
`imagescience` and `combinatoricslib` as `compile` deps, but these are transitive
and not exercised at runtime, so they can be left uninstalled. The old wiki
Installation page omits ImageScience — now moot.

**Output anomalies (logged, not yet fixed).** Reviewing the CSVs found:

- **Encoding inconsistency:** `Trajectories.csv` writes `µ` as Latin-1 (`0xB5`)
  while `VelocityAnalysis.csv` / `bleb_data_*.csv` write it as UTF-8. Likely a
  platform-default `FileWriter` charset; breaks cross-platform byte-identical
  baselines and confuses UTF-8-expecting Python tooling.
- **`bleb_data_*.csv` spurious first line:** a filesystem path
  (`…\test_data\blebbing_cell_0`) precedes the real header, so strict CSV parsers
  read the header off by one.
- **`bleb_data_*.csv` trailing comma:** 7 header fields vs 6 data columns.
- **`Morphology.csv` blank `Skew`/`Kurt`** (`" "`) on flat/binary regions —
  ImageJ "undefined", expected but schema-relevant.

These feed the output-structure simplification under Phase B2 and the golden-file
normalisation under Phase A3/H5.

---

## 2026-10-02 — Hardened `install-to-fiji.cmd`; wired up a fresh Fiji

The local-Fiji install script had two gaps that surfaced when wiring a fresh
Fiji (2.18.1, `fiji-latest-portable-nojava`) on a new machine:

- It ran `mvnw package` without `clean`, so a dirty `target/` left stale
  versioned jars (e.g. an old `adapt-3.0.10.jar` alongside the current
  `adapt-4.0.0-SNAPSHOT.jar`) and copied them all into `Fiji/plugins/`,
  causing duplicate classes.
- It only copied the ADAPT plugin jar; the two sibling libraries
  (`IAClassLibrary`, `TrackerLibrary`) were assumed to already be present in
  Fiji, which a fresh install lacks.

`bin/install-to-fiji.cmd` now runs `mvnw clean package`, installs the plugin
into `plugins/`, and installs the sibling libraries into `jars/` (replacing any
old versions). TrackMate is deliberately not copied: it is provided by Fiji's
updater (`jars/TrackMate-8.1.6.jar`), and `IAClassLibrary` declares it without a
`<version>`, so the version is governed by the shared parent
`pom-scijava:45.1.0` (`TrackMate.version=8.0.0`). Fiji's 8.1.6 is a compatible
patch/minor bump for ADAPT's transitive use.

---

## 2026-09-28 — Externalised machine-specific paths to `bin/local-env.cmd`

The `bin/` scripts (`install-to-fiji.cmd`, `run-fiji.cmd`,
`smoke-test-fiji.cmd`) no longer hardcode the Fiji/JDK paths. They `call`
`bin/local-env.cmd` (gitignored) for `FIJI_DIR`/`JAVA_HOME`, falling back to a
command-line argument. `bin/local-env.cmd.example` (committed) documents the
variables; each machine keeps its own `local-env.cmd` without churning the
committed scripts. The maintainer works on several machines, so hardcoded paths
were unworkable.

---

## 2026-09-28 — Removed `AdaptDataProcessing`; retired `Bleb_Data_Analysis`

`AdaptDataProcessing` is deprecated — its README now recommends a Python rewrite
for its analysis. Dropped it from ADAPT entirely:

- Removed the `com.github.djpbarry:AdaptDataProcessing` dependency from `pom.xml`.
- Deleted the `Bleb_Data_Analysis` plugin (a thin wrapper over
  `AdaptDataProcessing.DataFileAverager`) and its `plugins.config` entry.
- Removed the inline `DataFileAverager` call in `RunnableOutputGenerator` (the
  per-bleb `mean_data.csv` averaging step). That post-hoc averaging is now a
  Python step, per the upstream README.

ADAPT now has **two** registered plugins (`Analyse Movie`, `Batch Analysis`) and
**two** JitPack dependencies (`IAClassLibrary`, `TrackerLibrary`).

---

## 2026-09-27 — TrackerLibrary bumped to `v4.0.2` (version-skew fix)

`pom.xml` now pins `TrackerLibrary v4.0.2` (was `v4.0.1`). `v4.0.2` re-pins its
`IAClassLibrary` dependency to `v2.0.1` (the prior `v4.0.1` still pointed at the
old commit `37a1be016a`), so ADAPT's direct `IAClassLibrary v2.0.1` and the
transitive one now agree — resolving the version skew flagged during review.

---

## 2026-09-27 — Added Phase H: interactive Fiji run & smoke-test harness

From this point, headless build/test is no longer sufficient to consider a change
done — ADAPT must be exercised interactively inside Fiji. Added **Phase H** to
`DEVELOPMENT_PLAN.md` (staging/launch scripts, a per-change smoke test over the
three `plugins.config` commands against `test_data/ADAPT_Test_Data.zip`, and
entry-point hygiene for the `Main` debug path), plus **M11** in the sequencing
list. M11 is now the immediate gate for every change.

No code changed — planning only.

---

## 2026-09-27 — Switched to latest Fiji (2.18.1); fixed launcher

- Adopted the **latest Fiji** at `C:\Users\barryd\Fiji` (2.18.1, `nojre`). It
  requires **Java 21+** and ships **TrackMate 8.1.6** (matching the modernised
  `IAClassLibrary` `v2.0.1`).
- Launcher confirmed working: the jaunch launcher finds Java 21 via `JAVA_HOME`
  (no bundled JRE or junction needed — an initial `jre` junction was tried and
  then removed as unnecessary).
- Updated `bin/` scripts to default to the new Fiji dir, and `run-fiji.cmd` to
  use the bundled launcher. Updated `DEVELOPMENT_PLAN.md` Phase H1.

---

## 2026-09-27 — Re-pointed dependencies to tagged releases

`pom.xml` now pins the modernised siblings (Decision 2):

- `IAClassLibrary` `fe92f24c6e` → **`v2.0.1`**
- `TrackerLibrary` `99584ec579` → **`v4.0.1`**
- `AdaptDataProcessing` left at `95d31fcec8` (obsolete; still used by
  `Bleb_Data_Analysis`, pending removal).

`mvn clean verify` passes (18 sources compiled at `release 21`). One new
deprecation warning surfaced in `Analyse_Movie` — the modernised IAClassLibrary
marks `ProgressDialog`/`DataStatistics` `@Deprecated`; migrating those calls is a
follow-up, not a blocker.

---

## 2026-09-27 — JDK 21 upgrade applied; TrackMate pin removed

Executed the JDK 21 move in `pom.xml`:

- Added `scijava.jvm.version=21`; `maven.compiler.release` now resolves to **21**.
  Clean `mvn compile` recompiled all 18 sources with `javac [release 21]`
  (BUILD SUCCESS).
- Removed the `TrackMate:7.14.0` `dependencyManagement` pin. TrackMate is now
  resolved transitively (parent manages **TrackMate 8.0.0**), so ADAPT no longer
  pins it at all.

The three sibling dependencies are still the old commit hashes
(`fe92f24c6e`, `99584ec579`, `95d31fcec8`); re-pointing them to the new tags is
the next step once the upstream repos are tagged.

---

## 2026-09-27 — JDK 21 target + output-baseline plan (decisions)

- **JDK target moved to Java 21** (reverses Decision 3's Java 11). Rationale:
  avoid cross-project compatibility issues by aligning ADAPT with the siblings
  (IAClassLibrary, TrackerLibrary), Fiji's runtime, and TrackMate ≥ 8. Actions
  (not yet applied): compile target → 21, drop the `TrackMate:7.14.0` pin,
  re-point deps once upstream tags land. Recorded in `DEVELOPMENT_PLAN.md`
  Decision 3.
- **Output-baseline comparison** recorded as `DEVELOPMENT_PLAN.md` Phase H5: a
  SHA-256 per-file diff against the `ADAPT_Test_Data/` benchmark outputs, with
  normalization for timestamps (`*.properties`), non-deterministic ROI order
  (`labels.zip`, Issue #2), and TIFF/PNG metadata. Behaviour-preserving changes
  must reproduce the benchmark byte-for-byte; changes are only expected once the
  modernised deps land (upstream bug fixes).

No code changed — planning only.

---

## 2026-09-27 — Local Fiji wired up for live testing (M11 / Phase H)

- Fiji is at `C:\Users\barryd\fiji-nojre\Fiji.app` (a **nojre** build, no bundled
  JRE). Verified it runs on the local **JDK 21** (ImageJ 1.54p + Java 21.0.12.1).
- The bundled `fiji-windows-x64.exe` launcher **cannot find Java** (exits 1), so
  Fiji is launched via direct Java (`java -cp "jars\*;plugins\*" ij.ImageJ`).
- Installed `adapt-4.0.0-SNAPSHOT.jar` into `Fiji.app/plugins/`, removed the old
  `adapt-3.0.13.jar`. Sibling deps were already present and match the current
  pins (`iaclasslibrary-1.0.37.jar`, `trackerlibrary-3.0.10.jar`,
  `adaptdataprocessing-1.0.7.jar`).
- Confirmed all three ADAPT plugin classes load on the Fiji classpath.
- Committed `bin/install-to-fiji.cmd`, `bin/run-fiji.cmd`,
  `bin/smoke-test-fiji.cmd` (Phase H1), with the launcher fix left as a
  follow-up.

---

## 2026-09-27 — Plugin framework decision: stay ImageJ 1.x

Resolved (with the maintainer) that ADAPT **stays an ImageJ 1.x plugin**
(`plugins.config` + `ij.plugin.PlugIn`) rather than reimplementing as a modern
SciJava `@Plugin`/`@Parameter` command. A SciJava migration is recorded as a
future option only; if revisited, the preferred route is thin `Command` wrappers
over the existing core (after M4), not a full rewrite. Recorded in
`DEVELOPMENT_PLAN.md` as Decision 6 and Phase H0, with
https://imagej.net/develop/ij1-plugins as the authoritative reference for the
current model.

No code changed — planning only.

---

## 2026-09-27 — Version bump to `4.0.0-SNAPSHOT`

Bumped `pom.xml` `<version>` from `3.0.13` to `4.0.0-SNAPSHOT` — the first step
toward the semver discipline in L2 and the sibling's `4.0.0-SNAPSHOT` convention.
This is the pre-tag working version; the `v4.0.0` tag is only created once the
planned modernisation work lands.

---

## 2026-09-27 — M1 (Foundations) completed

**Phases A1, A2, A4.5, A4.6, and A6 done.** `mvn verify` passes on JDK 21 with
`maven.compiler.release=11`. The package rename (A5.2 / Decision 4) and the
`readParams()` → JSON replacement (Decision 5) are deliberately left to M4, as
the plan sequences them.

| Change | What |
|---|---|
| A1 | CI `actions/checkout@v4` → `@v5` and `actions/setup-java@v4` → `@v5` (Node 20 → Node 24 deprecation). |
| A2 | `pom.xml`: documented the three commit-hash pins (`IAClassLibrary:fe92f24c6e`, `TrackerLibrary:99584ec579`, `AdaptDataProcessing:95d31fcec8`) with the commit each refers to and why; flagged `AdaptDataProcessing` as deprecated. Re-pointing to tags remains M10 / Phase G. |
| A4.5 | Stripped ~300 lines of dead code: `Main.java` (commented experiments); the old Dijkstra segmentation methods (`initDistanceMaps`, `dijkstraDilate`, `buildDistanceMaps`, `expandRegions`); `generateScaleBar`, `drawBlebMovie`, `printParamFile`; and scattered debug `IJ.saveAs` blocks across `Analyse_Movie`, `BlebAnalyser`, `RunnableOutputGenerator`. |
| A4.6 | `pom.xml` license BSD-2 → GPL-3.0 (`license.licenseName=gpl_v3`, name "GNU General Public License v3.0 or later"); replaced the six NetBeans "change this header" stubs with the GPL header. |
| A6 | Replaced 2 empty `catch` blocks and 9 bare `System.out.println(e.toString())` / `IJ.log(e.toString())` with actionable `IJ.log(...)` messages; fixed a log typo ("saved" → "save"). |

### Verification

- `mvn verify` → **BUILD SUCCESS**.
- `mvnw` is tracked with the executable bit set (`100755`) — L10 is satisfied.

### Deliberately deferred

- **A5.2 (package rename)** — sequenced into M4 (see plan note).
- **`AdaptDataProcessing` / `Bleb_Data_Analysis` removal** — flagged as an open
  decision (see the upstream-dependency entry above), not forced inside M1.

---

## 2026-09-27 — Upstream dependencies have moved; `AdaptDataProcessing` is obsolete

The three JitPack dependencies are no longer in the state assumed by the plan.
Two of them have been substantially modernised, and the third is now deprecated.

### The two surviving libraries now publish Javadoc

- **IAClassLibrary** — https://djpbarry.github.io/IAClassLibrary/
- **TrackerLibrary** — https://djpbarry.github.io/TrackerLibrary/

Both have completed their own modernisation pass (GPL-3.0-or-later, Java 21,
Maven wrapper, and Javadoc publishing). Their `REVISION_LOG.md` files are the
authoritative record; their Javadoc sites are now the canonical API surface for
downstream consumers. This **changes Decision 2's reference point**: the
compatibility target for ADAPT is the published Javadoc, not the pinned git
commit hashes currently in `pom.xml`.

ADAPT's pins are therefore stale:
- `IAClassLibrary:fe92f24c6e` — superseded by the modernised tagged/Javadoc
  release.
- `TrackerLibrary:99584ec579` — superseded by the modernised release
  (`4.0.0-SNAPSHOT` in the sibling, tagged per its plan).

### `AdaptDataProcessing` is deprecated (archive candidate)

The [`AdaptDataProcessing` README](https://github.com/djpbarry/AdaptDataProcessing/blob/master/README.md)
now declares the project **obsolete** and recommends a **Python rewrite**
(pandas / NumPy / SciPy / matplotlib) for its analysis. Its three pieces are:

- `DataFileAverager` — the only functional engine (per-timepoint mean/σ across
  track CSVs).
- `DataResampler` — effectively non-functional (`run()` commented out).
- `DetectionMapAnalyser` — TIFF detection-map statistics.

**Impact on ADAPT:**

- The `Bleb_Data_Analysis` plugin is a thin wrapper over
  `AdaptDataProcessing.DataFileAverager`, so one of ADAPT's three registered
  entry points now rests on a deprecated dependency.
- `pom.xml` still pins `AdaptDataProcessing:95d31fcec8`.
- Phase G ("tag all three") and Decision 2 are **moot for `AdaptDataProcessing`**:
  it should not be tagged/fixed, it should be **removed** — with its
  `DataFileAverager` behaviour either reimplemented in ADAPT (or its Python
  equivalent surfaced), or the `Bleb_Data_Analysis` plugin retired.

### Open items this raises

1. Decide the fate of `Bleb_Data_Analysis`: drop it, reimplement
   `DataFileAverager` in ADAPT, or point users at a Python path.
2. Re-point `IAClassLibrary` / `TrackerLibrary` pins to their tagged releases
   using the Javadoc as the compatibility check (completing A2 / Decision 2).
3. Revise `DEVELOPMENT_PLAN.md` Phase G to drop `AdaptDataProcessing` from the
   tagging work.

---

## 2026-09-27 — Review & plan kick-off; M1 build tooling landed (partial)

Initial full review of ADAPT and creation of the modernisation plan
(`AGENTS.md` + `DEVELOPMENT_PLAN.md`). This is the last of the three siblings
to be brought into the coordinated pass, so the plan adopts the
already-resolved sibling decisions (GPL-3.0, JitPack pin-to-tags) but **diverges
deliberately on the Java target** (see Decision 3 below).

### What has already landed (build/tooling half of M1)

| Commit | What |
|---|---|
| `5384b31` | Added `AGENTS.md` (architecture, conventions, gotchas). |
| `97d4fd6` | Added `DEVELOPMENT_PLAN.md` (multi-phase roadmap), refined across the subsequent commits. |
| `ad4a749` | Added the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) pinned to Maven **3.9.16**. |
| `27b4f53` | Removed the vestigial `mvn_settings.xml`; rewrote CI to use the wrapper with a JDK **17/21 matrix**, `checkout@v4` / `setup-java@v4`, and Maven caching. |
| `5fbdf0b` | Added `.gitignore` (`target/`, IDE files, OS files). |
| `e076702` | Added `.gitattributes` (`mvnw` → LF, `*.cmd`/`*.bat` → CRLF) so Linux CI sees correct line endings. |
| `6dbea74` | Bumped parent to `pom-scijava:45.1.0`, declared `central` first in `<repositories>`, and pinned `TrackMate:7.14.0`. |

### Review findings (recorded for the plan — see `DEVELOPMENT_PLAN.md`)

- **License inconsistency (resolved in M1):** `LICENSE` was GPL-3.0 but `pom.xml`
  declared `Simplified BSD License` with `license.licenseName=bsd_2`. Corrected
  to GPL-3.0 in M1 (Decision 1).
- **No tests, no tags, no lint/format tooling.** `mvn verify` is
  compile-only. ADAPT has no git tags at all.
- **Brittle `Analyse_Batch.readParams()`** — positional `Scanner` + hard-coded
  `br.readLine()` skips; silently breaks on reordering (Decision 5: replace
  with JSON).
- **Mutable static/config state:** `GUI.UV` is a genuine static singleton
  (`GUI.getUv()`); `Analyse_Movie` carries many mutated `protected` fields
  through the class hierarchy and protrusion-analysis recursion.
- **Non-deterministic output:** `labels.zip` ROI ordering (Issue #2) — the
  per-frame threads share an unsynchronized `Overlay` (Phase B1).
- **Large dead-code blocks:** `Main.java` is almost entirely commented-out
  experiments; stale comment blocks in `Analyse_Movie`, `BlebAnalyser`,
  `RunnableOutputGenerator`.
- **Mixed-case package dirs** (`Adapt`, `Output`, `Visualisation`, `ui`) —
  Decision 4: rename to lowercase.
- **Effective exception handling missing:** bare `catch (Exception e) { IJ.log(e); }`
  and empty `catch` blocks (Phase A6).

### The five decisions (resolved 2024-09-24, authoritative)

1. **License — GPL-3.0.** Correct `pom.xml` (BSD-2 → GPL-3.0) and the six
   NetBeans header stubs. *(Done in M1.)*
2. **Dependencies — stay on JitPack, pin to tags.** Requires Phase G (tagging
   the three upstream libs) first. *(Open.)*
3. **Target JDK — Java 11 compile target, build on a modern JDK.** This is the
   deliberate divergence from the siblings (both Java 21): ADAPT is a Fiji
   plugin and Fiji "latest" bundles Java 21 at runtime, so targeting 11 keeps
   the plugin compatible. `TrackMate:7.14.0` is the stopgap that makes this
   possible. Future goal: Java 21 + TrackMate 8 as one coordinated bump
   (Phase D5). *(Implemented — build tooling matches.)*
4. **Package names — rename to lowercase.** *(Open.)*
5. **Params file — JSON.** Replace `readParams()` CSV parsing. *(Open.)*

### Early history (pre-modernisation, from git)

| Commit | What |
|---|---|
| `5445fde` | Added an error check to ensure time-lapse data is being input (start of pre-flight validation). |
| `b634de7` | Added an error check to `Analyse_Movie.saveFluorData()`. |
| `2772ba4` | Additional error checks + `pom.xml` update. |
| `843d5f2` | Fixed a bug introduced by `5445fde`. |
| `120a1dd` / `211654c` | `LICENSE` updated to GPL-3.0 (the source of today's BSD-2/GPL-3 contradiction). |

---

## Lessons learned (mistakes to avoid)

These are inherited from the sibling logs and re-stated here only as they apply
to ADAPT. The sibling logs remain the canonical write-ups.

### L1 — Decide the Java target *before* touching build/CI *(inherited)*

The siblings reversed a Java 11 → Java 21 decision mid-stream. ADAPT **has**
settled this (Java 11 now, coordinated Java 21 + TrackMate 8 later — Decision 3),
but the coordinated bump must be a single, planned change, not an incremental
drift.

**Rule:** confirm the cross-project Java / parent-POM / TrackMate version and
dependency pins in writing before any build or CI edit.

### L2 — Use consistent semver tag names *(inherited)*

ADAPT has **no tags** yet. Its version is now `4.0.0-SNAPSHOT` (bumped from the
old-style `3.0.13`); the siblings standardised on `vX.Y.Z`.

**Rule:** adopt `vX.Y.Z` from the first tag onward and decide the major bump
before tagging.

### L3 — Resolve the licence before tagging *(inherited)*

ADAPT had the exact contradiction the siblings fixed: GPL-3.0 `LICENSE` but
BSD-2 `pom.xml`. Fixed in M1 (pom.xml + headers).

**Rule:** align `pom.xml` (`<licenses>`, `license.licenseName`,
`license.copyrightOwners`) with the root `LICENSE` before tagging; treat the
source-header tidy-up as a separate, deferred item.

### L4 — "No references in this repo" ≠ "dead code" *(inherited, lightly)*

Less applicable to a plugin than a library, but the principle mapped onto ADAPT's
large commented-out blocks in `Main.java`/`Analyse_Movie` (since removed in M1):
commented code is historical context, not spec, and is often stale.

**Rule:** strip commented/experimental code (recoverable from git) rather than
leaving it to mislead.

### L5 — Check JitPack compatibility when moving the parent POM *(inherited)*

The parent `pom-scijava:45.1.0` move broke JitPack for the siblings; ADAPT
already worked around one consequence by declaring `central` **first** in
`<repositories>` (commit `6dbea74`).

**Rule:** verify the parent-POM version is consumable via JitPack, and keep
`central` declared first.

### L6 — Experimental code must not leak into `main` *(inherited)*

ADAPT's `Main.java` was almost entirely commented experiments (stripped in M1);
`Analyse_Movie` and others carried large stale blocks (also stripped).

**Rule:** gate experiments behind a flag or branch; strip debug/hardcoded-path
code before merge.

### L7 — Clean up *all* legacy IDE artifacts, not just the obvious ones *(inherited)*

`.gitignore` now covers `.idea/`, `*.iml`, etc., but the NetBeans-generated
`GUI.form` coupling remains (Phase B1).

**Rule:** after removing an IDE/legacy build, sweep for remaining config/output
files.

### L10 — Commit the Maven wrapper with its executable bit set *(inherited)*

ADAPT adopted the wrapper in `ad4a749`. The sibling hit "Permission denied" on
Linux CI because `mvnw` was committed as mode `100644`. ADAPT also added
`.gitattributes` (LF for `mvnw`) in the same pass. The executable bit was
verified in M1: `mvnw` is tracked as `100755`.

**Rule:** confirm `git ls-files -s mvnw` shows `100755` (and LF line endings)
before relying on `./mvnw` in CI. On Windows, set it with
`git update-index --chmod=+x mvnw`.

### L11 — A plugin's build-tooling win is not a completion *(new, ADAPT-specific)*

ADAPT landed the entire build/tooling half of M1 (wrapper, CI, `.gitignore`,
`.gitattributes`, parent bump, TrackMate pin) well before the code-level M1
work (license correction, dead-code removal, error handling — package rename
still deferred to M4). The easy, high-visibility wins came first and could
have been mistaken for a finished M1.

**Rule:** treat "wrapper + CI green" as necessary but far from sufficient;
track code-level milestones explicitly so a green `mvn verify` is not mistaken
for a finished milestone.
