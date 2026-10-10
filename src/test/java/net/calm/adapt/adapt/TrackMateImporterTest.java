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

import fiji.plugin.trackmate.Spot;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IAClasses.Region;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TrackMateImporterTest {

    @Test
    void buildRegionUsesSpotCentreWhenNoContour() {
        Region region = TrackMateImporter.buildRegion(100, 100, 50, 60, 10, null);

        assertNotNull(region);
        float[] centre = region.getCentres().get(0);
        assertEquals(50, Math.round(centre[0]));
        assertEquals(60, Math.round(centre[1]));
    }

    @Test
    void buildCellMapsEachSpotToAFrame() {
        Spot s0 = spot(0, 10, 10);
        Spot s1 = spot(1, 12, 10);
        Spot s2 = spot(2, 14, 10);

        CellData cell = TrackMateImporter.buildCell(Arrays.asList(s0, s1, s2), 100, 100);

        assertEquals(1, cell.getStartFrame());
        assertEquals(3, cell.getEndFrame());
        assertEquals(3, cell.getLength());

        Region[] regions = cell.getCellRegions();
        assertNotNull(regions);
        assertEquals(3, regions.length);
        assertNotNull(regions[0]);
        assertNotNull(regions[1]);
        assertNotNull(regions[2]);
        assertEquals(10, Math.round(regions[0].getCentres().get(0)[0]));
        assertEquals(12, Math.round(regions[1].getCentres().get(0)[0]));
        assertEquals(14, Math.round(regions[2].getCentres().get(0)[0]));
    }

    private static Spot spot(int frame, double x, double y) {
        Spot s = new Spot(x, y, 0.0, 5.0, 1.0);
        s.putFeature(Spot.FRAME, (double) frame);
        return s;
    }
}
