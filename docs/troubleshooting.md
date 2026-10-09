# Troubleshooting

## "Required dependency is missing"

ADAPT's segmentation needs **MorphoLibJ**, which is not in base Fiji. Enable the
**IJPB-plugins** update site in *Help ▸ Update… ▸ Manage update sites*, apply,
and restart Fiji.

## "The active image must be a hyperstack..."

*Analyse Movie* expects the active window to be a single multi-channel timelapse
hyperstack (Z=1, T>1). If you see this message, open a hyperstack first (or use
*Batch Analysis* for a directory of files).

## No cells are detected

- Make sure the **cytosol channel** has a uniform cell against a relatively
  uniform background.
- Try adjusting **Grey Level Threshold** (or switch to **Auto Threshold**).
- Check **Minimum Object Size (µm²)** and **Minimum Trajectory Length**.

## Java 21

ADAPT targets **Java 21**. If the plugin fails to load, make sure Fiji is running
on a Java 21 runtime.

## Curvature extrema markers are missing

The yellow curvature-extrema markers in the preview are currently absent because
the curvature calculation is unsigned. Bleb detection is unaffected (it is
velocity-based). This is a known issue tracked in the repository.

## Reporting problems

Please open an issue at
<https://github.com/djpbarry/Adapt/issues>, or ask on the
[Image.sc forum](https://forum.image.sc/tags/adapt).
