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

import net.calm.iaclasslibrary.UserVariables.UserVariables;

import java.util.Properties;

/**
 * Headless, GUI-free mapping from an {@link UserVariables} instance to the
 * label-keyed {@link Properties} that {@code ParameterWriter.saveParameters}
 * serialises as {@code parameters.json}. Mirrors exactly what the interactive
 * GUI produces via {@code PropertyExtractor}, so a macro/headless run writes an
 * identical manifest.
 */
public class ParameterProperties {

    private ParameterProperties() {
    }

    /**
     * Builds the label-keyed {@code Properties} for the given parameters.
     *
     * @param uv the parameters to serialise
     * @return a {@code Properties} whose keys are the GUI labels (from
     * {@link StaticVariables}) and whose values are the same strings the text
     * fields and toggle buttons would report
     */
    public static Properties fromUserVariables(UserVariables uv) {
        Properties props = new Properties();
        // Text-field-backed parameters.
        props.setProperty(StaticVariables.GREY_SENS, String.valueOf(uv.getGreyThresh()));
        props.setProperty(StaticVariables.SPAT_RES, String.valueOf(uv.getSpatialRes()));
        props.setProperty(StaticVariables.TIME_RES, String.valueOf(uv.getTimeRes()));
        props.setProperty(StaticVariables.MIN_TRAJ_LENGTH, String.valueOf(uv.getMinLength()));
        props.setProperty(StaticVariables.MIN_MORPH_AREA, String.valueOf(uv.getMorphSizeMin()));
        props.setProperty(StaticVariables.EROSION, String.valueOf(uv.getErosion()));
        props.setProperty(StaticVariables.SPAT_FILT_RAD, String.valueOf(uv.getSpatFiltRad()));
        props.setProperty(StaticVariables.TEMP_FILT_RAD, String.valueOf(uv.getTempFiltRad()));
        props.setProperty(StaticVariables.GAUSS_RAD, String.valueOf(uv.getGaussRad()));
        props.setProperty(StaticVariables.CORTEX_DEPTH, String.valueOf(uv.getCortexDepth()));
        props.setProperty(StaticVariables.VIS_LINE_WIDTH, String.valueOf(uv.getVisLineWidth()));
        props.setProperty(StaticVariables.MIN_CURVE_RANGE, String.valueOf(uv.getCurveRange()));
        props.setProperty(StaticVariables.MIN_CURVE_THRESH, String.valueOf(uv.getMinCurveThresh()));
        props.setProperty(StaticVariables.CUT_OFF, String.valueOf(uv.getCutOffTime()));
        props.setProperty(StaticVariables.SIG_THRESH_FACT, String.valueOf(uv.getSigThreshFact()));
        props.setProperty(StaticVariables.SIG_REC_THRESH, String.valueOf(uv.getSigRecoveryThresh()));
        props.setProperty(StaticVariables.FILO_MAX_SIZE, String.valueOf(uv.getFiloSizeMax()));
        props.setProperty(StaticVariables.FILO_MIN_SIZE, String.valueOf(uv.getFiloSizeMin()));
        // Toggle-button-backed parameters.
        props.setProperty(StaticVariables.AUTO_THRESH, String.valueOf(uv.isAutoThreshold()));
        props.setProperty(StaticVariables.GEN_VIS, String.valueOf(uv.isGenVis()));
        props.setProperty(StaticVariables.GET_MORPH, String.valueOf(uv.isGetMorph()));
        props.setProperty(StaticVariables.GEN_SIG_DIST, String.valueOf(uv.isGetFluorDist()));
        props.setProperty(StaticVariables.ANA_PROT, String.valueOf(uv.isAnalyseProtrusions()));
        props.setProperty(StaticVariables.USE_SIG_THRESH, String.valueOf(uv.isUseSigThresh()));
        props.setProperty(StaticVariables.DETECT_BLEB, String.valueOf(uv.isBlebDetect()));
        props.setProperty(StaticVariables.DETECT_FILO, String.valueOf(!uv.isBlebDetect()));
        props.setProperty(StaticVariables.DISPLAY_PLOTS, String.valueOf(uv.isDisplayPlots()));
        return props;
    }
}
