/*
 * Copyright (C) 2026 David Barry <david.barry at cancer.org.uk>
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
package net.calm.adapt.output;

import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IAClasses.Region;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Exports ADAPT cell trajectories in the Ultrack CSV format consumed by
 * inTRACKtive ({@code track_id, t, z, y, x, parent_track_id}).
 *
 * <p>ADAPT tracks one cell per {@link CellData}; each cell is an independent,
 * non-dividing track, so {@code parent_track_id} is always {@code -1} (no
 * parent). Coordinates are the raw pixel centroids produced by the analysis
 * (Ultrack's native voxel space); {@code z} is {@code 0} because ADAPT analyses
 * 2D (Z=1) timelapses.</p>
 */
public final class UltrackCsvWriter {

    public static final String[] HEADINGS = {"track_id", "t", "z", "y", "x", "parent_track_id"};
    private static final double NO_PARENT = -1.0;

    private UltrackCsvWriter() {
    }

    /**
     * Writes the Ultrack-format tracks CSV for the given cells.
     *
     * @param cells      the analysed cells (one track each)
     * @param minLength  cells whose frame span is at most this are excluded
     * @param outputFile destination CSV file
     */
    public static void saveTracks(ArrayList<CellData> cells, double minLength, File outputFile) throws IOException {
        CsvWriter.saveValues(buildTrackRows(cells, minLength), outputFile, HEADINGS, null, false);
    }

    /**
     * Builds one Ultrack row per cell per frame, ordered by cell then frame.
     */
    static double[][] buildTrackRows(ArrayList<CellData> cells, double minLength) {
        ArrayList<double[]> rows = new ArrayList<>();
        for (int n = 0; n < cells.size(); n++) {
            CellData cell = cells.get(n);
            if (cell.getLength() <= minLength) {
                continue;
            }
            Region[] regions = cell.getCellRegions();
            if (regions == null) {
                continue;
            }
            for (int t = cell.getStartFrame() - 1; t < cell.getEndFrame(); t++) {
                Region region = regions[t];
                if (region == null) {
                    continue;
                }
                ArrayList<float[]> centres = region.getCentres();
                if (centres.isEmpty()) {
                    continue;
                }
                float[] centre = centres.get(centres.size() - 1);
                rows.add(buildRow(n, t, centre[0], centre[1]));
            }
        }
        return rows.toArray(new double[0][]);
    }

    /**
     * Builds a single Ultrack row in column order
     * {@code track_id, t, z, y, x, parent_track_id}.
     */
    static double[] buildRow(int trackId, int t, double x, double y) {
        return new double[]{trackId, t, 0.0, y, x, NO_PARENT};
    }
}
