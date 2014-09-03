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

import IAClasses.BoundaryPixel;
import IAClasses.DSPProcessor;
import IAClasses.DataStatistics;
import IAClasses.Pixel;
import IAClasses.ProgressDialog;
import IAClasses.Region;
import IAClasses.Utils;
import UtilClasses.Utilities;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.EDM;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ColorBlitter;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;
import ij.process.TypeConverter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import ui.GUI;

/**
 * Analyse_Movie is designed to quantify cell membrane dynamics and correlate
 * membrane velocity with signal dynamics. It takes as input two movies. One
 * represents the cell cytosol, which should depict a uniform cell against a
 * relatively uniform background. The second movie contains a signal of interest
 * that the user wishes to correlate with membrane dynamics.
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class Analyse_Movie implements PlugIn {

    private final double scaleFactor = 60.0 / 216.0;
    private static File directory, // root directory
            childDir, // root output directory
            parDir, // output directory for each cell
            velDirName, curvDirName, trajDirName;
    private int intermediate, terminal;
    private String TITLE = StaticVariables.TITLE;
    private final String delimiter = GenUtils.getDelimiter(); // delimiter in directory strings
    private String channelLabels[] = {"Cytoplasmic channel", "Signal to be correlated"};
    /**
     * Determines the format of printed results
     */
    protected DecimalFormat numFormat = StaticVariables.numFormat; // For formatting results
    private PointRoi roi = null; // Points used as seeds for cell detection
    private CellData cellData[];
    private final ImageStack stacks[] = new ImageStack[2];
    private double morphSizeMin = 5.0;

