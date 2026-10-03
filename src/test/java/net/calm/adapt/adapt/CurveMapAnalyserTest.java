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
package net.calm.adapt.adapt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

public class CurveMapAnalyserTest {

    @Test
    void calcScaledCurveRangeScalesAndRounds() {
        assertEquals(40, CurveMapAnalyser.calcScaledCurveRange(10.0, 1.0));
        assertEquals(4, CurveMapAnalyser.calcScaledCurveRange(1.0, 1.0));
        assertEquals(2, CurveMapAnalyser.calcScaledCurveRange(1.0, 0.5));
        assertEquals(1, CurveMapAnalyser.calcScaledCurveRange(0.3, 1.0));
        assertEquals(0, CurveMapAnalyser.calcScaledCurveRange(0.1, 1.0));
    }

    @Test
    void isLocalCurvatureExtremeDetectsLocalMinimum() {
        double[] curve = {0.0, -1.0, 0.0};
        assertEquals(0, CurveMapAnalyser.isLocalCurvatureExtreme(1, 1, curve, 0.5, true));
    }

    @Test
    void isLocalCurvatureExtremeRejectsFlatRegion() {
        double[] curve = {0.0, 0.0, 0.0};
        assertNotEquals(0, CurveMapAnalyser.isLocalCurvatureExtreme(1, 1, curve, 0.5, true));
    }

    @Test
    void isLocalCurvatureExtremeWrapsAcrossBoundary() {
        double[] curve = {-1.0, 0.0, 0.0};
        assertEquals(0, CurveMapAnalyser.isLocalCurvatureExtreme(0, 1, curve, 0.5, true));
    }

    @Test
    void isLocalCurvatureExtremeDetectsLocalMaximum() {
        double[] curve = {0.0, 1.0, 0.0};
        assertEquals(0, CurveMapAnalyser.isLocalCurvatureExtreme(1, 1, curve, 0.5, false));
    }
}
