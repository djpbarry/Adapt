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

import ij.IJ;
import net.calm.iaclasslibrary.UserVariables.UserVariables;

/**
 * Static, GUI-free validation of an {@link UserVariables} instance. Extracted
 * from {@code GUI.validateRanges()} so the interactive and macro/headless paths
 * apply the same range checks.
 */
public class ParameterValidator {

    /**
     * Validates the numeric ranges of the given parameters, reporting the first
     * violation via {@link IJ#error(String)}.
     *
     * @param uv the parameters to validate
     * @return {@code true} if every range check passes
     */
    public static boolean validate(UserVariables uv) {
        if (uv.getSpatialRes() <= 0) {
            IJ.error("Spatial Resolution must be greater than 0.");
            return false;
        }
        if (uv.getTimeRes() <= 0) {
            IJ.error("Frames per Minute must be greater than 0.");
            return false;
        }
        if (uv.getErosion() < 0) {
            IJ.error("Erosion Iterations must be 0 or more.");
            return false;
        }
        if (uv.getCortexDepth() <= 0) {
            IJ.error("Cortex Depth must be greater than 0.");
            return false;
        }
        if (uv.getMinLength() < 1) {
            IJ.error("Minimum Trajectory Length must be at least 1.");
            return false;
        }
        if (uv.getMorphSizeMin() <= 0) {
            IJ.error("Minimum Object Size must be greater than 0.");
            return false;
        }
        if (uv.getVisLineWidth() < 1) {
            IJ.error("Visualisation Line Thickness must be at least 1.");
            return false;
        }
        if (uv.getCurveRange() < 1) {
            IJ.error("Curvature Window must be at least 1.");
            return false;
        }
        if (uv.getSigRecoveryThresh() < 0 || uv.getSigRecoveryThresh() > 1) {
            IJ.error("Signal Map Threshold must be between 0 and 1.");
            return false;
        }
        if (uv.getFiloSizeMax() <= 0) {
            IJ.error("Max Filopodia Size must be greater than 0.");
            return false;
        }
        if (uv.getFiloSizeMin() <= 0) {
            IJ.error("Min Filopodia Size must be greater than 0.");
            return false;
        }
        if (uv.getFiloSizeMin() > uv.getFiloSizeMax()) {
            IJ.error("Min Filopodia Size must not exceed Max Filopodia Size.");
            return false;
        }
        return true;
    }
}