//    public static void main(String args[]) {
//        Analyse_Movie am = new Analyse_Movie();
//        am.initialise();
//        am.run(null);
//        System.exit(0);
//    }

    /**
     * Default constructor
     */
    public Analyse_Movie() {
    }

    /*
     * For debugging - images loaded from file
     */
    void initialise() {
        directory = Utilities.getFolder(directory, null);
        if (directory == null) {
            return;
        }
        File cytoImageFiles[] = (new File(directory.getPath() + delimiter + StaticVariables.CYTO)).listFiles(); // Obtain file list
        int cytoSize = cytoImageFiles.length;
        File sigImageFiles[] = (new File(directory.getPath() + delimiter + StaticVariables.SIG)).listFiles(); // Obtain file list
        int sigSize = sigImageFiles.length;
        Arrays.sort(cytoImageFiles);
        Arrays.sort(sigImageFiles);

        //Load first image to get dimensions
        ImageProcessor cip = new ImagePlus(directory + delimiter + StaticVariables.CYTO + delimiter
                + cytoImageFiles[0].getName()).getProcessor();
        ImageStack cytoStack = new ImageStack(cip.getWidth(), cip.getHeight());
        ImageStack sigStack;
        if (cytoSize == sigSize) {
            ImageProcessor sip = new ImagePlus(directory + delimiter + StaticVariables.SIG + delimiter
                    + sigImageFiles[0].getName()).getProcessor();
            sigStack = new ImageStack(sip.getWidth(), sip.getHeight());
        } else {
            sigStack = null;
        }
        for (int i = 0; i < cytoSize; i++) {
            cytoStack.addSlice("", new ImagePlus(cytoImageFiles[i].getAbsolutePath()).getProcessor());
            if (sigStack != null) {
                sigStack.addSlice("", new ImagePlus(sigImageFiles[i].getAbsolutePath()).getProcessor());
            }
        }
        stacks[0] = cytoStack;
        stacks[1] = sigStack;
    }

    /**
     * Opens GUIs for user to specify directory for output then runs analysis
     *
     * @param arg redundant
     */
    @Override
    public void run(String arg) {
        Utilities.setLookAndFeel(GUI.class);
        TITLE = TITLE + "_v" + StaticVariables.VERSION + "." + numFormat.format(Revision.Revision.revisionNumber);
        if (IJ.getInstance() != null && WindowManager.getIDList() == null) {
            IJ.error("No Images Open.");
            return;
        }
        directory = Utilities.getFolder(directory, "Specify directory for output files..."); // Specify directory for output
        if (directory == null) {
            return;
        }
        analyse();
        IJ.showStatus(TITLE + " done.");
    }

    private void analyse() {
        int cytoSize, sigSize;
        ImageStack cytoStack;
        if (IJ.getInstance() == null) {
            cytoStack = stacks[0];
            cytoSize = cytoStack.getSize();
        } else {
            ImagePlus images[] = GenUtils.specifyInputs(channelLabels);
            if (images == null) {
                return;
            }
            ImagePlus cytoImp = images[0];
            ImagePlus sigImp;
            if (images[1] != null) {
                sigImp = images[1];
            } else {
                sigImp = null;
            }
            roi = (PointRoi) cytoImp.getRoi(); // Points specified by the user indicate cells of interest
            cytoStack = cytoImp.getImageStack();
            cytoSize = cytoImp.getImageStackSize();
            if (sigImp != null) {
                sigSize = sigImp.getStackSize();
                if (cytoSize != sigSize) {
                    Toolkit.getDefaultToolkit().beep();
                    IJ.error("File number mismatch!");
                    return;
                }
            }
            stacks[0] = cytoStack;
            if (sigImp != null) {
                stacks[1] = sigImp.getImageStack();
            } else {
                stacks[1] = null;
            }
        }
        /*
         * Create new parent output directory - make sure directory name is
         * unique so old results are not overwritten
         */
        String parDirName = GenUtils.openResultsDirectory(directory + delimiter + TITLE, delimiter);
        if (parDirName != null) {
            parDir = new File(parDirName);
        } else {
            return;
        }
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        /*
         Convert cyto channel to 8-bit for faster segmentation
         */
        ImagePlus tempCytoImp = new ImagePlus("", stacks[0]);
        StackConverter sc = new StackConverter(tempCytoImp);
        sc.convertToGray8();
        stacks[0] = tempCytoImp.getImageStack();
        cytoStack = stacks[0];

        GUI gui = new GUI(null, true, TITLE, stacks, this);
        gui.setVisible(true);
        if (!gui.isWasOKed()) {
            return;
        }

        ProgressDialog dialog = new ProgressDialog(null, "Segmenting...", false, true, TITLE);
        dialog.setVisible(true);

        int n = initialiseROIs(1);
        if (n < 1) {
            return;
        }
        /*
         * Cycle through all images in stack and detect cells in each. All
         * detected regions are stored (in order) in stackRegions.
         */
        int thresholds[] = new int[cytoSize];
        Region[][] allRegions = new Region[n][cytoSize];
        for (int i = 0; i < cytoSize; i++) {
            dialog.updateProgress(i, cytoSize);
            ImageProcessor cytoImage = cytoStack.getProcessor(i + 1).duplicate();
            thresholds[i] = getThreshold(cytoImage, UserVariables.isAutoThreshold());
            if (cytoImage != null) {
                ArrayList<Region> theseRegions = findCellRegions(cytoImage, thresholds[i], cellData);
                for (int k = 0; k < n; k++) {
                    allRegions[k][i] = theseRegions.get(k);
                }
            }
            for (int j = 0; j < n; j++) {
                Region current = allRegions[j][i];
                if (current != null) {
//                current.calcCentroid();
                /*
                     * Mask from last segmentation used to initialise next
                     * segmentation
                     */
//                    int sx = current.getMaskSeed().getX();
//                    int sy = current.getMaskSeed().getY();
                    current.calcCentre(current.getBorderPix());
                    ImageProcessor mask = current.getMask(width, height);
//                    IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/mask_" + i + "_" + j + ".png");
//                    IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/adapt_test_data/masks/maskb_" + j + "_" + i + ".png");
                    for (int k = 0; k < UserVariables.getErosion(); k++) {
                        mask.erode();
                    }
//                    IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/mask_" + i + "_" + j + "_eroded.png");
//                    int cPoints[][] = null;
//                    int radius = UserVariables.getErosion();
//                    ImageProcessor tempMask = mask.duplicate();
//                    tempMask.erode();
//                    tempMask.invert();
//                    while (cPoints == null && radius < 8.0 * UserVariables.getErosion()) {
//                        cPoints = Utils.searchNeighbourhood(sx, sy, radius,
//                                StaticVariables.FOREGROUND, tempMask);
//                        radius++;
//                    }
//                    IJ.saveAs((new ImagePlus("", mask)), "PNG", "C:/users/barry05/desktop/adapt_test_data/masks/maska_" + j + "_" + i + ".png");
                    if (current.calcCentre(mask)) {
                        ArrayList<Pixel> centres = current.getCentres();
                        Pixel centre = centres.get(centres.size() - 1);
                        Region temp = new Region(width, height);
                        temp.addBorderPoint(centre);
                        cellData[j].setInitialRegion(temp);
//                        System.out.println("t: " + i + " cell: " + j + " x: " + centre.getX() + " y: " + centre.getY());
//                        cellData[j].setMaskSeed(new Pixel(cPoints[0][0], cPoints[0][1], j));
                    } else {
//                        ArrayList<Pixel> centres = current.getCentres();
//                        Pixel centre = centres.get(centres.size() - 1);
                        cellData[j].setInitialRegion(null);
                        cellData[j].setLength(i + 1);
//                        System.out.println("t: " + i + " cell: " + j + " x: " + centre.getX() + " y: " + centre.getY());
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            cellData[i].setCellRegions(allRegions[i]);
            cellData[i].setGreyThresholds(thresholds);
        }
        dialog.dispose();
        /*
         * Analyse the dynamics of each cell, represented by a series of
         * detected regions.
         */
        for (int index = 0; index < cellData.length; index++) {
            /*
             * Create child directory for current cell
             */
            String childDirName = GenUtils.openResultsDirectory(parDir + delimiter + index, delimiter);
            int length = cellData[index].getLength();
            if (length > UserVariables.getMinLength()) {
                childDir = new File(childDirName);
                buildOutput(index, length, false);
                if (UserVariables.isAnalyseProtrusions()) {
                    findProtrusions(cellData[index]);
                    correlativePlot(cellData[index]);
                }
            }
        }
        if (UserVariables.isGenVis()) {
            velDirName = GenUtils.createDirectory(parDir + delimiter + "Velocity_Visualisation");
            curvDirName = GenUtils.createDirectory(parDir + delimiter + "Curvature_Visualisation");
            trajDirName = GenUtils.createDirectory(parDir + delimiter + "Trajectories_Visualisation");
            generateVisualisations(cellData);
            generateCellTrajectories(cellData);
        }
        File paramFile;
        PrintWriter paramStream;
        try {
            paramFile = new File(parDir + delimiter + "params.csv");
            paramStream = new PrintWriter(new FileOutputStream(paramFile));
        } catch (FileNotFoundException e) {
            System.out.println("Error: Failed to create parameter file.\n");
            System.out.println(e.toString());
            return;
        }
        if (!printParamFile(paramStream)) {
            return;
        }
        paramStream.close();
    }

    int initialiseROIs(int slice) {
        ArrayList<Pixel> initP = new ArrayList<Pixel>();
//        initP.add(new Pixel(9, 40));
//        initP.add(new Pixel(40, 40));
        int n;
//        int threshold = getThreshold(stacks[0].getProcessor(slice), UserVariables.isAutoThreshold());
        if (roi != null) {
            if (roi.getType() == Roi.POINT) {
                n = roi.getNCoordinates();
            } else {
                IJ.error("Point selection required.");
                return -1;
            }
        } else {
            getInitialSeedPoints((ByteProcessor) stacks[0].getProcessor(slice), initP);
            n = initP.size();
//            n = 2;
        }
        cellData = new CellData[n];
        for (int i = 0; i < n; i++) {
            cellData[i] = new CellData();
//            ArrayList<Pixel> initialPix = new ArrayList();
            Pixel init;
            if (roi != null) {
                init = new Pixel(roi.getXCoordinates()[i] + roi.getBounds().x,
                        roi.getYCoordinates()[i] + roi.getBounds().y);
            } else {
                init = initP.get(i);
            }
//            initialPix.add(init);
            if (!Utils.isEdgePixel(init.getX(), init.getY(), stacks[0].getWidth(), stacks[0].getHeight(), 1)) {
                ByteProcessor mask = new ByteProcessor(stacks[0].getWidth(), stacks[0].getHeight());
                mask.setColor(Region.BACKGROUND);
                mask.fill();
                mask.setColor(Region.FOREGROUND);
                mask.drawPixel(init.getX(), init.getY());
//            cellData[i].setInitialPix(initialPix);
                cellData[i].setInitialRegion(new Region(mask, init));
                cellData[i].setLength(stacks[0].getSize());
            } else {
                cellData[i].setInitialRegion(null);
                cellData[i].setLength(0);
            }
//            cellData[i].setMaskSeed(init);
        }
        return n;
    }

    /*
     * Build velocity and signal maps and generate output visualisations for
     * cell corresponding to index
     */
    void buildOutput(int index, int frames, boolean preview) {
        Region[] allRegions = cellData[index].getCellRegions();
//        ImageStack cytoStack = stacks[0];
        ImageStack sigStack = stacks[1];
        int size = frames;
        File trajFile, segPointsFile;
        PrintWriter trajStream, segStream;
        double scaleFactors[] = new double[size];

        /*
         * Analyse morphology of current cell in all frames and save results in
         * morphology.csv
         */
        int upLength;
        if (UserVariables.isGetMorph() || UserVariables.isAnalyseProtrusions()) {
            upLength = getMorphologyData(cellData[index], allRegions, index, frames, preview);
        } else {
            upLength = getMaxBoundaryLength(cellData[index], allRegions, index);
        }
        MorphMap curveMap = new MorphMap(size, upLength);
        cellData[index].setCurveMap(curveMap);
        cellData[index].setScaleFactors(scaleFactors);
        buildCurveMap(allRegions, cellData[index], index, cellData.length);

        if (!preview) {
            /*
             * To obain a uniform map, all boundary lengths (from each frame) are
             * scaled up to the same length. For signal processing convenience, this
             * upscaled length will always be a power of 2.
             */
//        int power = (int) Math.ceil(Math.log(maxBoundary) / Math.log(2.0));
//        int upLength = (int) Math.pow(2.0, power);
            MorphMap velMap = new MorphMap(size, upLength);
            MorphMap sigMap = null;
            if (sigStack != null) {
                sigMap = new MorphMap(size, upLength);
            }
            /*
             * Create file to store cell trajectory, which consists of the list of
             * cell centroids.
             */
            try {
                trajFile = new File(childDir + delimiter + "trajectory.csv");
                segPointsFile = new File(childDir + delimiter + "cell_boundary_points.csv");
                trajStream = new PrintWriter(new FileOutputStream(trajFile));
                segStream = new PrintWriter(new FileOutputStream(segPointsFile));
            } catch (FileNotFoundException e) {
                System.out.println("Error: Failed to create parameter files.\n");
                System.out.println(e.toString());
                return;
            }
            if (!prepareOutputFiles(trajStream, segStream, size, 3)) {
                return;
            }
            cellData[index].setVelMap(velMap);
            cellData[index].setSigMap(sigMap);
            cellData[index].setScaleFactors(scaleFactors);
            buildVelSigMaps(index, allRegions, trajStream, segStream, cellData[index], cellData.length);
            trajStream.close();
            segStream.close();
            double smoothVelocities[][] = velMap.smoothMap(UserVariables.getTempFiltRad() * UserVariables.getTimeRes() / 60.0, UserVariables.getSpatFiltRad() / UserVariables.getSpatialRes()); // Gaussian smoothing in time and space
            double curvatures[][] = curveMap.smoothMap(0.0, 0.0);
            double sigchanges[][];
            if (sigMap != null) {
//            sigchanges = sigMap.smoothMap(timeRes, spatialRes);
                sigchanges = sigMap.getzVals();
            } else {
                sigchanges = new double[velMap.getWidth()][velMap.getHeight()];
                for (int i = 0; i < smoothVelocities.length; i++) {
                    Arrays.fill(sigchanges[i], 0.0);
                }
            }
            FloatProcessor greyVelMap = new FloatProcessor(smoothVelocities.length, upLength);
            FloatProcessor greyCurvMap = new FloatProcessor(curvatures.length, upLength);
            FloatProcessor greySigMap = new FloatProcessor(sigchanges.length, upLength);
            ColorProcessor colorVelMap = new ColorProcessor(smoothVelocities.length, upLength);
            DataStatistics velstats = new DataStatistics(0.05, smoothVelocities, smoothVelocities.length * smoothVelocities[0].length);
            double maxvel = velstats.getUpper99(); // Max and min velocity values (for colourmap) based on upper.lower 99th percentile boundaries
            double minvel = velstats.getLower99();
            generateScaleBar(maxvel, minvel);
            cellData[index].setGreyVelMap(greyVelMap);
            cellData[index].setGreyCurveMap(greyCurvMap);
            cellData[index].setMaxVel(maxvel);
            cellData[index].setMinVel(minvel);
            cellData[index].setGreySigMap(greySigMap);
            cellData[index].setColorVelMap(colorVelMap);
            cellData[index].setSmoothVelocities(smoothVelocities);
            generateMaps(smoothVelocities, cellData[index], index, cellData.length);
//            File parentDirectory = new File(directory.getParent());
            IJ.saveAs(new ImagePlus("", greyVelMap), "TIF", childDir + delimiter + "VelocityMap.tif");
            IJ.saveAs(new ImagePlus("", greyCurvMap), "TIF", childDir + delimiter + "CurvatureMap.tif");
            IJ.saveAs(new ImagePlus("", colorVelMap), "PNG", childDir + delimiter + "ColorVelocityMap.png");
//            IJ.saveAs(new ImagePlus("", greyVelMap), "TIF", parentDirectory.getParent() + delimiter
//                    + "VelocityMap_" + parentDirectory.getName() + "_" + directory.getName()
//                    + "_" + numFormat.format(index) + ".tif");
            IJ.saveAs(velMap.periodicity2D(greyVelMap, greyVelMap, 100), "TIF",
                    childDir + delimiter + "VelMap_AutoCorrelation.tif");
            if (sigStack != null) {
                IJ.saveAs(new ImagePlus("", greySigMap), "TIF", childDir + delimiter
                        + "SignalMap.tif");
                IJ.saveAs(velMap.periodicity2D(greySigMap, greyVelMap, 100), "TIF",
                        childDir + delimiter + "VelMap_SigMap_CrossCorrelation.tif");
                ImageProcessor rateOfSigChange = sigMap.calcRateOfChange(greySigMap);
                IJ.saveAs(new ImagePlus("", rateOfSigChange), "TIF", childDir + delimiter
                        + "ChangeInSignalMap.tif");
                IJ.saveAs(velMap.periodicity2D(rateOfSigChange, greyVelMap, 100), "TIF",
                        childDir + delimiter + "VelMap_ChangeInSigMap_CrossCorrelation.tif");
            }
        }
    }

    int getMorphologyData(CellData cellData, Region[] allRegions, int index, int size, boolean preview) {
        int maxBoundary = 0;
        int measures = Integer.MAX_VALUE;
        ResultsTable rt = Analyzer.getResultsTable();
        rt.reset();
        int lengths[] = new int[size];
        Prefs.blackBackground = false;
        for (int h = 0; h < size; h++) {
            Region current = allRegions[h];
            lengths[h] = (current.getBorderPix()).size();
            if (lengths[h] > maxBoundary) {
                maxBoundary = lengths[h];
            }
            if (!preview) {
                double minArea = morphSizeMin / (Math.pow(UserVariables.getSpatialRes(), 2.0));
                ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS,
                        measures, rt, minArea, Double.POSITIVE_INFINITY);
//                ArrayList<Pixel> centroids = current.getCentroids();
//                Pixel centre = centroids.get(centroids.size() - 1);
                ImagePlus maskImp = new ImagePlus(StaticVariables.floatFormat.format(h / (UserVariables.getTimeRes() / 60.0)),
                        current.getMask(stacks[0].getWidth(), stacks[0].getHeight()));
                analyzer.analyze(maskImp);
            }
        }
        if (childDir != null && !preview) {
            try {
                Analyzer.getResultsTable().saveAs(childDir.getAbsolutePath() + delimiter + "morphology.csv");
            } catch (IOException e) {
                IJ.error("Could not save morphological data file.");
            }
        }
        return maxBoundary;
    }

    int getMaxBoundaryLength(CellData cellData, Region[] allRegions, int index) {
        int size = cellData.getLength();
        int maxBoundary = 0;
        for (int h = 0; h < size; h++) {
            Region current = allRegions[h];
            int length = (current.getBorderPix()).size();
            if (length > maxBoundary) {
                maxBoundary = length;
            }
        }
        return maxBoundary;
    }

    boolean prepareOutputFiles(PrintWriter trajStream, PrintWriter segStream, int size, int dim) {
        segStream.println("FRAMES " + String.valueOf(size));
        segStream.println("DIM " + String.valueOf(dim));
        trajStream.println("Time(s), X (" + String.valueOf(GenUtils.mu) + "m), Y (" + String.valueOf(GenUtils.mu) + "m)");
        return true;
    }

    boolean printParamFile(PrintWriter paramStream) {
        paramStream.println(TITLE);
        paramStream.println(Utilities.getDate("dd/MM/yyyy HH:mm:ss"));
        paramStream.println();
        paramStream.println(StaticVariables.AUTO_THRESH + ", " + String.valueOf(UserVariables.isAutoThreshold()));
        paramStream.println(StaticVariables.GREY_SENS + ", " + String.valueOf(UserVariables.getGreyThresh()));
        paramStream.println(StaticVariables.SPAT_RES + ", " + String.valueOf(UserVariables.getSpatialRes()));
        paramStream.println(StaticVariables.TIME_RES + ", " + String.valueOf(UserVariables.getTimeRes()));
        paramStream.println(StaticVariables.EROSION + ", " + String.valueOf(UserVariables.getErosion()));
        paramStream.println(StaticVariables.SPAT_FILT_RAD + ", " + String.valueOf(UserVariables.getSpatFiltRad()));
        paramStream.println(StaticVariables.TEMP_FILT_RAD + ", " + String.valueOf(UserVariables.getTempFiltRad()));
        paramStream.println(StaticVariables.GAUSS_RAD + ", " + String.valueOf(UserVariables.getGaussRad()));
        paramStream.println(StaticVariables.GEN_VIS + ", " + String.valueOf(UserVariables.isGenVis()));
        paramStream.println(StaticVariables.GET_MORPH + ", " + String.valueOf(UserVariables.isGetMorph()));
        paramStream.println(StaticVariables.ANA_PROT + ", " + String.valueOf(UserVariables.isAnalyseProtrusions()));
        paramStream.println(StaticVariables.CURVE_RANGE + ", " + String.valueOf(UserVariables.getCurveRange()));
        paramStream.println(StaticVariables.CURVE_THRESH + ", " + String.valueOf(UserVariables.getCurveThresh()));
        paramStream.println(StaticVariables.CUT_OFF + ", " + String.valueOf(UserVariables.getCutOffTime()));
        paramStream.println(StaticVariables.CORTEX_DEPTH + ", " + String.valueOf(UserVariables.getCortexDepth()));
        paramStream.println(StaticVariables.USE_SIG_THRESH + ", " + String.valueOf(UserVariables.isUseSigThresh()));
        paramStream.println(StaticVariables.SIG_THRESH_FACT + ", " + String.valueOf(UserVariables.getSigThreshFact()));
        paramStream.println(StaticVariables.SIG_REC_THRESH + ", " + String.valueOf(UserVariables.getSigRecoveryThresh()));
        return true;
    }

    void buildVelSigMaps(int index, Region[] allRegions, PrintWriter trajStream, PrintWriter segStream, CellData cellData, int total) {
        ImageStack cytoStack = stacks[0];
        ImageStack sigStack = stacks[1];
        ProgressDialog dialog = new ProgressDialog(null,
                "Initialising velocity map for cell " + String.valueOf(index + 1) + " of " + String.valueOf(total) + "...",
                false, true, TITLE);
        dialog.setVisible(true);
        MorphMap velMap = cellData.getVelMap();
        MorphMap sigMap = cellData.getSigMap();
        int width = velMap.getWidth();
        int height = velMap.getHeight();
        for (int i = 0; i < width; i++) {
            dialog.updateProgress(i, width);
            Region current = allRegions[i];
            ArrayList<Pixel> centres = current.getCentres();
            int cl = centres.size();
            double xc = centres.get(cl - 1).getPrecX();
            double yc = centres.get(cl - 1).getPrecY();
            trajStream.println(String.valueOf(i * 60.0 / UserVariables.getTimeRes())
                    + ", " + String.valueOf(xc * UserVariables.getSpatialRes())
                    + ", " + String.valueOf(yc * UserVariables.getSpatialRes()));
            /*
             * Get points for one column (time-point) of map
             */
            Pixel vmPoints[] = current.buildVelMapCol(xc, yc, cytoStack, i + 1, UserVariables.getTimeRes(), UserVariables.getSpatialRes(), cellData.getGreyThresholds());
            Pixel smPoints[] = null;
            if (sigStack != null) {
                smPoints = current.buildStandMapCol(xc, yc, sigStack, i + 1, height, (int) Math.round(UserVariables.getCortexDepth() / UserVariables.getSpatialRes()));
            }
            double x[] = new double[vmPoints.length];
            double y[] = new double[vmPoints.length];
            double vmz[] = new double[vmPoints.length];
            double smz[] = new double[height];
            /*
             * Build arrays for (x,y) coordinates and velocity/signal values
             * from pixel data
             */
            for (int j = 0; j < vmPoints.length; j++) {
                x[j] = vmPoints[j].getX();
                y[j] = vmPoints[j].getY();
                vmz[j] = vmPoints[j].getZ();
                segStream.println(String.valueOf(x[j]) + ", " + String.valueOf(y[j]) + ", " + String.valueOf(i));
            }
            if (smPoints != null) {
                for (int j = 0; j < height; j++) {
                    smz[j] = smPoints[j].getZ();
                }
            }
            /*
             * Upscale all columns to maxBoundary length before adding to maps
             */
            double upX[] = DSPProcessor.upScale(x, height, false);
            double upY[] = DSPProcessor.upScale(y, height, false);
            velMap.addColumn(upX, upY, DSPProcessor.upScale(vmz, height, false), i);
            if (sigMap != null) {
                sigMap.addColumn(upX, upY, smz, i);
            }
        }
        dialog.dispose();
    }

    private void buildCurveMap(Region[] allRegions, CellData cellData, int index, int total) {
        ProgressDialog dialog = new ProgressDialog(null,
                "Building curve map for cell " + String.valueOf(index + 1) + " of " + String.valueOf(total) + "...",
                false, true, TITLE);
        dialog.setVisible(true);
        MorphMap curveMap = cellData.getCurveMap();
        int width = curveMap.getWidth();
        int height = curveMap.getHeight();
        for (int i = 0; i < width; i++) {
            dialog.updateProgress(i, width);
            Region current = allRegions[i];
            ArrayList<Pixel> centres = current.getCentres();
            int cl = centres.size();
            double xc = centres.get(cl - 1).getPrecX();
            double yc = centres.get(cl - 1).getPrecY();
            /*
             * Get points for one column (time-point) of map
             */
            Pixel vmPoints[] = current.buildVelMapCol(xc, yc, stacks[0], i + 1,
                    UserVariables.getTimeRes(), UserVariables.getSpatialRes(), cellData.getGreyThresholds());
            double x[] = new double[vmPoints.length];
            double y[] = new double[vmPoints.length];
            /*
             * Build arrays for (x,y) coordinates and velocity/signal values
             * from pixel data
             */
            for (int j = 0; j < vmPoints.length; j++) {
                x[j] = vmPoints[j].getX();
                y[j] = vmPoints[j].getY();
            }
            /*
             * Upscale all columns to maxBoundary length before adding to maps
             */
            double upX[] = DSPProcessor.upScale(x, height, false);
            double upY[] = DSPProcessor.upScale(y, height, false);
            curveMap.addColumn(upX, upY, DSPProcessor.upScale(Region.calcCurvature(vmPoints,
                    UserVariables.getCurveRange()), height, false), i);
            cellData.getScaleFactors()[i] = ((double) height) / vmPoints.length;
        }
        dialog.dispose();
    }

    void generateMaps(double[][] smoothVelocities, CellData cellData, int index, int total) {
        ProgressDialog dialog = new ProgressDialog(null,
                "Generating maps for cell " + String.valueOf(index + 1) + " of " + String.valueOf(total) + "...",
                false, true, TITLE);
        dialog.setVisible(true);
        boolean sigNull = (cellData.getSigMap() == null);
        int l = smoothVelocities.length;
        MorphMap curveMap = cellData.getCurveMap();
        int upLength = curveMap.getHeight();
        FloatProcessor greyVelMap = cellData.getGreyVelMap();
        FloatProcessor greyCurvMap = cellData.getGreyCurveMap();
        FloatProcessor greySigMap = null;
        ColorProcessor colorVelMap = cellData.getColorVelMap();
        double curvatures[][] = curveMap.getzVals();
        double sigchanges[][] = null;
        if (!sigNull) {
            sigchanges = cellData.getSigMap().smoothMap(UserVariables.getTempFiltRad() * UserVariables.getTimeRes() / 60.0, UserVariables.getSpatFiltRad() / UserVariables.getSpatialRes()); ;
            greySigMap = cellData.getGreySigMap();
        }
        for (int i = 0; i < l; i++) {
            dialog.updateProgress(i, l);
            for (int j = 0; j < upLength; j++) {
                greyVelMap.putPixelValue(i, j, smoothVelocities[i][j]);
                greyCurvMap.putPixelValue(i, j, curvatures[i][j]);
                colorVelMap.setColor(getColor(smoothVelocities[i][j], cellData.getMaxVel(), cellData.getMinVel()));
                colorVelMap.drawPixel(i, j);
                if (!sigNull && greySigMap != null) {
                    greySigMap.putPixelValue(i, j, sigchanges[i][j]);
                }
            }
        }
        dialog.dispose();
    }

    void generateVisualisations(CellData cellDatas[]) {
        int N = cellDatas.length;
        ImageStack cytoStack = stacks[0];
        ProgressDialog dialog = new ProgressDialog(null, "Building Visualisations...", false, true, TITLE);
        dialog.setVisible(true);
        /*
         * Generate various visualisations for output
         */
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        int stackSize = cytoStack.getSize();
        double mincurve = -50.0, maxcurve = 50.0;
        for (int t = 0; t < stackSize; t++) {
            dialog.updateProgress(t, stackSize);
            ColorProcessor velOutput = new ColorProcessor(width, height);
            velOutput.setColor(Color.black);
            velOutput.fill();
//            velOutput.setLineWidth(3);
            ColorProcessor curveOutput = new ColorProcessor(width, height);
            curveOutput.setColor(Color.black);
            curveOutput.fill();
//            curveOutput.setLineWidth(3);
            for (int n = 0; n < N; n++) {
                int length = cellData[n].getLength();
                if (length > t && length > UserVariables.getMinLength()) {
                    double[][] smoothVelocities = cellData[n].getSmoothVelocities();
                    Region[] allRegions = cellData[n].getCellRegions();
                    MorphMap curveMap = cellData[n].getCurveMap();
                    int upLength = curveMap.getHeight();
                    double maxvel = cellData[n].getMaxVel();
                    double minvel = cellData[n].getMinVel();
                    double xCoords[][] = curveMap.getxCoords();
                    double yCoords[][] = curveMap.getyCoords();
                    double curvatures[][] = curveMap.getzVals();
                    for (int j = 0; j < upLength; j++) {
                        int x = (int) Math.round(xCoords[t][j]);
                        int y = (int) Math.round(yCoords[t][j]);
                        velOutput.setColor(getColor(smoothVelocities[t][j], maxvel, minvel));
                        velOutput.drawDot(x, y);
                        curveOutput.setColor(getColor(curvatures[t][j], maxcurve, mincurve));
                        curveOutput.drawDot(x, y);
                    }
                    velOutput.setColor(Color.white);
                    Region current = allRegions[t];
                    ArrayList<Pixel> centres = current.getCentres();
                    int cl = centres.size();
                    int xc = (int) Math.round(centres.get(cl - 1).getX());
                    int yc = (int) Math.round(centres.get(cl - 1).getY());
                    velOutput.fillOval(xc - 1, yc - 1, 3, 3);
                    velOutput.drawString(String.valueOf(n + 1), xc + 2, yc + 2);
                }
            }
            IJ.saveAs((new ImagePlus("", velOutput)), "PNG", velDirName.getAbsolutePath() + delimiter + numFormat.format(t));
            IJ.saveAs((new ImagePlus("", curveOutput)), "PNG", curvDirName.getAbsolutePath() + delimiter + numFormat.format(t));
        }
        dialog.dispose();
    }

    void generateCellTrajectories(CellData cellDatas[]) {
        int N = cellDatas.length;
        ImageStack cytoStack = stacks[0];
        ProgressDialog dialog = new ProgressDialog(null, "Building Cell Trajectories...", false, true, TITLE);
        dialog.setVisible(true);
        /*
         * Generate various visualisations for output
         */
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        int stackSize = cytoStack.getSize();
        int origins[][] = new int[N][2];
        double distances[] = new double[N];
        Color colors[] = new Color[N];
        Random rand = new Random();
        Arrays.fill(distances, 0.0);
        int xc = width / 2;
        int yc = height / 2;
        File trajFile;
        PrintWriter trajStream;
        try {
            trajFile = new File(trajDirName + delimiter + "trajectory.csv");
            trajStream = new PrintWriter(new FileOutputStream(trajFile));
        } catch (FileNotFoundException e) {
            System.out.println("Error: Failed to create cell trajectories file.\n");
            System.out.println(e.toString());
            return;
        }
        trajStream.print("Frame,");
        for (int n = 0; n < N; n++) {
            colors[n] = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            if (cellData[n].getLength() > UserVariables.getMinLength()) {
                trajStream.print("Cell_" + String.valueOf(n + 1) + "_X,");
                trajStream.print("Cell_" + String.valueOf(n + 1) + "_Y,");
                Region[] allRegions = cellData[n].getCellRegions();
                Region current = allRegions[0];
                ArrayList<Pixel> centres = current.getCentres();
                int cl = centres.size();
                origins[n][0] = (int) Math.round(centres.get(cl - 1).getX());
                origins[n][1] = (int) Math.round(centres.get(cl - 1).getY());
            }
        }
        trajStream.println();
        for (int t = 0; t < stackSize; t++) {
            trajStream.print(String.valueOf(t) + ",");
            dialog.updateProgress(t, stackSize);
            ColorProcessor trajOutput = new ColorProcessor(width, height);
            trajOutput.setColor(StaticVariables.BACKGROUND);
            trajOutput.fill();
            for (int n = 0; n < N; n++) {
                trajOutput.setColor(colors[n]);
                int length = cellData[n].getLength();
                if (length > t && length > UserVariables.getMinLength()) {
                    Region[] allRegions = cellData[n].getCellRegions();
                    Region current = allRegions[t];
                    ArrayList<Pixel> centres = current.getCentres();
                    int c = centres.size();
                    double x = centres.get(c - 1).getX();
                    double y = centres.get(c - 1).getY();
                    trajOutput.fillOval((int) Math.round(x + xc - origins[n][0]) - 1,
                            (int) Math.round(y + yc - origins[n][1]) - 1, 3, 3);
                    trajStream.print(String.valueOf(x) + "," + String.valueOf(y) + ",");
                    if (t > 0) {
                        Region last = allRegions[t - 1];
                        ArrayList<Pixel> lastCentres = last.getCentres();
                        int lc = lastCentres.size();
                        double lx = lastCentres.get(lc - 1).getX();
                        double ly = lastCentres.get(lc - 1).getY();
                        distances[n] += Utils.calcDistance(x, y, lx, ly) * UserVariables.getSpatialRes();
                    }
                }
            }
            IJ.saveAs((new ImagePlus("", trajOutput)), "PNG", trajDirName.getAbsolutePath() + delimiter + numFormat.format(t));
            trajStream.println();
        }
        trajStream.print("\nMean Velocity (" + IJ.micronSymbol + "m/min):,");
        for (int n = 0; n < N; n++) {
            int l = cellData[n].getLength();
            if (l > UserVariables.getMinLength()) {
                trajStream.print(String.valueOf(distances[n] * UserVariables.getSpatialRes()
                        / (l * UserVariables.getTimeRes())) + ",,");
            }
        }
        trajStream.print("\nDirectionality:,");
        for (int n = 0; n < N; n++) {
            int l = cellData[n].getLength();
            if (l > UserVariables.getMinLength()) {
                Region current = cellData[n].getCellRegions()[l - 1];
                ArrayList<Pixel> centres = current.getCentres();
                Pixel centre = centres.get(centres.size() - 1);
                trajStream.print(String.valueOf(Utils.calcDistance(origins[n][0], origins[n][1], centre.getX(), centre.getY()) / distances[n]) + ",,");
            }
        }
        dialog.dispose();
        trajStream.close();
    }

    /*
     * Generate graphic scalebar and output to child directory
     */
    void generateScaleBar(double max, double min) {
        ColorProcessor scaleBar = new ColorProcessor(90, 480);
        scaleBar.setColor(Color.white);
        scaleBar.fill();
        double step = (max - min) / (scaleBar.getHeight() - 1);
        for (int j = 0; j < scaleBar.getHeight(); j++) {
            double val = max - j * step;
            Color thiscolor = getColor(val, max, min);
            scaleBar.setColor(thiscolor);
            scaleBar.drawLine(0, j, scaleBar.getWidth() / 2, j);
        }
        DecimalFormat decformat = new DecimalFormat("0.0");
        scaleBar.setFont(new Font("Times", Font.BOLD, 20));
        int x = scaleBar.getWidth() - scaleBar.getFontMetrics().charWidth('0') * 4;
        scaleBar.setColor(Color.black);
        scaleBar.drawString(decformat.format(max), x, scaleBar.getFontMetrics().getHeight());
        scaleBar.drawString(decformat.format(min), x, scaleBar.getHeight());
        IJ.saveAs(new ImagePlus("", scaleBar), "PNG", childDir + delimiter + "VelocityScaleBar.png");
    }

    /*
     * Essentially acts as a look-up table, calculated 'on the fly'. The output
     * will range somewhere between red for retmax, green for promax and yellow
     * if val=0.
     */
    Color getColor(double val, double promax, double retmax) {
        Color colour = Color.black;
        int r, g;
        if (val >= 0.0) {
            r = 255 - (int) Math.round(255 * val / promax);
            if (r < 0) {
                r = 0;
            } else if (r > 255) {
                r = 255;
            }
            colour = new Color(r, 255, 0);
        } else if (val < 0.0) {
            g = 255 - (int) Math.round(255 * val / retmax);
            if (g < 0) {
                g = 0;
            } else if (g > 255) {
                g = 255;
            }
            colour = new Color(255, g, 0);
        }
        return colour;
    }

    void findProtrusions(CellData cellData) {
        /*
         * Protrusion events are identified by thresholding velMapImage.
         */
        ByteProcessor binmap = (ByteProcessor) (new TypeConverter(cellData.getGreyVelMap(), true)).convertToByte();
        binmap.invert();
        binmap.threshold((int) Math.floor(-binmap.getStatistics().stdDev + binmap.getStatistics().mean));
        /*
         * Protrusions are detected using ParticleAnalyzer and added to an
         * instance of RoiManager.
         */
        binmap.invert(); // Analyzer assumes background is black
        /*
         Lines are drawn such that protrusions in contact with image edges (t=min, t=max)
         are not excluded from analysis.
         */
        binmap.setColor(0);
        binmap.drawLine(0, 0, 0, binmap.getHeight() - 1);
        binmap.drawLine(binmap.getWidth() - 1, 0, binmap.getWidth() - 1, binmap.getHeight() - 1);
        Prefs.blackBackground = true;
        RoiManager manager = new RoiManager(true);
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER
                + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES + ParticleAnalyzer.SHOW_MASKS,
                0, null, 0.0, Double.POSITIVE_INFINITY);
        analyzeDetections(manager, binmap, analyzer);
        ByteProcessor binmapnoedge = (ByteProcessor) analyzer.getOutputImage().getProcessor();
        ByteProcessor flippedBinMap = new ByteProcessor(binmap.getWidth(), binmap.getHeight());
        int offset = constructFlippedBinMap(binmap, binmapnoedge, flippedBinMap);
        RoiManager manager2 = new RoiManager(true);
        analyzeDetections(manager2, flippedBinMap, analyzer);
        copyRoisWithOffset(manager, manager2, offset);
        cellData.setVelRois(manager.getRoisAsArray());
        if (UserVariables.isUseSigThresh()) {
            ImageProcessor scaledSigMap = cellData.getGreySigMap().duplicate();
            scaledSigMap.multiply(UserVariables.getSpatialRes() / UserVariables.getCortexDepth());
            ImageStatistics sigStats = ImageStatistics.getStatistics(scaledSigMap,
                    Measurements.MEAN + Measurements.STD_DEV, null);
            cellData.setSigThresh(sigStats.mean + UserVariables.getSigThreshFact() * sigStats.stdDev);
        } else {
            cellData.setSigThresh(0.0);
        }
    }

    void analyzeDetections(RoiManager manager, ImageProcessor binmap, ParticleAnalyzer analyzer) {
        ParticleAnalyzer.setRoiManager(manager);
        if (!analyzer.analyze(new ImagePlus("", binmap))) {
            IJ.error("Protrusion analysis failed.");
            return;
        }
        WindowManager.getImage(WindowManager.getIDList()[WindowManager.getImageCount() - 1]).hide();
    }

    int constructFlippedBinMap(ByteProcessor input1, ByteProcessor input2, ByteProcessor output) {
        ByteBlitter blitter1 = new ByteBlitter(input1);
        blitter1.copyBits(input2, 0, 0, Blitter.SUBTRACT);
        ByteBlitter flipBlitter = new ByteBlitter(output);
        Rectangle topROI = new Rectangle(0, 0, input1.getWidth(), input1.getHeight() / 2);
        Rectangle bottomROI;
        if (input1.getHeight() % 2 == 0) {
            bottomROI = new Rectangle(0, input1.getHeight() / 2, input1.getWidth(), input1.getHeight() / 2);
        } else {
            bottomROI = new Rectangle(0, input1.getHeight() / 2, input1.getWidth(), input1.getHeight() / 2 + 1);
        }
        input1.setRoi(topROI);
        flipBlitter.copyBits(input1.crop(), 0, bottomROI.height, Blitter.COPY);
        input1.setRoi(bottomROI);
        flipBlitter.copyBits(input1.crop(), 0, 0, Blitter.COPY);
        return bottomROI.y;
    }

    void copyRoisWithOffset(RoiManager manager, RoiManager manager2, int offset) {
        Roi preAdjusted[] = manager2.getRoisAsArray();
        for (Roi r : preAdjusted) {
            Polygon poly = ((PolygonRoi) r).getPolygon();
            int n = poly.npoints;
            int xp[] = new int[n];
            int yp[] = new int[n];
            for (int i = 0; i < n; i++) {
                xp[i] = poly.xpoints[i];
                yp[i] = poly.ypoints[i] + offset;
            }
            manager.addRoi(new PolygonRoi(xp, yp, n, Roi.POLYGON));
        }
    }

    /*
     * Detects the cells in the specified image and, if showPreview is true,
     * returns an image illustrating the detected boundary.
     */
    private ArrayList<Region> findCellRegions(ImageProcessor inputProc, double threshold, CellData[] cellData) {
        int outVal = 1;
        ImageProcessor inputFloatProc = (new TypeConverter(inputProc, true)).convertToFloat(null);
        ImageProcessor inputDup = inputFloatProc.duplicate();
        int width = inputDup.getWidth();
        int height = inputDup.getHeight();
        int n = cellData.length;
        ArrayList<Region> singleImageRegions = new ArrayList<Region>();
        /*
         * Create image depicting regions to be "grown". Regions initialised
         * using centroids.
         */
        ByteProcessor indexedRegions = new ByteProcessor(width, height);
        indexedRegions.setValue(StaticVariables.BACKGROUND);
        indexedRegions.fill();
        indexedRegions.setColor(outVal);
        ByteBlitter bb = new ByteBlitter(indexedRegions);
        for (int i = 0; i < n; i++) {
//            ArrayList<Pixel> initialPix = cellData[i].getInitialPix();
            Region region = cellData[i].getInitialRegion();
            if (region != null) {
                if (region.getCentres().size() < 1) {
                    region.calcCentre(region.getBorderPix());
                }
//                region.setMaskSeed(cellData[i].getMaskSeed());
                ImageProcessor mask = region.getMask(width, height);
//            region.setSeedPix();
                mask.invert();
                mask.multiply((i + 1) / 255.0);
                bb.copyBits(mask, 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
            }
            singleImageRegions.add(region);
            outVal++;
        }
//        IJ.saveAs(new ImagePlus("", indexedRegions), "PNG", "C:/users/barry05/desktop/indexedRegions.png");
        intermediate = singleImageRegions.size() + 1;
        terminal = intermediate + 1;
        /*
         * Filter image to be used as basis for region growing.
         */
        sigmoidFilter(inputDup, threshold);
//        PSF psf = new PSF(spatialRes, gaussRad);
//        psf.calcPSF();
//        double gaussRad = psf.getSigEst();
        (new GaussianBlur()).blurGaussian(inputDup, UserVariables.getGaussRad(), UserVariables.getGaussRad(), 0.01);
        growRegions(indexedRegions, inputDup, singleImageRegions, UserVariables.isSimple(), threshold);
        return singleImageRegions;
    }

    /*
     * Conditional dilate the regions in regionImage based on the information in
     * inputImage.
     */
    private ByteProcessor growRegions(ByteProcessor regionImage, ImageProcessor inputImage, ArrayList<Region> singleImageRegions, boolean simple, double threshold) {
        int i, j;
        int width = regionImage.getWidth();
        int height = regionImage.getHeight();
        boolean totChange = true;
        boolean thisChange;
        /*
         * Image texture (and grey levels) used to control region growth.
         * Standard deviation of grey levels is used as a simple measure of
         * texture.
         */
        int cellNum = singleImageRegions.size();
        float distancemaps[][][] = null;
        if (!simple) {
            distancemaps = new float[cellNum][width][height];
        }
        ByteProcessor regionImages[] = new ByteProcessor[cellNum];
        if (!simple) {
            initDistanceMaps(inputImage, regionImage, singleImageRegions, distancemaps,
                    regionImages, width, 1.0, threshold);
        }
        /*
         * Reset regionImages
         */
        for (int n = 0; n < cellNum; n++) {
            regionImages[n] = (ByteProcessor) regionImage.duplicate();
        }
        /*
         * Grow regions according to texture, grey levels and distance maps
         */
//        ImageStack regionImageStack = new ImageStack(regionImage.getWidth(), regionImage.getHeight());
        while (totChange) {
            totChange = false;
            for (i = 0; i < cellNum; i++) {
//                ImageStack distancemapStack = new ImageStack(distancemaps[0].length, distancemaps[0][0].length);
//                for (int n = 0; n < distancemaps.length; n++) {
//                    FloatProcessor distanceMapImage = new FloatProcessor(distancemaps[i].length, distancemaps[i][0].length);
//                    for (int x = 0; x < distancemaps[i].length; x++) {
//                        for (int y = 0; y < distancemaps[i][x].length; y++) {
//                            distanceMapImage.putPixelValue(x, y, distancemaps[i][x][y]);
//                        }
//                    }
//                    distancemapStack.addSlice(distanceMapImage);
//                }
//                IJ.saveAs(new ImagePlus("", distanceMapImage), "TIF", "C:/users/barry05/desktop/distanceMapImage_" + i + ".tif");}
                ByteProcessor ref = (ByteProcessor) regionImages[i].duplicate();
                Region cell = singleImageRegions.get(i);
//                if (cell.isActive()) {
//                    cell.savePixels(width, height);
                if (cell != null) {
                    LinkedList<Pixel> borderPix = cell.getBorderPix();
                    int borderLength = borderPix.size();
                    thisChange = false;
                    for (j = 0; j < borderLength; j++) {
                        Pixel thispix = borderPix.get(j);
                        if (!simple) {
                            thisChange = dijkstraDilate(ref, cell, thispix,
                                    distancemaps, intermediate, i + 1) || thisChange;
                        } else {
                            thisChange = simpleDilate(regionImage,
                                    inputImage, cell, thispix, intermediate, threshold, i + 1)
                                    || thisChange;
                        }
                    }
                    cell.setActive(thisChange);
                    totChange = thisChange || totChange;
                }
//                regionImageStack.addSlice(ref.duplicate());
//                }
            }
//            if (regionImageStack.getSize() == 222) {
//                IJ.wait(0);
//            }
//            regionImageStack.addSlice(regionImage.duplicate());
//            IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "c:\\users\\barry05\\desktop\\regions.tif");
            if (simple) {
                expandRegions(singleImageRegions, regionImage, cellNum, terminal);
            } else {
                expandRegions(singleImageRegions, regionImages, cellNum);
            }
//            regionImageStack.addSlice(regionImage.duplicate());
//            IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "c:\\users\\barry05\\desktop\\regions.tif");
        }
//        for (i = 0; i < cellNum; i++) {
//            Region cell = singleImageRegions.get(i);
//            cell.clearPixels();
//        }
//        IJ.saveAs((new ImagePlus("", regionImageStack)), "TIF", "c:\\users\\barry05\\desktop\\regions.tif");
//        IJ.saveAs(new ImagePlus("", inputImage), "TIF", "C:/users/barry05/desktop/inputImage.tif");
        return regionImage;
    }

    void initDistanceMaps(ImageProcessor inputImage, ByteProcessor regionImage, ArrayList<Region> singleImageRegions, float[][][] distancemaps, ByteProcessor[] regionImages, int width, double filtRad, double thresh) {
        GaussianBlur blurrer = new GaussianBlur();
        /*
         * Image texture (and grey levels) used to control region growth.
         * Standard deviation of grey levels is used as a simple measure of
         * texture.
         */
//        ImageProcessor texture = sdImage(inputImage.duplicate(), 1);
        ImageProcessor texture = inputImage.duplicate();
        texture.findEdges();
        blurrer.blurGaussian(texture, filtRad, filtRad, 0.01);
//        ImageProcessor grad = inputImage.duplicate();
//        grad.findEdges();
        int cellNum = singleImageRegions.size();
        ArrayList<Region> tempRegions = new ArrayList<Region>();
        for (int n = 0; n < cellNum; n++) {
            /*
             * Initialise distance maps. Any non-seed pixels are set to
             * MAX_VALUE. Seed pixels are set to zero distance. Using these seed
             * pixels, temporary regions are added to a temporary ArrayList.
             */
            for (int x = 0; x < width; x++) {
                Arrays.fill(distancemaps[n][x], Float.MAX_VALUE);
            }
            Region cell = singleImageRegions.get(n);
            if (cell != null) {
//            Pixel centre = cell.getCentroids().get(0);
//            ArrayList<Pixel> pixels = cell.getPixels();

                ImageProcessor mask = cell.getMask(regionImage.getWidth(), regionImage.getHeight());
                LinkedList<Pixel> borderPix = cell.getBorderPix();
                Region cellcopy = new Region(inputImage.getWidth(), inputImage.getHeight());
//            int pixsize = pixels.size();
            /*
                 * Copy initial pixels and border pixels to cell copy for distance
                 * map construction. This can probably be replaced with a clone
                 * method.
                 */
//                ArrayList<Pixel> centres = cell.getCentres();
//                Pixel median = centres.get(centres.size() - 1);
                for (int i = 0; i < mask.getWidth(); i++) {
                    for (int j = 0; j < mask.getHeight(); j++) {
                        if (mask.getPixel(i, j) == 0) {
                            distancemaps[n][i][j] = 0.0f;
                        }
//                cellcopy.addPoint(pix);
                    }
                }
                int bordersize = borderPix.size();
                for (int s = 0; s < bordersize; s++) {
                    Pixel pix = borderPix.get(s);
                    int sx = pix.getX();
                    int sy = pix.getY();
                    distancemaps[n][sx][sy] = 0.0f;
                    cellcopy.addBorderPoint(pix);
                }
                cellcopy.calcCentre(borderPix);
                tempRegions.add(cellcopy);
                regionImages[n] = (ByteProcessor) regionImage.duplicate();
            } else {
                tempRegions.add(new Region(inputImage.getWidth(), inputImage.getHeight()));
                regionImages[n] = (ByteProcessor) regionImage.duplicate();
            }
        }
        boolean totChange = true, thisChange;
        while (totChange) {
            totChange = false;
            for (int i = 0; i < cellNum; i++) {
                if (singleImageRegions.get(i) != null) {
                    ByteProcessor tempRef = (ByteProcessor) regionImages[i].duplicate(); // Temporary reference to ensure each pixel is only considered once.
                    Region cell = tempRegions.get(i);
                    if (cell.isActive()) {
                        LinkedList<Pixel> borderPix = cell.getBorderPix();
                        int borderLength = borderPix.size();
                        thisChange = false;
                        for (int j = 0; j < borderLength; j++) {
                            Pixel thispix = borderPix.get(j);
                            /*
                             * thisChange is set to true if dilation occurs at any
                             * border pixel.
                             */
                            thisChange = buildDistanceMaps(tempRef, inputImage, cell,
                                    thispix, distancemaps[i], thresh, texture, i + 1) || thisChange;
                        }
                        cell.setActive(thisChange);
                        totChange = thisChange || totChange; // if all regions cease growing, while loop will exit
                    }
                }
            }
            /*
             * Update each region to new dilated size and update regionImages to
             * assign index to scanned pixels
             */
            expandRegions(tempRegions, regionImages, cellNum);
        }
    }

    float calcDistance(Pixel point, int x, int y, ImageProcessor gradient) {
        return (float) ((Math.pow(gradient.getPixelValue(point.getX(), point.getY())
                - gradient.getPixelValue(x, y), 2.0) + UserVariables.getLambda()) / (1.0 + UserVariables.getLambda()));
//        return (float) Utils.calcDistance(point.getX(), point.getY(), x, y);
//        return Math.abs(point.getX() - x) + Math.abs(point.getY() - y);
    }
    /*
     * Returns an image which illustrates the standard deviation at each point
     * in image. The standard deviation is evaluated in a square neighbourhood
     * of size 2 * window + 1 about each point.
     */

    ImageProcessor sdImage(ImageProcessor image, int window) {
        int width = image.getWidth();
        int height = image.getHeight();
        FloatProcessor sdImage = new FloatProcessor(width, height);
        int windowSide = 2 * window + 1;
        int arraySize = windowSide * windowSide;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double pix[] = new double[arraySize];
                double sum = 0.0;
                int index = 0;
                int i = (x - window < 0) ? 0 : x - window;
                int j = (y - window < 0) ? 0 : y - window;
                for (; (i <= x + window) && (i < width); i++) {
                    for (; (j <= y + window) && (j < height); j++) {
                        pix[index] = image.getPixelValue(i, j);
                        sum += image.getPixelValue(i, j);
                        index++;
                    }
                }
                double mean = sum / index;
                double var = 0.0;
                for (int k = 0; k < index; k++) {
                    var += Math.pow(pix[k] - mean, 2.0);
                }
                sdImage.putPixelValue(x, y, var / sum);
            }
        }
        return sdImage;
    }

    private boolean simpleDilate(ByteProcessor regionImage, ImageProcessor greys, Region cell, Pixel point, int intermediate, double greyThresh, int index) {
        int x = point.getX(), y = point.getY();
        if (regionImage.getPixel(x, y) > intermediate) {
            cell.addExpandedBorderPix(point);
            return false;
        }
        boolean dilate = false, remove = true;
        int width = regionImage.getWidth();
        int height = regionImage.getHeight();
        regionImage.setValue(intermediate);
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (!(Utils.isEdgePixel(i, j, width, height, 0))) {
                    int r = regionImage.getPixel(i, j);
                    double g = greys.getPixelValue(i, j);
                    if ((r == StaticVariables.BACKGROUND || r == intermediate) && (g > greyThresh)) {
                        Pixel p = new Pixel(i, j, index, 1);
                        regionImage.drawPixel(i, j);
                        dilate = true;
                        if (!cell.getExpandedBorder().contains(p)) {
                            cell.addExpandedBorderPix(p);
                        }
                    }
                    r = regionImage.getPixel(i, j);
                    remove = (r == intermediate || r == index) && remove;
                }
            }
        }
        if (!remove) {
            cell.addExpandedBorderPix(point);
            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
                cell.setEdge(true);
            }
        } else if (Utils.isEdgePixel(x, y, width, height, 1)) {
            cell.addExpandedBorderPix(point);
//        } else {
//            cell.setMaskSeed(point);
        }
        return dilate;
    }

    /*
     * Dilate region at current point according to grey levels, texture,
     * gradient and Dijkstra distance map.
     */
    boolean dijkstraDilate(ByteProcessor regionImage, Region region, Pixel point, float[][][] distanceMaps, int intermediate, int index) {
        int width = regionImage.getWidth();
        int height = regionImage.getHeight();
        int x = point.getX();
        int y = point.getY();
        int N = distanceMaps.length;
        boolean dilate = false;
        boolean remove = true;
        regionImage.setValue(intermediate); // No region in regionImage should have the index value INTERMEDIATE
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (!(Utils.isEdgePixel(i, j, width, height, 0))) {
                    if (regionImage.getPixel(i, j) == StaticVariables.BACKGROUND && distanceMaps[index - 1][i][j] < Float.MAX_VALUE) {
                        boolean thisdilate = true;
                        for (int k = 0; k < N; k++) {
                            if (k != index - 1) {
                                /*
                                 * Dilation will occur at the current point if
                                 * distance to the current region's seed is less
                                 * than all others.
                                 */
                                thisdilate = (distanceMaps[index - 1][i][j]
                                        < distanceMaps[k][i][j]) && thisdilate;
                            }
                        }
                        /*
                         * If dilation is to occur, update regionImage and add a
                         * pixel to the expandedBorder of the current region.
                         */
                        if (thisdilate) {
                            Pixel p = new Pixel(i, j, index, 1);
                            regionImage.drawPixel(i, j);
                            dilate = true;
                            region.addExpandedBorderPix(p);
                        }
                    }
                }
                int r = regionImage.getPixel(i, j);
                /*
                 * Remove (x,y) from the borderpixel set and add it to the inner
                 * region set if all surrounding pixels are either set to
                 * INTERMEDIATE or already assigned to index.
                 */
                remove = (r == intermediate || r == index) && remove;
            }
        }
        if (!remove) {
            region.addExpandedBorderPix(point);
            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
                region.setEdge(true);
            }
        } else if (Utils.isEdgePixel(x, y, width, height, 1)) {
            region.addExpandedBorderPix(point);
//        } else {
//            cell.setMaskSeed(point);
        }
