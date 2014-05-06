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
package Adapt;

import UtilClasses.GenUtils;
import java.text.DecimalFormat;

/**
 * Container class for variable labels
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class StaticVariables {

//    public static final String TEXT_SENS = "Texture Sensitivity",
//     GRAD_SENS  = "Gradient Threshold",
    /**
     * GUI label for Spatial Resolution
     */
    public static final String SPAT_RES = "Spatial Resolution";
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
    public static final String EROSION = "Segmentation Step Back";
    /**
     * GUI label for Curvature Window
     */
    public static final String CURVE_RANGE = "Curvature Window";
    /**
     * GUI label for Curvature Threshold
     */
    public static final String CURVE_THRESH = "Curvature Threshold";
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
    public static final String CUT_OFF = "Cut-off time";
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
    public static final String TEMP_FILT_RAD = "Temporal Filter Radius";
    /**
     * GUI label for Spatial Filter Radius
     */
    public static final String SPAT_FILT_RAD = "Spatial Filter Radius";
    /**
     * GUI label for Gaussian Filter Radius
     */
    public static final String GAUSS_RAD = "Gaussian Filter Radius";
    /**
     * GUI label for Signal map threshold
     */
    public static final String SIG_REC_THRESH = "Signal map threshold";
    /**
     * GUI label for Cortex Depth
     */
    public static final String CORTEX_DEPTH = "Cortex Depth";
    /**
     * GUI label for Auto Threshold
     */
    public static final String AUTO_THRESH = "Auto Threshold";
    /**
     * GUI label for Auto Threshold
     */
    public static final String DATA_STREAM_HEADINGS = "Time_(s), Zeroed_Time_(s), "
            + "V_(" + GenUtils.mu + "m/s), Total_Signal_(AU), Length_(" + GenUtils.mu + "m), "
            + "Total_Signal/L0, Mean_Signal, Normalised_Length";
    /**
     * Software Name
     */
    public static final String TITLE = "Adapt";
    /**
     * Formatting of integer results
     */
    protected static DecimalFormat numFormat = new DecimalFormat("000"); // For formatting results
    /**
     * Formatting of floating-point results
     */
    protected static DecimalFormat floatFormat = new DecimalFormat("0.000"); // For formatting results
    /**
     * Background values in generated mask images
     */
    public static final int BACKGROUND = 0;
    /**
     * Foreground values in generated mask images
     */
    public static final int FOREGROUND = 255;
    /**
     * Program version number
     */
    public static final int VERSION = 1;
}
