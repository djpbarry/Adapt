# ADAPT Development Plan

This plan outlines a multi-phase effort to (a) make ADAPT more robust and
maintainable, (b) improve the user experience, and (c) overhaul the
documentation (migrating from the GitHub wiki to ReadTheDocs). It is grounded in
the current state of the codebase as of the plan's writing.

## Current state (context for the plan)

- ADAPT is a Fiji/ImageJ plugin (`net.calm.adapt`) that analyses cell migration,
  membrane protrusions, and correlated fluorescence intensity.
- The heavy lifting — segmentation (`RegionGrower`), curvature computation,
  particle/trajectory tracking (`TrajectoryAnalysis`, `TrajectoryBuilder`),
  Bio-Formats I/O, and CSV writing — lives in three external libraries pulled
  from JitPack: `IAClassLibrary`, `TrackerLibrary`, `AdaptDataProcessing`
  (pinned to git commit hashes in `pom.xml`). ADAPT also contains substantial
  in-repo domain logic (protrusion/bleb/fluorescence analysis and
  cell-trajectory extraction), so it is not purely orchestration/glue.
- The build is Maven 3 with `org.scijava:pom-scijava:37.0.0` as parent, JDK 11
  (per CI). `mvn_settings.xml` wires up GitHub Packages but is vestigial (see
  Decision 2) — no declared dependency resolves from it.
- There are **no unit tests**, **no lint/format tooling**, and **no `.gitignore`**.
- The GUI is a NetBeans-generated `JDialog` (`ui/GUI.java` + `ui/GUI.form`), with
  parameters held in a single **static** `UserVariables` instance.
- Documentation currently lives in the GitHub wiki and a short `README.md`, with
  screenshots under `content/` and a zip of test data under `test_data/`.

---

## Phase A — Robustness & maintainability

### A1. Modernise the build and CI

1. **Pin and commit a reproducible toolchain** — add a Maven wrapper
   (`mvnw[.cmd]`) so builds don't depend on a system Maven of an unknown version.
2. **Confirm the JDK target** — Decision 3 is to compile to Java 11 on a modern
   JDK. Keep CI's source/target at 11; only revisit if a dependency (e.g.
   TrackMate v8, see Phase D) requires newer.
3. **Harden CI** (`.github/workflows/maven.yml`):
   - Add a matrix over supported JDKs, or at least pin the exact one used.
   - Add caching for Maven dependencies to speed up runs.
   - Split into distinct jobs: `build`, `test` (future), `docs` (see Phase C).
4. **Add a `.gitignore`** covering `target/`, `.idea/`, `*.iml`, and OS files.

### A2. Fix the dependency pinning problem

1. The three JitPack dependencies are pinned to raw commit hashes
   (`fe92f24c6e`, `99584ec579`, `95d31fcec8`). Per Decision 2, promote them to
   tagged releases in their own repos and pin `pom.xml` to those tags (still on
   JitPack, no vendoring). Until that lands, document in the POM *which* commit
   each hash refers to and why.

### A3. Introduce tests (the single biggest maintainability win)

1. **Add a test framework** (JUnit 5 via the SciJava parent's conventional test
   setup) and wire `mvn verify` to run it.
2. **Start with the highest-value, lowest-cost targets** — the pure-logic,
   static-method classes already extracted:
   - `CurveMapAnalyser` (curvature-**extrema** detection + tracking of curvature
     minima via `TrackerLibrary`; the `calcScaledCurveRange`/`isLocalCurvatureExtreme`
     helpers are the extractable pure parts — the top-level method is not pure)
   - `BlebAnalyser` (boundary/anchoring math — hard to test because it touches
     `ImageProcessor`/`MorphMap`)
   - `FluorescenceDistAnalyser` (GLCM statistics — `calcGlcmStats()` is pure, but
     `constructGLCM()`/`setStats()` operate on `ImageProcessor`/`ImageStatistics`)
   - `Analyse_Batch.readParams()` (positional CSV parsing).
3. **Add golden-file tests** for CSV output: run the pipeline against
   `test_data/ADAPT_Test_Data.zip` and assert the output schema/headings are
   stable. This catches the silent schema-drift risk flagged in `AGENTS.md`.