//        } else {
//            region.addPoint(point);
//        }
        return dilate;
    }

    /*
     * Values are added to distanceMaps in the neighbourhood of the specified
     * point. Returns false if no values added, true otherwise.
     */
    boolean buildDistanceMaps(ByteProcessor regionImage, ImageProcessor greys, Region region, Pixel point, float[][] distancemap, double thresh, ImageProcessor gradient, int index) {
        int x = point.getX();
        int y = point.getY();
//        ArrayList<Pixel> centres = region.getCentres();
//        Pixel centre = centres.get(centres.size() - 1);
        boolean dilate = false;
        boolean remove = true;
        float minDist = Float.MAX_VALUE;
        regionImage.setValue(intermediate); // No region in regionImage should have the index value INTERMEDIATE
        Pixel p = null;
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                int r = regionImage.getPixel(i, j);
                float g = greys.getPixelValue(i, j);
                /*
                 * Dilation considered if grey-level threshold exceeded
                 */
                if (r == StaticVariables.BACKGROUND && (g > UserVariables.getGreyThresh())) {
                    float dist = calcDistance(point, i, j, gradient);
//                    System.out.println(" x: " + centre.getX() + " y: " + centre.getY() + " i: " + i + " j: " + j + " dist: " + dist);
//                    float dist = (float) ((Math.pow(gradient.getPixelValue(i, j)
//                            - gradient.getPixelValue(x, y), 2.0) + lambda) / (1.0 + lambda)); // See Jones et al., 2005: dx.doi.org/10.1007/11569541_54
                    /*
                     * Dilation will only occur at point minimally distant from
                     * seed
                     */
                    if (dist < minDist) {
                        minDist = dist;
                        p = new Pixel(i, j, index, 1);
                        r = index;
                    }
                }
                /*
                 * Remove (x,y) from the borderpixel set and add it to the inner
                 * region set if all surrounding pixels are either set to
                 * INTERMEDIATE or already assigned to index.
                 */
                remove = (r == intermediate || r == index) && remove;
            }
        }
        if (p != null) {
            regionImage.drawPixel(p.getX(), p.getY());
            dilate = true;
            region.addExpandedBorderPix(p);
            distancemap[p.getX()][p.getY()] = distancemap[x][y] + minDist;
//            distancemap[p.getX()][p.getY()] = minDist;
        }
        if (!remove) {
            region.addExpandedBorderPix(point);
            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
                region.setEdge(true);
            }
        }
