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
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PointRoi;
import net.calm.adapt.ui.GUI;
import net.calm.iaclasslibrary.UserVariables.UserVariables;
import net.calm.iaclasslibrary.UtilClasses.Utilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 */
public class Analyse_Batch extends Analyse_Movie {

    private boolean showGUI = true, mono = false;
    private File c1Directory, c2Directory;
//

    public Analyse_Batch() {
        super();
    }

    public Analyse_Batch(boolean showGUI, boolean mono, File c1Directory, File c2Directory, UserVariables uv) {
        super();
        this.showGUI = showGUI;
        this.mono = mono;
        this.c1Directory = c1Directory;
        this.c2Directory = c2Directory;
        this.uv = uv;
    }

    public void run(String arg) {
//        MacroWriter.write();
//        Utilities.setLookAndFeel(GUI.class);
        String version = null;
        try {
            final Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
            version = properties.getProperty("version");
        } catch (IOException e) {
            IJ.log("Failed to read version from project.properties.");
            version = "unknown";
        }
        TITLE = TITLE + "_v" + version;
        File cytoImageFiles[] = null; // Obtain file list
        File sigImageFiles[] = null;
        batchMode = true;
//        if (arg == null) {
        try {
            if (c1Directory == null) {
                directory = Utilities.getFolder(directory, "Select directory for reference channel", true);
                if (directory == null) {
                    return;
                }
            } else {
                directory = c1Directory;
            }
            cytoImageFiles = directory.listFiles(); // Obtain file list
            if (c2Directory == null && !mono) {
                c2Directory = Utilities.getFolder(directory, "Select directory for second channel", false);
            }
            directory = new File(directory.getAbsolutePath() + delimiter + "..");
        } catch (Exception e) {
            IJ.log("Failed to locate image directories: " + e.getMessage());
            return;
        }
//        } else {
//            directory = new File(arg + delimiter + "cyto");
//            cytoImageFiles = directory.listFiles(); // Obtain file list
//            secondChannel = new File(arg + delimiter + "sig");
//            directory = new File(directory.getAbsolutePath() + delimiter + "..");
//        }
        int cytoSize = cytoImageFiles.length;
        int sigSize = 0;
        if (c2Directory != null) {
            sigImageFiles = c2Directory.listFiles();
            sigSize = sigImageFiles.length;
        }
        Arrays.sort(cytoImageFiles);
        if (sigImageFiles != null) {
            Arrays.sort(sigImageFiles);
        }
        for (int f = 0; f < cytoSize; f++) {
            ImagePlus cytoImp = new ImagePlus(cytoImageFiles[f].getAbsolutePath());
            ImageStack cytoStack = cytoImp.getImageStack();
            roi = (PointRoi) cytoImp.getRoi();
            if (cytoStack != null && cytoStack.getSize() > 0) {
                try {
                    ImageStack sigStack;
                    if (cytoSize == sigSize) {
                        ImagePlus sigImp = new ImagePlus(sigImageFiles[f].getAbsolutePath());
                        sigStack = sigImp.getImageStack();
                    } else {
                        sigStack = null;
                    }
                    stacks[0] = cytoStack;
                    stacks[1] = sigStack;
                    if (showGUI) {
                        showGUI = false;
                        GUI gui = new GUI(null, true, TITLE, stacks, roi);
                        gui.setVisible(true);
                        if (!gui.isWasOKed()) {
                            return;
                        }
                        uv = gui.getUv();
                    }
                    analyse(cytoImageFiles[f].getName());
                } catch (Exception e) {
                    IJ.log("Failed to analyse " + cytoImageFiles[f].getName() + ": " + e.getMessage());
                }
            }
        }
        IJ.showStatus(TITLE + " done.");
    }

    private static final int PARAMS_SCHEMA_VERSION = 1;

    public static void readParams(UserVariables uv, File input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            applyParams(uv, mapper.readTree(input));
        } catch (Exception e) {
            IJ.log("Error reading parameter file: " + e.getMessage());
        }
    }

    static void applyParams(UserVariables uv, JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Parameter file must be a JSON object.");
        }
        int version = root.path("version").asInt(-1);
        if (version != PARAMS_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported parameter schema version " + version
                    + " (expected " + PARAMS_SCHEMA_VERSION
                    + "). Old positional-CSV parameter files are no longer supported.");
        }
        uv.setAutoThreshold(reqBool(root, "autoThreshold"));
        uv.setThreshMethod(reqText(root, "threshMethod"));
        uv.setGreyThresh(reqDouble(root, "greyThresh"));
        uv.setSpatialRes(reqDouble(root, "spatialRes"));
        uv.setTimeRes(reqDouble(root, "timeRes"));
        uv.setErosion(reqInt(root, "erosion"));
        uv.setSpatFiltRad(reqDouble(root, "spatFiltRad"));
        uv.setTempFiltRad(reqDouble(root, "tempFiltRad"));
        uv.setGaussRad(reqDouble(root, "gaussRad"));
        uv.setGenVis(reqBool(root, "genVis"));
        uv.setGetMorph(reqBool(root, "getMorph"));
        uv.setAnalyseProtrusions(reqBool(root, "analyseProtrusions"));
        uv.setBlebDetect(reqBool(root, "blebDetect"));
        uv.setCurveRange(reqInt(root, "curveRange"));
        uv.setMinCurveThresh(reqDouble(root, "minCurveThresh"));
        uv.setBlebLenThresh(reqDouble(root, "blebLenThresh"));
        uv.setBlebDurThresh(reqDouble(root, "blebDurThresh"));
        uv.setCutOffTime(reqDouble(root, "cutOffTime"));
        uv.setCortexDepth(reqDouble(root, "cortexDepth"));
        uv.setUseSigThresh(reqBool(root, "useSigThresh"));
        uv.setSigThreshFact(reqDouble(root, "sigThreshFact"));
        uv.setSigRecoveryThresh(reqDouble(root, "sigRecoveryThresh"));
        uv.setMinLength(reqInt(root, "minLength"));
        uv.setFiloSizeMax(reqDouble(root, "filoSizeMax"));
        uv.setGetFluorDist(reqBool(root, "getFluorDist"));
        uv.setMorphSizeMin(reqDouble(root, "morphSizeMin"));
    }

    private static boolean reqBool(JsonNode root, String name) {
        JsonNode n = root.get(name);
        if (n == null || !n.isBoolean()) {
            throw new IllegalArgumentException("Missing or invalid boolean field '" + name + "'.");
        }
        return n.booleanValue();
    }

    private static int reqInt(JsonNode root, String name) {
        JsonNode n = root.get(name);
        if (n == null || !n.isIntegralNumber()) {
            throw new IllegalArgumentException("Missing or invalid integer field '" + name + "'.");
        }
        return n.intValue();
    }

    private static double reqDouble(JsonNode root, String name) {
        JsonNode n = root.get(name);
        if (n == null || !n.isNumber()) {
            throw new IllegalArgumentException("Missing or invalid numeric field '" + name + "'.");
        }
        return n.doubleValue();
    }

    private static String reqText(JsonNode root, String name) {
        JsonNode n = root.get(name);
        if (n == null || !n.isTextual()) {
            throw new IllegalArgumentException("Missing or invalid string field '" + name + "'.");
        }
        return n.textValue();
    }

}
