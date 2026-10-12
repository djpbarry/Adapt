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

import ij.process.ByteProcessor;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IAClasses.Region;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UltrackCsvWriterTest {

    @Test
    void buildRowMapsColumnsToUltrackOrder() {
        double[] row = UltrackCsvWriter.buildRow(2, 5, 11.5, 22.5);

        assertArrayEquals(new double[]{2, 5, 0, 22.5, 11.5, -1}, row, 1e-9);
    }

    @Test
    void buildTrackRowsEmitsOneRowPerFrameWithPixelCoordinates() {
        ArrayList<CellData> cells = new ArrayList<>();
        cells.add(cell(1, 3, new double[][]{{10, 20}, {12, 20}, {14, 20}}));

        double[][] rows = UltrackCsvWriter.buildTrackRows(cells, 0);

        assertEquals(3, rows.length);
        assertArrayEquals(new double[]{0, 0, 0, 20, 10, -1}, rows[0], 1e-9);
        assertArrayEquals(new double[]{0, 1, 0, 20, 12, -1}, rows[1], 1e-9);
        assertArrayEquals(new double[]{0, 2, 0, 20, 14, -1}, rows[2], 1e-9);
    }

    @Test
    void buildTrackRowsSkipsCellsBelowMinLength() {
        ArrayList<CellData> cells = new ArrayList<>();
        cells.add(cell(1, 2, new double[][]{{10, 20}, {12, 20}}));

        double[][] rows = UltrackCsvWriter.buildTrackRows(cells, 3);

        assertEquals(0, rows.length);
    }

    private static CellData cell(int startFrame, int endFrame, double[][] centres) {
        CellData cell = new CellData(startFrame);
        cell.setEndFrame(endFrame);
        Region[] regions = new Region[endFrame];
        for (int i = 0; i < centres.length; i++) {
            regions[startFrame - 1 + i] = region(centres[i][0], centres[i][1]);
        }
        cell.setCellRegions(regions);
        return cell;
    }

    private static Region region(double cx, double cy) {
        ByteProcessor mask = new ByteProcessor(64, 64);
        mask.setColor(Region.MASK_BACKGROUND);
        mask.fill();
        mask.setColor(Region.MASK_FOREGROUND);
        mask.fillOval((int) Math.round(cx) - 5, (int) Math.round(cy) - 5, 11, 11);
        return new Region(mask, new short[]{(short) Math.round(cx), (short) Math.round(cy)});
    }
}