//        } else {
//            region.addPoint(point);
//        }
        return dilate;
    }

    /*
     * 'Soft' threshold - enhances contrast and edges.
     */
    void sigmoidFilter(ImageProcessor image, double t) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                double val = image.getPixelValue(x, y);
                double newval = val / (1.0 + Math.exp(-val + t));
                image.putPixelValue(x, y, newval);
            }
        }
    }

    /*
     * Updates regionImages according to the expanded border sets in regions.
     * When complete, borders are dilated to expanded borders and expanded
     * borders are set to null.
     */
    void expandRegions(ArrayList<Region> regions, ByteProcessor regionImage, int N, int terminal) {
        ByteProcessor tempRegionImage = new ByteProcessor(regionImage.getWidth(), regionImage.getHeight());
        tempRegionImage.setValue(StaticVariables.BACKGROUND);
        tempRegionImage.fill();
        for (int i = 0; i < N; i++) {
            Region cell = regions.get(i);
            if (cell != null) {
                LinkedList<Pixel> pixels = cell.getExpandedBorder();
                int borderLength = pixels.size();
                for (int j = 0; j < borderLength; j++) {
                    Pixel current = pixels.get(j);
                    int x = current.getX();
                    int y = current.getY();
                    tempRegionImage.putPixelValue(x, y, tempRegionImage.getPixel(x, y) + 1);
                }
            }
        }
//        IJ.saveAs((new ImagePlus("", tempRegionImage)), "PNG", "c:\\users\\barry05\\desktop\\tempregions.png");
        for (int i = 0; i < N; i++) {
            Region cell = regions.get(i);
            if (cell != null) {
                LinkedList<Pixel> pixels = cell.getExpandedBorder();
                int borderLength = pixels.size();
                for (int j = 0; j < borderLength; j++) {
                    Pixel current = pixels.get(j);
                    int x = current.getX();
                    int y = current.getY();
                    if (tempRegionImage.getPixel(x, y) > 1) {
                        regionImage.putPixelValue(x, y, terminal);
//                                                double minDist = Double.MAX_VALUE;
//                        for (int k = 0; k < N; k++) {
//                            ArrayList<Pixel> centroids = regions.get(k).getCentroids();
//                            Pixel centroid = centroids.get(centroids.size() - 1);
//                            int xc = centroid.getX();
//                            int yc = centroid.getY();
//                            double dist = Utils.calcDistance(x, y, xc, yc);
//                            if (dist < minDist) {
//                                
//                            }
//                        }
                    } else {
                        regionImage.putPixelValue(x, y, i + 1);
                    }
                }
                cell.expandBorder();
            }
        }
    }

    void expandRegions(ArrayList<Region> regions, ByteProcessor[] regionImage, int N) {
        for (int i = 0; i < N; i++) {
            Region cell = regions.get(i);
            if (cell != null) {
                LinkedList<Pixel> pixels = cell.getExpandedBorder();
                int borderLength = pixels.size();
                for (int j = 0; j < borderLength; j++) {
                    Pixel current = pixels.get(j);
                    int x = current.getX();
                    int y = current.getY();
                    regionImage[i].putPixelValue(x, y, i + 1);
                }
                cell.expandBorder();
            }
        }
    }

    /*
     * Correlates data in velImage and sigImage within the Roi's specified in
     * sigrois and velrois.
     */
    void correlativePlot(CellData cellData) {
        double minBlebDuration = UserVariables.getCurveRange() * scaleFactor / 1.0 / (UserVariables.getTimeRes() / 60.0);
        CurveMapAnalyser.findAllCurvatureMinima(cellData, 0, stacks[0].getSize() - 1, minBlebDuration);
//        ArrayList<Bleb> blebs = new ArrayList();
//        CurveMapAnalyser.findAllBlebs(blebs, cellData);
//        IJ.saveAs(new ImagePlus("", CurveMapAnalyser.drawAllBlebs(cellData, blebs, stacks[0])), "TIF", "c:\\users\\barry05\\desktop\\allblebs.tif");
//        CurveMapAnalyser.drawAllMinima(cellData, timeRes, spatialRes, stacks[0], 1, stacks[0].getSize() - 1, minBlebDuration);
        ImageProcessor velMapWithDetections = cellData.getGreyVelMap().duplicate(); // Regions of interest will be drawn on
        cellData.getGreyVelMap().resetRoi();
        cellData.setVelMapWithDetections(velMapWithDetections);
        File thisMeanData;
        PrintWriter thisDataStream;
        File plotDataDir = GenUtils.createDirectory(childDir + delimiter + "Bleb_Data_Files");
        File detectDir = GenUtils.createDirectory(childDir + delimiter + "Detection_Visualisation");
        File mapDir = GenUtils.createDirectory(childDir + delimiter + "Bleb_Signal_Maps");
        ProgressDialog dialog = new ProgressDialog(null, "Plotting Data...", false, true, TITLE);
        dialog.setVisible(true);
        ImageStack detectionStack = new ImageStack(stacks[0].getWidth(),
                stacks[0].getHeight());
        for (int s = 0; s < stacks[0].getSize(); s++) {
            ColorProcessor detectionSlice = new ColorProcessor(detectionStack.getWidth(), detectionStack.getHeight());
            detectionSlice.setChannel(1, (ByteProcessor) ((new TypeConverter(stacks[0].getProcessor(s + 1), true)).convertToByte()));
            detectionSlice.setChannel(2, (ByteProcessor) ((new TypeConverter(stacks[1].getProcessor(s + 1), true)).convertToByte()));
            detectionStack.addSlice(detectionSlice);
        }
        /*
         * Cycle through all sigrois and calculate, as functions of time, mean
         * velocity, mean signal strength for all sigrois (all protrusions).
         */
        int count = 0;
        for (int i = 0; i < cellData.getVelRois().length; i++) {
            if (cellData.getVelRois()[i] != null) {
                Rectangle bounds = cellData.getVelRois()[i].getBounds();
                /*
                 * Ignore this protrusion if it is too small
                 */
                if (((double) bounds.height / cellData.getGreyVelMap().getHeight()) > UserVariables.getBlebLenThresh()
                        && bounds.width > UserVariables.getBlebDurThresh()) {
                    Bleb currentBleb = new Bleb();
                    dialog.updateProgress(i, cellData.getVelRois().length);
                    ArrayList<Double> meanVel = new ArrayList<Double>();
                    ArrayList<Double> sumSig = new ArrayList<Double>();
                    ArrayList<Double> protrusionLength = new ArrayList<Double>();
                    currentBleb.setBounds(bounds);
                    currentBleb.setDetectionStack(detectionStack);
                    currentBleb.setMeanVel(meanVel);
                    currentBleb.setProtrusionLength(protrusionLength);
                    currentBleb.setSumSig(sumSig);
                    currentBleb.setPolys(new ArrayList<Polygon>());
                    currentBleb.setBlebPerimSigs(new ArrayList<ArrayList<Double>>());
                    if (BlebAnalyser.extractAreaSignalData(currentBleb, cellData,
                            count, stacks)) {
                        generateDetectionStack(currentBleb, count);
                        /*
                         * Draw velocity regions on output images
                         */
                        GenUtils.drawRegionWithLabel(velMapWithDetections, cellData.getVelRois()[i],
                                "" + count, cellData.getVelRois()[i].getBounds(), Color.white, 3,
                                new Font("Helvetica", Font.PLAIN, 20), false);
                        /*
                         * Open files to save data for current protrusion
                         */
                        try {
                            thisMeanData = new File(plotDataDir + delimiter + "bleb_data_" + count + ".csv");
                            thisDataStream = new PrintWriter(new FileOutputStream(thisMeanData));
                        } catch (FileNotFoundException e) {
                            System.out.println(e.toString());
                            return;
                        }
                        thisDataStream.println(directory.getAbsolutePath() + "_" + count);
                        thisDataStream.print(StaticVariables.DATA_STREAM_HEADINGS);
                        thisDataStream.println();
                        IJ.saveAs(new ImagePlus("", BlebAnalyser.drawBlebSigMap(currentBleb,
                                UserVariables.getSpatialRes(), UserVariables.isUseSigThresh())),
                                "TIF", mapDir + delimiter + "detection_" + numFormat.format(count) + "_map.tif");
//                        IJ.saveAs(new ImagePlus("", detectionStack), "TIF", currentProtrusion.getDetectionDir()
//                                + "_startindex_" + currentProtrusion.getStartIndex() + ".tif");
                        for (int z = 0; z < meanVel.size(); z++) {
                            meanVel.set(z, meanVel.get(z) / protrusionLength.get(z)); //Divide by protrusion length to get mean
                        }
                        double time0 = bounds.x * 60.0 / UserVariables.getTimeRes();
                        for (int z = 0; z < meanVel.size(); z++) {
                            int t = z + bounds.x;
                            double normFactor = bounds.height * UserVariables.getSpatialRes() * cellData.getScaleFactors()[t];
                            double time = t * 60.0 / UserVariables.getTimeRes();
                            double currentMeanSig;
                            currentMeanSig = sumSig.get(z) / protrusionLength.get(z);
                            thisDataStream.print(String.valueOf(time) + ", "
                                    + String.valueOf(time - time0) + ", "
                                    + String.valueOf(meanVel.get(z)) + ", "
                                    + String.valueOf(sumSig.get(z)) + ", "
                                    + String.valueOf(protrusionLength.get(z)) + ", "
                                    + String.valueOf((sumSig.get(z) / normFactor)) + ", "
                                    + String.valueOf(currentMeanSig) + ", "
                                    + String.valueOf(protrusionLength.get(z) / protrusionLength.get(0)));
                            thisDataStream.println();
                        }
                        thisDataStream.close();
                        count++;
                    }
                    IJ.freeMemory();
                }
            }
        }
        Utils.saveStackAsSeries(detectionStack, detectDir + delimiter, "JPEG", numFormat);
        dialog.dispose();

        IJ.saveAs(new ImagePlus("", velMapWithDetections), "PNG", childDir + delimiter + "Velocity_Map_with_Detected_Regions.png");
    }

    void generateDetectionStack(Bleb currentBleb, int index) {
        int cortexRad = (int) Math.round(UserVariables.getCortexDepth() / UserVariables.getSpatialRes());
        Rectangle bounds = currentBleb.getBounds();
        int duration = currentBleb.getBlebPerimSigs().size();
        ArrayList<Polygon> polys = currentBleb.getPolys();
        int ovalRadius = UserVariables.getOvalRadius();
        int ovalDiam = ovalRadius * 2 + 1;
        for (int timeIndex = bounds.x; timeIndex - bounds.x < duration; timeIndex++) {
            ColorProcessor detectionSlice = (ColorProcessor) currentBleb.getDetectionStack().getProcessor(timeIndex + 1);
            Polygon poly = polys.get(timeIndex - bounds.x);
            ByteProcessor blebMask = BlebAnalyser.drawBlebMask(poly, cortexRad, stacks[0].getWidth(), stacks[0].getHeight(), 255, 0);
            Rectangle polyBounds = poly.getBounds();
            detectionSlice.setColor(Color.yellow);
            detectionSlice.drawString("" + index, polyBounds.x + polyBounds.width / 2,
                    polyBounds.y + polyBounds.height / 2);
            blebMask.invert();
            blebMask.outline();
            blebMask.invert();
            ColorBlitter blitter = new ColorBlitter(detectionSlice);
            blitter.copyBits(blebMask, 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
            detectionSlice.setColor(Color.yellow);
            detectionSlice.drawOval(poly.xpoints[0] - ovalRadius,
                    poly.ypoints[0] - ovalRadius, ovalDiam, ovalDiam);
            detectionSlice.drawOval(poly.xpoints[poly.npoints - 1] - ovalRadius,
                    poly.ypoints[poly.npoints - 1] - ovalRadius, ovalDiam, ovalDiam);
        }
    }

    /**
     * Generates preview segmentation of the image frame specified by sliceIndex
     *
     * @param sliceIndex Frame number of stack to be previewed
     * @return 1- or 2-channel preview image showing segmentation result
     */
    public ImageProcessor[] generatePreview(int sliceIndex) {
        int nCell = initialiseROIs(sliceIndex);
        ImageProcessor cytoProc = stacks[0].getProcessor(sliceIndex);
        Region[][] allRegions = new Region[nCell][1];
        int threshold = getThreshold(cytoProc, UserVariables.isAutoThreshold());
        ArrayList<Region> detectedRegions = findCellRegions(cytoProc, threshold, cellData);
        for (int k = 0; k < nCell; k++) {
            allRegions[k][0] = detectedRegions.get(k);
            cellData[k].setCellRegions(allRegions[k]);
        }
        if (UserVariables.isAnalyseProtrusions()) {
            for (int i = 0; i < nCell; i++) {
                buildOutput(i, 1, true);
                CurveMapAnalyser.findAllCurvatureMinima(cellData[i], sliceIndex, sliceIndex, 0.0);
            }
        }

        /*
         * Generate output for segmentation preview.
         */
        int channels = (stacks[1] == null) ? 1 : 2;
        ImageProcessor regionsOutput[] = new ImageProcessor[channels];
        for (int i = 0; i < channels; i++) {
            TypeConverter outToColor = new TypeConverter(stacks[i].getProcessor(sliceIndex).duplicate(), true);
            regionsOutput[i] = outToColor.convertToRGB();
        }
        for (int r = 0; r < nCell; r++) {
            Region region = detectedRegions.get(r);
            if (region != null) {
                if (region.getCentres().size() < 1) {
                    region.calcCentre(region.getMask(stacks[0].getWidth(), stacks[0].getHeight()));
                }
                ArrayList<Pixel> centres = region.getCentres();
                Pixel centre = centres.get(centres.size() - 1);
                LinkedList<Pixel> border = region.getBorderPix();
//            ArrayList<Pixel> seed = region.getSeedPix();
                for (int i = 0; i < channels; i++) {
                    regionsOutput[i].setColor(Color.red);
                    for (Pixel current : border) {
                        regionsOutput[i].drawDot(current.getX(), current.getY());
                    }
                }
                for (int i = 0; i < channels; i++) {
                    regionsOutput[i].setColor(Color.blue);
                    Utils.drawCross(regionsOutput[i], centre.getX(), centre.getY(), 6);
                }
                if (channels > 1) {
//                ArrayList<Pixel> centroids = region.getCentroids();
//                Pixel centre = centroids.get(centroids.size() - 1);
                    ImageProcessor origMask = region.getMask(stacks[0].getWidth(), stacks[0].getHeight());
                    ImageProcessor shrunkMask = origMask.duplicate();
                    ImageProcessor enlargedMask = origMask.duplicate();
                    int erosions = (int) Math.round(UserVariables.getCortexDepth() / UserVariables.getSpatialRes());
                    for (int e = 0; e < erosions; e++) {
                        shrunkMask.erode();
                        enlargedMask.dilate();
                    }
                    Region shrunkRegion = new Region(shrunkMask, centre);
                    LinkedList<Pixel> shrunkBorder = shrunkRegion.getBorderPix();
                    Region enlargedRegion = new Region(enlargedMask, centre);
                    LinkedList<Pixel> enlargedBorder = enlargedRegion.getBorderPix();
                    for (int i = 0; i < channels; i++) {
                        regionsOutput[i].setColor(Color.green);
                        for (Pixel sCurrent : shrunkBorder) {
                            regionsOutput[i].drawDot(sCurrent.getX(), sCurrent.getY());
                        }
                    }
                    for (Pixel eCurrent : enlargedBorder) {
                        for (int i = 0; i < channels; i++) {
                            regionsOutput[i].drawDot(eCurrent.getX(), eCurrent.getY());
                        }
                    }
                }
                if (UserVariables.isAnalyseProtrusions()) {
                    ArrayList<BoundaryPixel> minPos[] = cellData[r].getCurvatureMinima();
                    for (int i = 0; i < channels; i++) {
                        regionsOutput[i].setColor(Color.yellow);
                        if (minPos[0] != null) {
                            int mpSize = minPos[0].size();
                            for (int j = 0; j < mpSize; j++) {
                                BoundaryPixel currentMin = minPos[0].get(j);
                                int x = (int) Math.round(currentMin.getPrecX() / UserVariables.getSpatialRes());
                                int y = (int) Math.round(currentMin.getPrecY() / UserVariables.getSpatialRes());
                                regionsOutput[i].drawOval(x - 2, y - 2, 5, 5);
                            }
                        }
                    }
                }
            }
        }
        return regionsOutput;
    }

//    void setLookAndFeel() {
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//    }
    void getInitialSeedPoints(ByteProcessor image, ArrayList<Pixel> pixels) {
        ByteProcessor binary = (ByteProcessor) image.duplicate();
        (new GaussianBlur()).blurGaussian(binary, UserVariables.getInitGaussRad(), UserVariables.getInitGaussRad(), 0.01);
        binary.threshold((int) Math.round(Utils.getPercentileThresh(image, UserVariables.getGreyThresh())));
        binary.invert();
        if (binary.isInvertedLut()) {
            binary.invertLut();
        }
        Prefs.blackBackground = false;
        EDM edm = new EDM();
        edm.setup("points", new ImagePlus("", binary));
        edm.run(binary);
        int width = binary.getWidth();
        int height = binary.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (binary.getPixel(x, y) > 0) {
                    pixels.add(new Pixel(x, y));
                }
            }
        }
    }

    int getThreshold(ImageProcessor image, boolean auto) {
        if (UserVariables.isAutoThreshold()) {
            return (new AutoThresholder()).getThreshold(AutoThresholder.Method.Huang, image.getStatistics().histogram);
        } else {
            return (int) Math.round(Utils.getPercentileThresh(image, UserVariables.getGreyThresh()));
        }
    }
}
