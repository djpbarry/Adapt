# User Guide

## Workflow

1. Open a multi-channel timelapse hyperstack (Z=1, T>1) in Fiji.
2. Run **Plugins ▸ Adapt ▸ Analyse Movie**.
3. Choose the **cytosol** and **signal** channels from the dropdowns at the top
   of the dialog (they may be the same channel).
4. Click **Preview** to render a segmentation overlay on the source image.
5. Adjust parameters and click **Run**.

The dialog is non-modal, so the source image windows stay interactive while you
tune the parameters. **Save Preset** / **Load Preset** round-trip the current
settings as a `params.json` file.

The analysis runs on a background thread; a progress dialog shows the current
phase and lets you **Cancel**.

## Using existing TrackMate tracks

If you have already segmented and tracked your cells in
[TrackMate](https://imagej.net/plugins/trackmate/), you can skip ADAPT's own
segmentation and analyse the TrackMate boundaries instead:

1. Save your TrackMate session as an XML file (the `.xml` written by
   *Save* in the TrackMate dialog).
2. Open the matching movie as a multi-channel timelapse hyperstack in Fiji.
3. Run **Plugins ▸ Adapt ▸ Analyse TrackMate File** and select the XML file.
4. Choose the cytosol/signal channels and parameters as usual, then **Run**.

ADAPT reads one cell per TrackMate track and one boundary per spot (using the
spot contour, or a circle from the spot radius when no contour was saved), then
runs the same velocity/curvature/signal and protrusion/bleb analysis. Protrusion
detection and output layout are identical to **Analyse Movie**.

## Parameters

### Simple tab

- **Grey Level Threshold** — manual grey-level threshold used for segmentation
  when *Auto Threshold* is off.
- **Auto Threshold** — automatically determine the segmentation threshold using
  the method chosen in the Advanced tab.
- **Spatial Resolution (µm/pixel)** — the image pixel size, used to convert
  pixels to physical units.
- **Frames per Minute** — the acquisition rate, used for velocity and time axes.
- **Minimum Object Size (µm²)** — the smallest area considered a cell.
- **Minimum Trajectory Length** — the minimum number of frames a cell must be
  tracked to be analysed.
- **Generate Visualisations** — write velocity/curvature visualisation stacks.
- **Generate Morphology Data** — write per-frame morphology measurements.
- **Generate Signal Distribution** — write the fluorescence (GLCM) measurements.

### Advanced tab

- **Erosion Iterations** — erosion passes used to refine the segmentation border.
- **Smoothing Filter Radius** — Gaussian blur applied before segmentation.
- **Spatial Filter Radius (µm)** / **Temporal Filter Radius (s)** — smoothing
  applied to the derived maps.
- **Thresholding Method** — the algorithm used when *Auto Threshold* is on.
- **Cortex Depth (µm)** — width of the cortex band sampled for velocity and
  signal.
- **Visualisation Line Thickness** — line width used when drawing overlays and
  visualisations.

### Protrusion Analysis tab

- **Analyse Individual Protrusions** — run per-protrusion analysis.
- **Detect Blebs** / **Detect Filopodia** — choose velocity-based bleb detection
  or morphology-based filopodia detection.
- **Curvature Window** — window size for curvature calculation.
- **Min Curvature Threshold** — threshold for curvature extrema.
- **Signal Threshold Factor** — multiplier of the signal standard deviation used
  as the detection threshold.
- **Signal Map Threshold** — threshold applied to the protrusion signal maps.
- **Cut-Off Time (s)** — time cut-off for protrusion analysis.
- **Max/Min Filopodia Size (µm²)** — filopodia area bounds (morphology mode).
- **Display Plots** — show protrusion analysis plots.

## Output structure

Each run writes a `<name>_Output/` directory:

```
<name>_Output/
├── parameters.json        # run parameters
├── README.md              # schema + file manifest
├── tables/                # tidy long-format CSVs (UTF-8, snake_case)
│   ├── trajectories.csv
│   ├── morphology.csv
│   ├── velocity.csv
│   ├── boundary.csv
│   ├── fluorescence.csv
│   └── blebs.csv
├── images/                # maps + visualisations per cell
│   └── cell_000/
│       ├── velocity_visualisation.tif   # multi-page stack
│       ├── curvature_visualisation.tif
│       └── ...
└── labels.zip             # ImageJ ROI overlay
```

![Output folder structure](_static/Output_Folder_Structure.PNG)
