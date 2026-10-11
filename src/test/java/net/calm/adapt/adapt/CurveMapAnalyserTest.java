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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void signedMengerCurvatureIsPositiveForLeftTurn() {
        assertTrue(CurveMapAnalyser.signedMengerCurvature(1, 0, 0, 0, 1, 1) > 0.0);
    }

    @Test
    void signedMengerCurvatureIsNegativeForRightTurn() {
        assertTrue(CurveMapAnalyser.signedMengerCurvature(1, 1, 0, 1, 1, 0) < 0.0);
    }

    @Test
    void signedMengerCurvatureIsZeroForCollinearPoints() {
        assertEquals(0.0, CurveMapAnalyser.signedMengerCurvature(1, 0, 0, 0, 2, 0), 1e-9);
    }

    @Test
    void calcSignedCurvatureMatchesCircleRadiusAndSign() {
        int n = 12;
        double radius = 100.0;
        short[][] circle = new short[n][2];
        for (int i = 0; i < n; i++) {
            double theta = 2.0 * Math.PI * i / n;
            circle[i][0] = (short) Math.round(radius * Math.cos(theta));
            circle[i][1] = (short) Math.round(radius * Math.sin(theta));
        }
        double[] curvature = CurveMapAnalyser.calcSignedCurvature(circle, 1);
        double expected = 1.0 / radius;
        boolean positive = curvature[0] > 0.0;
        for (int i = 0; i < n; i++) {
            assertTrue((curvature[i] > 0.0) == positive);
            assertEquals(expected, Math.abs(curvature[i]), expected * 0.2);
        }
    }
}