4. **Make code testable first** — extract pure logic from god methods (see A4)
   so tests don't require a running ImageJ.

### A4. Refactor for clarity and testability

Targets, priority-ordered:

1. **`Analyse_Movie.analyse()`** and **`RunnableOutputGenerator.buildOutput()`**
   — these are the largest, most intertwined methods. Decompose into
   single-responsibility private methods (segmentation, map building, protrusion
   analysis, output writing) and move pure math into package-private/static
   helpers.
2. **`Analyse_Batch.readParams()`** — per Decision 5, replace the brittle
   positional `Scanner` + `br.readLine()` parsing with JSON (validated via
   Jackson), including a schema/version field so old files are detected and
   rejected with a clear message.
3. **Reduce mutable static/config state** — `GUI.UV` is a genuine static
   singleton (returned by `GUI.getUv()`), so all runs share one `UserVariables`
   instance. `Analyse_Movie` also carries many `protected` *instance* fields that
   are mutated through the class hierarchy and the protrusion-analysis recursion,
   making state hard to reason about. Introduce an explicit per-run "context"
   object passed down instead of relying on statics/field mutation.
4. **Normalise the two concurrency abstractions** — `NotificationThread` (in
   repo) and `MultiThreadedProcess`/`RunnableProcess` (external) are unrelated.
   Consolidate on one, or document/enforce which to use where.
5. **Remove dead code** — `Main.java` is almost entirely commented-out
   experiments; `Analyse_Movie`, `BlebAnalyser`, and `RunnableOutputGenerator`
   contain large stale comment blocks. Delete them (they're recoverable from
   git) and strip unused imports.
6. **Normalise license headers** — per Decision 1, GPL-3.0 is authoritative.
   Replace the six NetBeans "change this header" stubs with the GPL header and
   correct `pom.xml` (currently declares BSD-2; see Decision 1).

### A5. Add static analysis and formatting

1. Add a formatter (e.g. Spotless with a standard Java style) and a linter
   (SpotBugs / PMD as appropriate) wired into `mvn verify`.
2. Fix the mixed-case package-directory convention (`Adapt`, `Output`,
   `Visualisation`, `ui`) — per Decision 4, rename to lowercase and update
   `plugins.config` + all imports.

### A6. Error handling & user-facing failure modes

1. Replace bare `catch (Exception e) { IJ.log(e); }` and empty `catch` blocks
   (e.g. the version-property load in `run()`) with structured, actionable
   messages.
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

### B1. Rework the GUI

1. **Replace the NetBeans `.form` coupling** — the hand-versus-generator split
   between `GUI.java` and `GUI.form` is risky to edit. Migrate to a hand-managed
   layout (GridBag/GroupLayout written by hand) so the UI is version-controllable
   and diffable.
2. **Eliminate the static `UserVariables` singleton** — pass a `UserVariables`
   instance explicitly; this fixes a class of bugs from stale/shared state across
   sessions.
3. **Group parameters into collapsible sections** mirroring the *Simple /
   Advanced / Protrusions* screenshots already in `content/`, and add tooltips
   or inline help for every parameter (wording drawn from `StaticVariables`).
4. **Add validation and sane defaults** at the UI layer: numeric ranges, required
   fields, and a "load/save parameter preset" feature (the raw material exists in
   `Analyse_Batch.readParams()`).
5. **Add cancellation & progress** — the GUI has a Cancel button, but it only
   `dispose()`s the setup dialog; the background analysis threads
   (`MultiThreaded*` generators) have no cancellation path. Wire the existing
   `NotificationThread`/`TaskListener` and `MultiThreadedProcess` mechanisms to a
   progress dialog whose Cancel actually interrupts the running analysis.

### B2. Onboarding & output UX

1. **First-run / help flow** — link the tutorial and docs directly from the GUI.
2. **Output organisation** — preserve the current on-disk structure (the
   `Output_Folder_Structure.PNG` screenshot documents it), but document each file
   in-product (a `README.txt` written into the output folder).
