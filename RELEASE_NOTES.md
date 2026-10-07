# ADAPT Release Notes

User-facing changelog: what changed and which changes affect behaviour. For the
internal, dated engineering record (decisions, mistakes, lessons) see
[`REVISION_LOG.md`](REVISION_LOG.md).

---

## Unreleased (next public release)

### Overview

ADAPT has been modernised: the codebase now targets **Java 21**, the two sibling
libraries (`IAClassLibrary`, `TrackerLibrary`) have been updated, and the on-disk
output has been restructured into a cleaner, more portable layout.

### Requirements / upgrade notes

- **Java 21** — Fiji must run on a Java 21 runtime.
- **TrackMate 8** — resolved transitively via the updated `IAClassLibrary`.
- Existing scripts that referenced the old output paths or column headings will
  need updating (see "Behaviour-changing changes" below).

### Behaviour-changing / breaking changes

- **Output layout renamed.**
  - `Population_Data_Output` → `tables/`
  - `Individual_Cell_Data_Output` → `images/`
  - per-cell folders are now zero-padded `images/cell_NNN/`.
- **CSV column names are lowercase snake_case ASCII** (e.g. `time_s`, `v_um_s`,
  `cell_id`); the old mixed-case and `µ` headings are gone.
- **`parameters.json` + `README.md` replace `properties.xml`** at the output root.
- **Per-cell tables are merged into tidy long-format files** — `velocity.csv`,
  `boundary.csv`, `blebs.csv` — each with `cell_id`/`bleb_id` columns.
- **Visualisations are multi-page TIFF stacks** (`velocity_visualisation.tif`,
  `curvature_visualisation.tif`) instead of many per-frame `NNN.tiff` files.
- **`labels.zip` ROI order is now deterministic.**
- **The GUI is non-modal** — analysis starts when you click Run, and the source
  image windows stay interactive.
- **Parameter-file format** — the old positional `params.csv` is no longer
  supported; use the versioned `params.json` (see
  `src/main/resources/params.example.md`).
- **Single-hyperstack input.** `Analyse Movie` no longer asks for two separate
  image windows — it uses the active image window as a single multi-channel
  timelapse hyperstack (Z=1, T>1), and you choose the cytosol and signal
  channels in the dialog (they may be the same channel).

### Improvements

- **Non-destructive previews.** The segmentation preview is drawn as an overlay
  on the original image windows; input pixel data is never modified. Preview is
  now a single-click button rather than a toggle.
- **Tooltips** on every parameter.
- **Reproducible output baseline** — `bin/compute-baseline.cmd` and
  `bin/verify-baseline.cmd` SHA-256 the deterministic text outputs.
- **Concurrency model documented** (no behaviour change).

### Bug fixes

- Fixed the preview hiding the source image windows during automatic seeding.

### Known issues

- **Curvature-extrema markers are missing from the preview** because the
  curvature calculation is unsigned. Bleb detection is unaffected (it is
  velocity-based). See `DEVELOPMENT_PLAN.md` / `REVISION_LOG.md`.
