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
package net.calm.adapt.Adapt;

import ij.IJ;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

import java.text.DecimalFormat;

/**
 * Container class for variable labels
 */
public class StaticVariables {

//    public static final String TEXT_SENS = "Texture Sensitivity",
//     GRAD_SENS  = "Gradient Threshold",
    /**
     * GUI label for Spatial Resolution
     */
    public static final String SPAT_RES = "Spatial Resolution (" + IJ.micronSymbol + "m/pixel)";
    /**
     * GUI label for Time Resolution
     */
    public static final String TIME_RES = "Frames per Minute";
    /**
     * GUI label for Cyto Channel
     */
    public static final String CYTO = "Cyto";
    /**
     * GUI label for Sig Channel
     */
    public static final String SIG = "Sig";
    /**
     * GUI label for Grey Level Threshold
     */
    public static final String GREY_SENS = "Grey Level Threshold";
    /**
     * GUI label for Segmentation Step Back
     */
    public static final String EROSION = "Erosion Iterations";
    /**
     * GUI label for Minimum Curvature Window
     */
    public static final String MIN_CURVE_RANGE = "Curvature Window";
    /**
     * GUI label for Minimum Curvature Threshold
     */
    public static final String MIN_CURVE_THRESH = "Min Curvature Threshold";
    /**
     * GUI label for Maximum Curvature Threshold
     */
    public static final String MAX_CURVE_THRESH = "Max Curvature Threshold";
    /**
     * GUI label for Signal Threshold Factor
     */
    public static final String SIG_THRESH_FACT = "Signal Threshold Factor";
    /**
     * GUI label for Use Signal Threshold
     */
    public static final String USE_SIG_THRESH = "Use Signal Threshold";
    /**
     * GUI label for Cut-off time
     */
    public static final String CUT_OFF = "Cut-Off Time (s)";
    /**
     * GUI label for Generate Visualisations
     */
    public static final String GEN_VIS = "Generate Visualisations";
    /**
     * GUI label for Generate Morphology Data
     */
    public static final String GET_MORPH = "Generate Morphology Data";
    /**
     * GUI label for Analyse Individual Protrusions
     */
    public static final String ANA_PROT = "Analyse Individual Protrusions";
    /**
     * GUI label for Temporal Filter Radius
     */
    public static final String TEMP_FILT_RAD = "Temporal Filter Radius (s)";
    /**
     * GUI label for Spatial Filter Radius
     */
    public static final String SPAT_FILT_RAD = "Spatial Filter Radius (" + IJ.micronSymbol + "m)";
    /**
     * GUI label for Gaussian Filter Radius
     */
    public static final String GAUSS_RAD = "Smoothing Filter Radius";
    /**
     * GUI label for Signal map threshold
     */
    public static final String SIG_REC_THRESH = "Signal Map Threshold";
    /**
     * GUI label for Cortex Depth
     */
    public static final String CORTEX_DEPTH = "Cortex Depth (" + IJ.micronSymbol + "m)";
    /**
     * GUI label for Auto Threshold
     */
    public static final String AUTO_THRESH = "Auto Threshold";

    public static final String SIMP_SEG = "Simple Segmentation";

    public static final String ADV_SEG = "Advanced Segmentation";

    public static final String INIT_FILT_RAD = "Pre-Initilialisation Smoothing Radius";

    public static final String LAMBDA = "Texture Sensitivity";

    public static final String MIN_TRAJ_LENGTH = "Minimum Trajectory Length";

    public static final String THRESH_METHOD = "Thresholding Method";

    public static final String PROT_LEN_THRESH = "Minimum Protrusion Length (%)";

    public static final String PROT_DUR_THRESH = "Minimum Protrusion Duration (frames)";

    public static final String DETECT_FILO = "Detect Filopodia";

    public static final String DETECT_BLEB = "Detect Blebs";

    public static final String FILO_MAX_SIZE = "Max Filopodia Size (" + IJ.micronSymbol + "m^2)";

    public static final String FILO_MIN_SIZE = "Min Filopodia Size (" + IJ.micronSymbol + "m^2)";

    public static final String GEN_SIG_DIST = "Generate Signal Distribution";

    public static final String DISPLAY_PLOTS = "Display Plots";

    public static final String MIN_MORPH_AREA = "Minimum Object Size (" + IJ.micronSymbol + "m^2)";

    public static final String VIS_LINE_WIDTH = "Visualisation Line Thickness";

    public static final String TIME = "Time_(s)";

    public static final String ZEROED_TIME = "Zeroed_Time_(s)";

    public static final String VELOCITY = "V_(" + GenUtils.mu + "m/s)";

    public static final String TOTAL_SIGNAL = "Total_Signal_(AU)";

    public static final String LENGTH = "Length_(" + GenUtils.mu + "m)";

    public static final String MEAN_SIGNAL = "Mean_Signal";

    public static final String NORM_LENGTH = "Normalised_Length";

    public static final String DATA_STREAM_HEADINGS[] = {TIME, VELOCITY,
        TOTAL_SIGNAL, MEAN_SIGNAL, LENGTH, NORM_LENGTH};
    /**
     * Software Name
     */
    public static final String TITLE = "Adapt";
    /**
     * Formatting of integer results
     */
    public static DecimalFormat numFormat = new DecimalFormat("000"); // For formatting results
    /**
     * Formatting of floating-point results
     */
    protected static DecimalFormat floatFormat = new DecimalFormat("0.000"); // For formatting results
    
    public static final String MIN_VEL = "Minimum Display Velocity  (" + GenUtils.mu + "m/min)";
    
    public static final String MAX_VEL = "Maximum Display Velocity (" + GenUtils.mu + "m/min)";
    /**
     * Background values in generated mask images
     */
//    public static final int BACKGROUND = 0;
    /**
     * Foreground values in generated mask images
     */
//    public static final int FOREGROUND = 255;
    /**
     * Program version number
     */
//    public static final int VERSION = 1;

    public static final int FLUOR_MAP_HEIGHT = 512;
}