3. **Consistent exit messaging** — unify the `IJ.showStatus`/`IJ.log` completion
   messages and always report elapsed time and output location.

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
  YouTube video + `test_data/ADAPT_Test_Data.zip`).
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
TrackMate ≥ 8 requires **Java 21**, whereas ADAPT has just agreed to target
**Java 11** (Decision 3), so any *runtime* TrackMate coupling reopens that
decision (or pins TrackMate to v7).

### D4. Constraints to confirm before any compile-time dependency

1. **Java target** — TrackMate ≥ 8 needs Java 21; v7 targets older JVMs. Stage 1
   (XML-only) avoids this entirely.
2. **License** — ADAPT is GPL-3.0 (Decision 1). Verify TrackMate's license is
   compatible before adding `sc.fiji:TrackMate` as a compile dependency.

---

## Suggested sequencing & milestones

1. **M1 — Foundations (low risk, high value):** `.gitignore`, Maven wrapper, CI
   hardening, license-header consistency, delete dead code. (Phase A1, A2, A4.5,
   A5.5, A6)
2. **M2 — Test harness:** JUnit + a couple of unit tests + golden-file output
   test. (Phase A3)
3. **M3 — Docs migration:** stand up Sphinx/RTD, migrate wiki content. (Phase C)
4. **M4 — Refactor core:** decompose `analyse()`/`buildOutput()`, replace
   `readParams()` with JSON, remove static state, rename packages. (Phase A4,
   A5.2)
5. **M5 — GUI & UX:** hand-managed layout, parameter presets, progress/cancel.
   (Phase B)
6. **M6 — Distribution:** update site, semver, in-product help links. (Phase B3)
7. **M7 — TrackMate interop:** Stage-1 XML import/export bridge (Phase D1);
   Stage-2 `TrackAnalyzer` module only after M4 lands.

Each milestone is independently shippable and testable; M1–M3 can proceed in
parallel. Package renaming (Q4) should be done early in M4 before it cascades
into other work. M7's Stage-1 bridge is independent of the Java-target decision
and can be tackled earlier if desired; Stage-2 depends on M4's model-agnostic
refactor.

## Decisions (resolved open questions)

These were resolved with the maintainer on 2024-09-24 and are the authoritative
input for the phases above.

1. **License — GPL-3.0.** The root `LICENSE` and the majority of source headers
   already declare GPL-3.0; `pom.xml`'s `Simplified BSD` (and the
   `license.licenseName=bsd_2` property) is stale metadata and must be corrected
   to GPL-3.0. Replace the six NetBeans "change this header" stubs with the GPL
   header. Confirm the correct `license.copyrightOwners` (Francis Crick
   Institute / David Barry) while doing so.
2. **Dependencies — stay on JitPack, pin to tags.** Do not vendor. Do not use
   GitHub Packages. Tag each of `IAClassLibrary`, `TrackerLibrary`,
   `AdaptDataProcessing` with a release in its own repo; JitPack resolves tagged
   versions auth-free. Update the three `pom.xml` versions from commit hashes to
   those tags.
   - **`mvn_settings.xml` / GitHub Packages is vestigial.** Investigation shows
     only the CI workflow references it; no `pom.xml` dependency resolves from
     `maven.pkg.github.com`. Remove `mvn_settings.xml` and the `PAT`/`--settings`
     wiring from `maven.yml` (verify with one clean CI run before deleting).
3. **Target JDK — Java 11 (compile target), build on a modern JDK.** This
   matches the current `pom-scijava` convention (`scijava.jvm.version=11`).
   Recent Fiji "latest" bundles Java 21 at runtime, so building on JDK 21 and
   targeting 11 keeps the plugin compatible while staying current.
4. **Package names — rename to lowercase.** Rename `Adapt`, `Output`,
   `Visualisation`, `ui` to conventional lowercase (`adapt`, `output`,
   `visualisation`, `ui`); update `plugins.config` and all imports accordingly.
5. **Params file — JSON.** Replace the positional CSV parsing in
   `Analyse_Batch.readParams()` with JSON (validated via Jackson, adding it as a
   dependency if it is not already transitive via Bio-Formats/SciJava),
   including a schema/version for forward-compatibility.
