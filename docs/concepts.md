# Concepts & Method

ADAPT quantifies cell migration and membrane morphodynamics from a timelapse of
a fluorescently labelled cell (or cells). The workflow is:

```
segment cells → build maps → detect protrusions/blebs → measure
```

## Segmentation

Cells are detected in the **cytosol channel** using a seeded watershed
(`RegionGrower`). The channel is converted to 8-bit, thresholded, and each cell
is tracked frame-to-frame by seed-following segmentation — there is no
independent linking step. A cytoplasmic channel with a uniform cell against a
relatively uniform background gives the best results.

## Maps

For each cell, ADAPT builds upsampled, boundary-length-normalised maps over time:

- **Curvature map** — the local membrane curvature along the cell boundary.
- **Velocity map** — the membrane protrusion/retraction velocity (µm/min),
  computed from boundary movement.
- **Signal map** — the fluorescence intensity of the signal channel mapped onto
  the boundary.

These maps are the core abstraction passed between the analysis libraries and
the reporting/visualisation code.

## Protrusions vs blebs

Protrusion analysis runs in one of two modes:

- **Blebs** (velocity-based) — detected from the velocity map.
- **Filopodia** (morphology-based) — detected from the membrane geometry.

Both produce per-protrusion measurements (length, velocity, signal) in addition
to the whole-cell metrics.

## Cell migration

Cell centroids across frames are extracted into `trajectories.csv`, from which
migration statistics (speed, directionality, persistence) are derived.

## Reference

For the underlying method, see the publication cited on the [front page](index).
