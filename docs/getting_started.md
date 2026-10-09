# Getting Started

## Installation

1. **Install Fiji** — download the latest Fiji from
   <https://fiji.sc/>. ADAPT targets **Java 21**, so use a Fiji build that runs
   on Java 21 (recent "latest" builds do).
2. **Enable the ADAPT update site** — in Fiji, open
   *Help ▸ Update…*, click *Manage update sites*, and tick the **ADAPT** site.
   Apply the changes and restart Fiji.
3. **Install the required dependency** — ADAPT's segmentation needs MorphoLibJ,
   which is *not* part of base Fiji. Enable the **IJPB-plugins** update site.

   If it is missing, ADAPT reports a clear error at launch.

## Test data

Sample image stacks ship under
[`test_data/`](https://github.com/djpbarry/Adapt/tree/master/test_data) (Git
LFS). There are two datasets:

- `blebbing_cell` — a blebbing cell with a signal channel.
- `migrating_cell` — a migrating cell.

## Quick start

1. Open a multi-channel timelapse hyperstack (Z=1, T>1) in Fiji.
2. Run **Plugins ▸ Adapt ▸ Analyse Movie**.
3. Pick the cytosol and signal channels in the dialog.
4. Click **Preview** to check the segmentation overlay, then **Run**.

![Channel selection](_static/Channel_Selection.PNG)

## Tutorial

A walkthrough with the test data is available on YouTube:

[![ADAPT tutorial](_static/YouTube.png)](https://youtu.be/TWD4mrTnXvk)
