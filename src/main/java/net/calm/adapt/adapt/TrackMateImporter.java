/*
 * Copyright (C) 2014 David Barry <david.barry at cancer.org.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.calm.adapt.adapt;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IAClasses.Region;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports a TrackMate session XML into ADAPT {@link CellData} objects. Each
 * TrackMate track becomes one cell; each spot becomes one per-frame
 * {@link Region} whose boundary comes from the spot's {@link SpotRoi} contour
 * (falling back to a circle from its RADIUS when no contour exists).
 */
public class TrackMateImporter {

    private TrackMateImporter() {
    }

    /**
     * Reads a TrackMate session XML and converts its tracks into ADAPT cells.
     *
     * @param xmlFile     the TrackMate XML file
     * @param imageWidth  the image width in pixels (for mask rasterisation)
     * @param imageHeight the image height in pixels
     */
    public static List<CellData> importTracks(File xmlFile, int imageWidth, int imageHeight) {
        TmXmlReader reader = new TmXmlReader(xmlFile);
        if (!reader.isReadingOk()) {
            throw new IllegalArgumentException("Invalid TrackMate XML: " + reader.getErrorMessage());
        }
        Model model = reader.getModel();
        if (model == null) {
            throw new IllegalArgumentException("No model found in TrackMate XML.");
        }
        TrackModel trackModel = model.getTrackModel();
        List<CellData> cells = new ArrayList<>();
        for (Integer trackId : trackModel.trackIDs(false)) {
            List<Spot> spots = new ArrayList<>(trackModel.trackSpots(trackId));
            spots.sort(Spot.frameComparator);
            if (!spots.isEmpty()) {
                cells.add(buildCell(spots, imageWidth, imageHeight));
            }
        }
        return cells;
    }

    private static CellData buildCell(List<Spot> spots, int imageWidth, int imageHeight) {
        // TrackMate FRAME is 0-based; ADAPT uses 1-based frame numbers
        // (ImageJ stack slices are 1-based).
        int startFrame = spots.get(0).getFeature(Spot.FRAME).intValue() + 1;
        int endFrame = spots.get(spots.size() - 1).getFeature(Spot.FRAME).intValue() + 1;
        CellData cell = new CellData(startFrame);
        cell.setEndFrame(endFrame);
        cell.setImageWidth(imageWidth);
        cell.setImageHeight(imageHeight);
        Region[] regions = new Region[endFrame];
        for (Spot spot : spots) {
            int frame = spot.getFeature(Spot.FRAME).intValue() + 1;
            double x = spot.getDoublePosition(0);
            double y = spot.getDoublePosition(1);
            double radius = spot.getFeature(Spot.RADIUS);
            regions[frame - 1] = buildRegion(imageWidth, imageHeight, x, y, radius, spot.getRoi());
        }
        cell.setCellRegions(regions);
        return cell;
    }

    private static Region buildRegion(int width, int height, double x, double y, double radius, SpotRoi roi) {
        ByteProcessor mask = new ByteProcessor(width, height);
        mask.setColor(Region.MASK_BACKGROUND);
        mask.fill();
        mask.setColor(Region.MASK_FOREGROUND);
        if (roi != null && roi.x.length >= 3) {
            int n = roi.x.length;
            int[] xp = new int[n];
            int[] yp = new int[n];
            for (int i = 0; i < n; i++) {
                xp[i] = (int) Math.round(x + roi.x[i]);
                yp[i] = (int) Math.round(y + roi.y[i]);
            }
            mask.fill(new PolygonRoi(xp, yp, n, Roi.POLYGON));
        } else {
            int r = Math.max(1, (int) Math.round(radius));
            mask.fillOval((int) Math.round(x - radius), (int) Math.round(y - radius), 2 * r, 2 * r);
        }
        return new Region(mask, new short[]{(short) Math.round(x), (short) Math.round(y)});
    }
}
