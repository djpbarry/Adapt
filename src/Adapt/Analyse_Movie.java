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

import DataProcessing.DataFileAverager;
import IAClasses.BoundaryPixel;
import IAClasses.CrossCorrelation;
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
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.AutoThresholder;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ColorBlitter;
import ij.process.ColorProcessor;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import ui.GUI;

/**
 * Analyse_Movie is designed to quantify cell membrane dynamics and correlate
 * membrane velocity with signal dynamics. It takes as input two movies. One
 * represents the cell cytosol, which should depict a uniform cell against a
 * relatively uniform background. The second movie contains a signal of interest
 * that the user wishes to correlate with membrane dynamics.
 */
public class Analyse_Movie extends NotificationThread implements PlugIn {

//    private final double scaleFactor = 60.0 / 216.0;
    protected static File directory; // root directory
    protected File childDir, // root output directory
            parDir, // output directory for each cell
            velDirName, curvDirName, trajDirName, segDirName;
    private int intermediate, terminal;
    protected String TITLE = StaticVariables.TITLE;
    final String BLEB_DATA_FILES = "Bleb_Data_Files";
    protected final String delimiter = GenUtils.getDelimiter(); // delimiter in directory strings
    private final String channelLabels[] = {"Cytoplasmic channel", "Signal to be correlated"};
    /**
     * Determines the format of printed results
     */
    protected DecimalFormat numFormat = StaticVariables.numFormat; // For formatting results
    protected PointRoi roi = null; // Points used as seeds for cell detection
    private ArrayList<CellData> cellData;
//    private CellData parentCellData;
    protected ImageStack stacks[] = new ImageStack[2];
    private final double trajMin = 5.0;
    protected boolean batchMode = false;
    protected boolean protMode = false;
    protected UserVariables uv;
    private double minLength;
    private int previewSlice;
    private ImageProcessor[] previewImages;

    /**
     * Default constructor
     */
    public Analyse_Movie() {
    }

    public Analyse_Movie(ImageStack[] stacks, boolean protMode, boolean batchMode, UserVariables uv, File parDir, PointRoi roi) {
        this.stacks = stacks;
        this.protMode = protMode;
        this.batchMode = batchMode;
        this.uv = uv;
        this.parDir = parDir;
        this.roi = roi;
//        this.parentCellData = parentCellData;
    }

    /*
     * For debugging - images loaded from file
     */
    void initialise() {
        directory = Utilities.getFolder(directory, null, true);
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
//        Utilities.setLookAndFeel(GUI.class);
        TITLE = TITLE + "_v" + StaticVariables.VERSION + "." + numFormat.format(Revision.Revision.revisionNumber);
        if (IJ.getInstance() != null && WindowManager.getIDList() == null) {
            IJ.error("No Images Open.");
            return;
        }
        if (!batchMode) {
            directory = Utilities.getFolder(directory, "Specify directory for output files...", true); // Specify directory for output
        }
        if (directory == null) {
            return;
        }
        analyse(arg);
        IJ.showStatus(TITLE + " done.");
    }

    protected void analyse(String imageName) {
        int cytoSize, sigSize;
        ImageStack cytoStack;
        ImagePlus cytoImp = new ImagePlus(), sigImp;
        if (IJ.getInstance() == null || batchMode || protMode) {
            cytoStack = stacks[0];
            cytoSize = cytoStack.getSize();
        } else {
            ImagePlus images[] = GenUtils.specifyInputs(channelLabels);
            if (images == null) {
                return;
            }
            cytoImp = images[0];
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
        if (stacks[0].getProcessor(1) instanceof ColorProcessor
                || (stacks[1] != null && stacks[1].getProcessor(1) instanceof ColorProcessor)) {
            IJ.showMessage("Warning: greyscale images should be used for optimal results.");
        }
        /*
         * Create new parent output directory - make sure directory name is
         * unique so old results are not overwritten
         */
        String parDirName = null;
        if (batchMode) {
            parDirName = GenUtils.openResultsDirectory(directory + delimiter + TITLE + delimiter + FilenameUtils.getBaseName(imageName), delimiter);
        } else if (!protMode) {
            parDirName = GenUtils.openResultsDirectory(directory + delimiter + TITLE + delimiter + cytoImp.getShortTitle(), delimiter);
        }
        if (parDirName != null) {
            parDir = new File(parDirName);
        } else if (parDir == null) {
            return;
        }
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        /*
         Convert cyto channel to 8-bit for faster segmentation
         */
        cytoStack = GenUtils.convertStackTo8Bit(stacks[0]);
        stacks[0] = cytoStack;
        if (IJ.getInstance() == null && !protMode) {
            roi = new PointRoi(100, 256);
        }
        if (!(batchMode || protMode)) {
            GUI gui = new GUI(null, true, TITLE, stacks, roi);
            gui.setVisible(true);
            if (!gui.isWasOKed()) {
                return;
            }
            uv = GUI.getUv();
        }
        minLength = protMode ? uv.getBlebLenThresh() : uv.getMinLength();
        String pdLabel = protMode ? "Segmenting Filopodia..." : "Segmenting Cells...";
        ProgressDialog segDialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
        segDialog.setVisible(true);
        cellData = new ArrayList();
        ImageProcessor cytoImage = cytoStack.getProcessor(1).duplicate();
        (new GaussianBlur()).blurGaussian(cytoImage, uv.getGaussRad(), uv.getGaussRad(), 0.01);
        if (initialiseROIs(1, null, -1, 1, cytoImage) < 1) {
            return;
        }
        roi = null;
        /*
         * Cycle through all images in stack and detect cells in each. All
         * detected regions are stored (in order) in stackRegions.
         */
        int thresholds[] = new int[cytoSize];
        ArrayList<Region>[] allRegions = new ArrayList[cytoSize];
        ByteProcessor allMasks = null;
        File filoData;
        PrintWriter filoStream = null;
        if (protMode) {
            try {
                filoData = new File(parDir + delimiter + "FilopodiaVersusTime.csv");
                filoStream = new PrintWriter(new FileOutputStream(filoData));
                filoStream.println("Frame,Number of Filopodia");
            } catch (FileNotFoundException e) {
                System.out.println(e.toString());
                return;
            }
        }
        for (int i = 0; i < cytoSize; i++) {
            segDialog.updateProgress(i, cytoSize);
            cytoImage = cytoStack.getProcessor(i + 1).duplicate();
            (new GaussianBlur()).blurGaussian(cytoImage, uv.getGaussRad(), uv.getGaussRad(), 0.01);
            thresholds[i] = getThreshold(cytoImage, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
            int N = cellData.size();
            if (cytoImage != null) {
                if (i > 0 && protMode) {
                    initialiseROIs(i, allMasks, thresholds[i - 1], i + 1, cytoImage);
                }
                allRegions[i] = findCellRegions(cytoImage, thresholds[i], cellData);
                if (protMode) {
                    allMasks = new ByteProcessor(width, height);
                    allMasks.setColor(Region.FOREGROUND);
                    allMasks.fill();
                    ByteBlitter bb = new ByteBlitter(allMasks);
                    for (int k = 0; k < allRegions[i].size(); k++) {
                        Region current = allRegions[i].get(k);
                        if (current != null) {
                            ImageProcessor currentMask = current.getMask();
                            currentMask.invert();
                            bb.copyBits(currentMask, 0, 0, Blitter.ADD);
                        }
                    }
                }
            }
            int fcount = 0;
            for (int j = 0; j < N; j++) {
                Region current = allRegions[i].get(j);
                if (current != null) {
                    fcount++;
                    /*
                     * Mask from last segmentation used to initialise next
                     * segmentation
                     */
                    ImageProcessor mask = current.getMask();
                    current.calcCentroid(mask);
                    int e = uv.getErosion();
                    for (int k = 0; k < e; k++) {
                        mask.erode();
                    }
                    Pixel seed = current.findSeed(mask);
                    if (seed != null) {
                        Region temp;
                        if (e < 0) {
                            temp = new Region(width, height, seed);
                        } else {
                            temp = new Region(mask, seed);
                        }
                        cellData.get(j).setInitialRegion(temp);
                    } else {
                        cellData.get(j).setInitialRegion(null);
                        cellData.get(j).setEndFrame(i + 1);
                    }
                }
            }
            if (protMode) {
                filoStream.println(i + ", " + fcount);
            }
        }
        if (protMode) {
            filoStream.close();
        }
        for (int i = 0; i < cellData.size(); i++) {
            Region regions[] = new Region[cytoSize];
            for (int j = 0; j < cytoSize; j++) {
                if (allRegions[j].size() > i) {
                    regions[j] = allRegions[j].get(i);
                }
            }
            cellData.get(i).setCellRegions(regions);
            cellData.get(i).setGreyThresholds(thresholds);
        }
        segDialog.dispose();
        /*
         * Analyse the dynamics of each cell, represented by a series of
         * detected regions.
         */
        if ((uv.isGenVis() || uv.isGetFluorDist()) && !protMode) {
            String pdLabel2 = protMode ? "Generating individual filipodia outputs..." : "Generating individual cell outputs...";
            ProgressDialog dialog = new ProgressDialog(null, pdLabel2, false, TITLE, false);
            dialog.setVisible(true);
            for (int index = 0; index < cellData.size(); index++) {
                /*
                 * Create child directory for current cell
                 */
                dialog.updateProgress(index, cellData.size());
                String childDirName = GenUtils.openResultsDirectory(parDir + delimiter + index, delimiter);
                int length = cellData.get(index).getLength();
                if (length > minLength) {
                    childDir = new File(childDirName);
                    buildOutput(index, length, false);
                    if (uv.isGetFluorDist()) {
                        generateFluorMaps(getFluorDists(cellData.get(index), StaticVariables.FLUOR_MAP_HEIGHT));
                    }
                    if (!protMode && uv.isAnalyseProtrusions()) {
                        calcSigThresh(cellData.get(index));
                        if (uv.isBlebDetect()) {
                            findProtrusionsBasedOnVel(cellData.get(index));
                            correlativePlot(cellData.get(index));
                            String normHeadings[] = new String[]{StaticVariables.TOTAL_SIGNAL, StaticVariables.MEAN_SIGNAL};
                            (new DataFileAverager(StaticVariables.DATA_STREAM_HEADINGS,
                                    normHeadings, uv.isDisplayPlots(), StaticVariables.VELOCITY,
                                    StaticVariables.TIME,
                                    StaticVariables.ZEROED_TIME)).run(childDir + delimiter + BLEB_DATA_FILES);
                        } else {
                            ImageStack protStacks[] = new ImageStack[2];
                            protStacks[0] = findProtrusionsBasedOnMorph(cellData.get(index), (int) Math.round(uv.getFiloSize()), 1, cytoSize);
                            protStacks[1] = stacks[1];
                            UserVariables protUV = (UserVariables) uv.clone();
                            protUV.setAnalyseProtrusions(false);
                            protUV.setErosion(2);
                            Analyse_Movie protAM = new Analyse_Movie(protStacks,
                                    true, false, protUV,
                                    new File(GenUtils.openResultsDirectory(childDir + delimiter + "Protrusions", delimiter)), roi);
                            protAM.analyse(null);
                        }
                    }
                }
            }
            dialog.dispose();
            velDirName = GenUtils.createDirectory(parDir + delimiter + "Velocity_Visualisation", false);
            curvDirName = GenUtils.createDirectory(parDir + delimiter + "Curvature_Visualisation", false);
            genCurveVelVis(cellData);
        } else {
            segDirName = GenUtils.createDirectory(parDir + delimiter + "Segmentation_Visualisation", false);
            genSimpSegVis(cellData);
        }
        if (uv.isGetMorph()) {
            getMorphologyData(cellData);
        }
        trajDirName = GenUtils.createDirectory(parDir + delimiter + "Trajectories_Visualisation", false);
        generateCellTrajectories(cellData);
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

    int initialiseROIs(int slice, ByteProcessor masks, int threshold, int start, ImageProcessor input) {
        ArrayList<Pixel> initP = new ArrayList<Pixel>();
        int n;
        if (roi != null) {
            if (roi.getType() == Roi.POINT) {
                n = roi.getNCoordinates();
            } else {
                IJ.error("Point selection required.");
                return -1;
            }
        } else {
//            ByteProcessor image = (ByteProcessor) (new TypeConverter((stacks[0].getProcessor(slice)).duplicate(), true)).convertToByte();
//            (new GaussianBlur()).blurGaussian(input, uv.getGaussRad(), uv.getGaussRad(), 0.01);
            if (threshold < 0) {
                threshold = getThreshold(input, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
            }
            input.threshold(threshold);
            ByteProcessor binary = (ByteProcessor) input.duplicate();
            if (masks != null) {
                ByteBlitter bb = new ByteBlitter(binary);
                bb.copyBits(masks, 0, 0, Blitter.SUBTRACT);
            }
//            (new ImagePlus("",image)).show();
            getSeedPoints(binary, initP);
            n = initP.size();
        }
        int s = cellData.size();
        int N = s + n;
        for (int i = s; i < N; i++) {
            cellData.add(new CellData(start));
            cellData.get(i).setImageWidth(stacks[0].getWidth());
            cellData.get(i).setImageHeight(stacks[0].getHeight());
            Pixel init;
            if (roi != null) {
                init = new Pixel(roi.getXCoordinates()[i] + roi.getBounds().x,
                        roi.getYCoordinates()[i] + roi.getBounds().y);
            } else {
                init = initP.get(i - s);
            }
            if (!Utils.isEdgePixel(init.getX(), init.getY(), stacks[0].getWidth(), stacks[0].getHeight(), 1)) {
                ByteProcessor mask = new ByteProcessor(stacks[0].getWidth(), stacks[0].getHeight());
                mask.setColor(Region.BACKGROUND);
                mask.fill();
                mask.setColor(Region.FOREGROUND);
                mask.drawPixel(init.getX(), init.getY());
                cellData.get(i).setInitialRegion(new Region(mask, init));
                cellData.get(i).setEndFrame(stacks[0].getSize());
            } else {
                cellData.get(i).setInitialRegion(null);
                cellData.get(i).setEndFrame(0);
            }
        }
        return n;
    }

    /*
     * Build velocity and signal maps and generate output visualisations for
     * cell corresponding to index
     */
    void buildOutput(int index, int length, boolean preview) {
        Region[] allRegions = cellData.get(index).getCellRegions();
        ImageStack sigStack = stacks[1];
        File trajFile, segPointsFile;
        PrintWriter trajStream, segStream;
        double scaleFactors[] = new double[length];

        /*
         * Analyse morphology of current cell in all frames and save results in
         * morphology.csv
         */
        int upLength = getMaxBoundaryLength(cellData.get(index), allRegions, index);
        MorphMap curveMap = new MorphMap(length, upLength);
        cellData.get(index).setCurveMap(curveMap);
        cellData.get(index).setScaleFactors(scaleFactors);
        buildCurveMap(allRegions, cellData.get(index));

        if (!preview) {
            /*
             * To obain a uniform map, all boundary lengths (from each frame) are
             * scaled up to the same length. For signal processing convenience, this
             * upscaled length will always be a power of 2.
             */
            MorphMap velMap = new MorphMap(length, upLength);
            MorphMap sigMap = null;
            if (sigStack != null) {
                sigMap = new MorphMap(length, upLength);
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
            if (!prepareOutputFiles(trajStream, segStream, length, 3)) {
                return;
            }
            cellData.get(index).setVelMap(velMap);
            cellData.get(index).setSigMap(sigMap);
            cellData.get(index).setScaleFactors(scaleFactors);
            buildVelSigMaps(index, allRegions, trajStream, segStream, cellData.get(index), cellData.size());
            trajStream.close();
            segStream.close();
            double smoothVelocities[][] = velMap.smoothMap(uv.getTempFiltRad() * uv.getTimeRes() / 60.0, uv.getSpatFiltRad() / uv.getSpatialRes()); // Gaussian smoothing in time and space
            double curvatures[][] = curveMap.smoothMap(0.0, 0.0);
            double sigchanges[][];
            if (sigMap != null) {
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
            cellData.get(index).setGreyVelMap(greyVelMap);
            cellData.get(index).setGreyCurveMap(greyCurvMap);
            cellData.get(index).setMaxVel(maxvel);
            cellData.get(index).setMinVel(minvel);
            cellData.get(index).setGreySigMap(greySigMap);
            cellData.get(index).setColorVelMap(colorVelMap);
            cellData.get(index).setSmoothVelocities(smoothVelocities);
            generateMaps(smoothVelocities, cellData.get(index), index, cellData.size());
            IJ.saveAs(new ImagePlus("", greyVelMap), "TIF", childDir + delimiter + "VelocityMap.tif");
            IJ.saveAs(new ImagePlus("", greyCurvMap), "TIF", childDir + delimiter + "CurvatureMap.tif");
            IJ.saveAs(new ImagePlus("", colorVelMap), "PNG", childDir + delimiter + "ColorVelocityMap.png");
            IJ.saveAs(CrossCorrelation.periodicity2D(greyVelMap, greyVelMap, 100), "TIF",
                    childDir + delimiter + "VelMap_AutoCorrelation.tif");
            if (sigStack != null) {
                IJ.saveAs(new ImagePlus("", greySigMap), "TIF", childDir + delimiter
                        + "SignalMap.tif");
                IJ.saveAs(CrossCorrelation.periodicity2D(greySigMap, greyVelMap, 100), "TIF",
                        childDir + delimiter + "VelMap_SigMap_CrossCorrelation.tif");
                ImageProcessor rateOfSigChange = sigMap.calcRateOfChange(greySigMap);
                IJ.saveAs(new ImagePlus("", rateOfSigChange), "TIF", childDir + delimiter
                        + "ChangeInSignalMap.tif");
                IJ.saveAs(CrossCorrelation.periodicity2D(rateOfSigChange, greyVelMap, 100), "TIF",
                        childDir + delimiter + "VelMap_ChangeInSigMap_CrossCorrelation.tif");
            }
        }
    }

    void getMorphologyData(ArrayList<CellData> cellData) {
//        int measures = Integer.MAX_VALUE;
        ResultsTable rt = Analyzer.getResultsTable();
        Prefs.blackBackground = false;
        double minArea = getMinArea();
        File morph;
        PrintWriter morphStream = null;
        try {
            morph = new File(parDir.getAbsolutePath() + delimiter + "morphology.csv");
            morphStream = new PrintWriter(new FileOutputStream(morph));
        } catch (IOException e) {
            IJ.error("Could not save morphological data file.");
        }
        for (int index = 0; index < cellData.size(); index++) {
            int length = cellData.get(index).getLength();
            if (length > minLength) {
                rt.reset();
                Region[] allRegions = cellData.get(index).getCellRegions();
                int start = cellData.get(index).getStartFrame();
                int end = cellData.get(index).getEndFrame();
                for (int h = start - 1; h < end; h++) {
                    Region current = allRegions[h];
                    ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS,
                            Analyzer.getMeasurements(), rt, minArea, Double.POSITIVE_INFINITY);
                    ImagePlus maskImp = new ImagePlus(String.valueOf(index) + "_" + String.valueOf(h),
                            current.getMask());
                    analyzer.analyze(maskImp);
                }
                int N = rt.getCounter();
                morphStream.println(rt.getColumnHeadings());
                morphStream.println("Cell_" + String.valueOf(index));
                for (int i = 0; i < N; i++) {
                    morphStream.println(rt.getRowAsString(i));
                }
            }
        }
        morphStream.close();
    }

    int getMaxBoundaryLength(CellData cellData, Region[] allRegions, int index) {
        int size = allRegions.length;
        int maxBoundary = 0;
        for (int h = 0; h < size; h++) {
            Region current = allRegions[h];
            if (current != null) {
                ArrayList<Pixel> centres = current.getCentres();
                Pixel centre = centres.get(centres.size() - 1);
                int length = (current.getOrderedBoundary(stacks[0].getWidth(), stacks[0].getHeight(), current.getMask(), centre)).length;
                if (length > maxBoundary) {
                    maxBoundary = length;
                }
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
        paramStream.println(StaticVariables.AUTO_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isAutoThreshold()));
        paramStream.println(StaticVariables.THRESH_METHOD.replaceAll("\\s", "_") + ", " + uv.getThreshMethod());
        paramStream.println(StaticVariables.GREY_SENS.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getGreyThresh()));
        paramStream.println(StaticVariables.SPAT_RES.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSpatialRes()));
        paramStream.println(StaticVariables.TIME_RES.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getTimeRes()));
        paramStream.println(StaticVariables.EROSION.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getErosion()));
        paramStream.println(StaticVariables.SPAT_FILT_RAD.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSpatFiltRad()));
        paramStream.println(StaticVariables.TEMP_FILT_RAD.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getTempFiltRad()));
        paramStream.println(StaticVariables.GAUSS_RAD.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getGaussRad()));
        paramStream.println(StaticVariables.GEN_VIS.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isGenVis()));
        paramStream.println(StaticVariables.GET_MORPH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isGetMorph()));
        paramStream.println(StaticVariables.ANA_PROT.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isAnalyseProtrusions()));
        paramStream.println(StaticVariables.DETECT_BLEB.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isBlebDetect()));
        paramStream.println(StaticVariables.MIN_CURVE_RANGE.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getCurveRange()));
        paramStream.println(StaticVariables.MIN_CURVE_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMinCurveThresh()));
//        paramStream.println(StaticVariables.MAX_CURVE_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMaxCurveThresh()));
        paramStream.println(StaticVariables.PROT_LEN_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getBlebLenThresh()));
        paramStream.println(StaticVariables.PROT_DUR_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getBlebDurThresh()));
        paramStream.println(StaticVariables.CUT_OFF.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getCutOffTime()));
        paramStream.println(StaticVariables.CORTEX_DEPTH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getCortexDepth()));
        paramStream.println(StaticVariables.USE_SIG_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isUseSigThresh()));
        paramStream.println(StaticVariables.SIG_THRESH_FACT.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSigThreshFact()));
        paramStream.println(StaticVariables.SIG_REC_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSigRecoveryThresh()));
        paramStream.println(StaticVariables.SIMP_SEG.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isSimple()));
        paramStream.println(StaticVariables.LAMBDA.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getLambda()));
        paramStream.println(StaticVariables.MIN_TRAJ_LENGTH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMinLength()));
        paramStream.println(StaticVariables.FILO_SIZE.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getFiloSize()));
        paramStream.println(StaticVariables.GEN_SIG_DIST.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isGetFluorDist()));
        paramStream.println(StaticVariables.MIN_MORPH_AREA.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMorphSizeMin()));
        paramStream.println(StaticVariables.VIS_LINE_WIDTH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getVisLineWidth()));
        paramStream.println(StaticVariables.DISPLAY_PLOTS.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isDisplayPlots()));
        return true;
    }

    void buildVelSigMaps(int index, Region[] allRegions, PrintWriter trajStream, PrintWriter segStream, CellData cellData, int total) {
        ImageStack cytoStack = stacks[0];
        ImageStack sigStack = stacks[1];
        MorphMap velMap = cellData.getVelMap();
        MorphMap sigMap = cellData.getSigMap();
        int width = velMap.getWidth();
        int height = velMap.getHeight();
        for (int i = cellData.getStartFrame() - 1; i < width; i++) {
            Region current = allRegions[i];
            ArrayList<Pixel> centres = current.getCentres();
            double xc = centres.get(0).getX();
            double yc = centres.get(0).getY();
            trajStream.println(String.valueOf(i * 60.0 / uv.getTimeRes())
                    + ", " + String.valueOf(xc * uv.getSpatialRes())
                    + ", " + String.valueOf(yc * uv.getSpatialRes()));
            /*
             * Get points for one column (time-point) of map
             */
            Pixel vmPoints[] = current.buildMapCol(current.buildVelImage(cytoStack, i + 1,
                    uv.getTimeRes(), uv.getSpatialRes(), cellData.getGreyThresholds()), height,
                    (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes()));
            Pixel smPoints[] = null;
            if (sigStack != null) {
                smPoints = current.buildMapCol(sigStack.getProcessor(i + 1), height,
                        (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes()));
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
//        if (protMode) {
//            sigMap.allignMapMaxDistToPoint(sigMap.getHeight() / 2, parentCellData);
//        }
    }

    private void buildCurveMap(Region[] allRegions, CellData cellData) {
        MorphMap curveMap = cellData.getCurveMap();
//        int width = curveMap.getWidth();
        int height = curveMap.getHeight();
        int start = cellData.getStartFrame();
        int end = cellData.getEndFrame();
        for (int i = start - 1; i < end; i++) {
            int index = i + 1 - start;
            Region current = allRegions[i];
            ArrayList<Pixel> centres = current.getCentres();
            double xc = centres.get(0).getX();
            double yc = centres.get(0).getY();
            /*
             * Get points for one column (time-point) of map
             */
            Pixel vmPoints[] = current.getOrderedBoundary(stacks[0].getWidth(), stacks[0].getHeight(),
                    current.getMask(), new Pixel(xc, yc, 0.0));
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
                    uv.getCurveRange()), height, false), index);
            cellData.getScaleFactors()[index] = ((double) height) / vmPoints.length;
        }
//        curveMap.allignMap();
    }

    void generateMaps(double[][] smoothVelocities, CellData cellData, int index, int total) {
        boolean sigNull = (cellData.getSigMap() == null);
        int l = smoothVelocities.length;
        MorphMap curveMap = cellData.getCurveMap();
        int upLength = curveMap.getHeight();
        FloatProcessor greyVelMap = cellData.getGreyVelMap();
        FloatProcessor greyCurvMap = cellData.getGreyCurveMap();
        FloatProcessor greySigMap = null;
        ColorProcessor colorVelMap = cellData.getColorVelMap();
        double curvatures[][] = curveMap.smoothMap(0.0, 0.0);
        double sigchanges[][] = null;
        File velStats;
        PrintWriter velStatWriter;
        try {
            velStats = new File(childDir + delimiter + "VelocityAnalysis.csv");
            velStatWriter = new PrintWriter(new FileOutputStream(velStats));
            velStatWriter.println("Frame,% Protruding,% Retracting,Mean Protrusion Velocity (" + IJ.micronSymbol + "m/min), Mean Retraction Velocity (" + IJ.micronSymbol + "m/min)");
            if (!sigNull) {
                sigchanges = cellData.getSigMap().smoothMap(uv.getTempFiltRad() * uv.getTimeRes() / 60.0, uv.getSpatFiltRad() / uv.getSpatialRes());
                greySigMap = cellData.getGreySigMap();
            }
            for (int i = 0; i < l; i++) {
                int neg = 0, pos = 0;
                double negVals = 0.0, posVals = 0.0;
                for (int j = 0; j < upLength; j++) {
                    if (smoothVelocities[i][j] > 0.0) {
                        pos++;
                        posVals += smoothVelocities[i][j];
                    } else {
                        neg++;
                        negVals += smoothVelocities[i][j];
                    }
                    greyVelMap.putPixelValue(i, j, smoothVelocities[i][j]);
                    greyCurvMap.putPixelValue(i, j, curvatures[i][j]);
                    colorVelMap.setColor(getColor(smoothVelocities[i][j], cellData.getMaxVel(), cellData.getMinVel()));
                    colorVelMap.drawPixel(i, j);
                    if (!sigNull && greySigMap != null) {
                        greySigMap.putPixelValue(i, j, sigchanges[i][j]);
                    }
                }
                double pProt = (100.0 * pos) / upLength;
                double meanPos = pos > 0 ? posVals / pos : 0.0;
                double meanNeg = neg > 0 ? negVals / neg : 0.0;
                String pProtS = String.valueOf(pProt);
                String nProtS = String.valueOf(100.0 - pProt);
                velStatWriter.println(i + "," + pProtS + "," + nProtS + ","
                        + String.valueOf(meanPos) + "," + String.valueOf(meanNeg));
            }
            velStatWriter.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
        }
    }

    void genCurveVelVis(ArrayList<CellData> cellDatas) {
        int N = cellDatas.size();
        ImageStack cytoStack = stacks[0];
        String pdLabel = protMode ? "Building Filopodia Visualisations..." : "Building Cell Visualisations...";
        ProgressDialog dialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
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
            velOutput.setLineWidth(uv.getVisLineWidth());
            velOutput.setColor(Color.black);
            velOutput.fill();
            ColorProcessor curveOutput = new ColorProcessor(width, height);
            curveOutput.setLineWidth(uv.getVisLineWidth());
            curveOutput.setColor(Color.black);
            curveOutput.fill();
            for (int n = 0; n < N; n++) {
                int start = cellData.get(n).getStartFrame();
                int end = cellData.get(n).getEndFrame();
                int length = cellData.get(n).getLength();
                if (length > minLength && t + 1 >= start && t < end) {
                    int index = t + 1 - start;
                    double[][] smoothVelocities = cellData.get(n).getSmoothVelocities();
                    Region[] allRegions = cellData.get(n).getCellRegions();
                    MorphMap curveMap = cellData.get(n).getCurveMap();
                    int upLength = curveMap.getHeight();
                    double maxvel = cellData.get(n).getMaxVel();
                    double minvel = cellData.get(n).getMinVel();
                    double xCoords[][] = curveMap.getxCoords();
                    double yCoords[][] = curveMap.getyCoords();
                    double curvatures[][] = curveMap.getzVals();
                    for (int j = 0; j < upLength; j++) {
                        int x = (int) Math.round(xCoords[index][j]);
                        int y = (int) Math.round(yCoords[index][j]);
                        velOutput.setColor(getColor(smoothVelocities[index][j], maxvel, minvel));
                        velOutput.drawDot(x, y);
                        curveOutput.setColor(getColor(curvatures[index][j], maxcurve, mincurve));
                        curveOutput.drawDot(x, y);
                    }
                    velOutput.setColor(Color.blue);
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

    void genSimpSegVis(ArrayList<CellData> cellDatas) {
        int N = cellDatas.size();
        ImageStack cytoStack = stacks[0];
        String pdLabel = protMode ? "Building Filopodia Visualisations..." : "Building Cell Visualisations...";
        ProgressDialog dialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
        dialog.setVisible(true);
        /*
         * Generate various visualisations for output
         */
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        int stackSize = cytoStack.getSize();
        for (int t = 0; t < stackSize; t++) {
            dialog.updateProgress(t, stackSize);
            ColorProcessor output = new ColorProcessor(width, height);
            output.setLineWidth(uv.getVisLineWidth());
            output.setColor(Color.black);
            output.fill();
            for (int n = 0; n < N; n++) {
                int start = cellDatas.get(n).getStartFrame();
                int end = cellDatas.get(n).getEndFrame();
                int length = cellDatas.get(n).getLength();
                if (length > minLength && t + 1 >= start && t < end) {
                    Region[] allRegions = cellDatas.get(n).getCellRegions();
                    Region current = allRegions[t];
                    LinkedList<Pixel> border = current.getBorderPix();
                    output.setColor(Color.yellow);
                    int bsize = border.size();
                    for (int i = 0; i < bsize; i++) {
                        Pixel pix = border.get(i);
                        output.drawDot(pix.getX(), pix.getY());
                    }
                    output.setColor(Color.blue);
                    ArrayList<Pixel> centres = current.getCentres();
                    int cl = centres.size();
                    int xc = (int) Math.round(centres.get(cl - 1).getX());
                    int yc = (int) Math.round(centres.get(cl - 1).getY());
                    output.fillOval(xc - 1, yc - 1, 3, 3);
                    output.drawString(String.valueOf(n + 1), xc + 2, yc + 2);
                }
            }
            IJ.saveAs((new ImagePlus("", output)), "PNG", segDirName.getAbsolutePath() + delimiter + numFormat.format(t));
        }
        dialog.dispose();
    }

    void generateCellTrajectories(ArrayList<CellData> cellDatas) {
        int N = cellDatas.size();
        ImageStack cytoStack = stacks[0];
        String pdLabel = protMode ? "Building Filopodia Trajectories..." : "Building Cell Trajectories...";
        ProgressDialog dialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
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
        trajStream.print("Frame,Time (s),");
        for (int n = 0; n < N; n++) {
            colors[n] = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            if (cellData.get(n).getLength() > minLength) {
                trajStream.print("Cell_" + String.valueOf(n + 1) + "_X,");
                trajStream.print("Cell_" + String.valueOf(n + 1) + "_Y,");
                Region[] allRegions = cellData.get(n).getCellRegions();
                Region current = allRegions[cellData.get(n).getStartFrame() - 1];
                ArrayList<Pixel> centres = current.getCentres();
                int cl = centres.size();
                origins[n][0] = (int) Math.round(centres.get(cl - 1).getX());
                origins[n][1] = (int) Math.round(centres.get(cl - 1).getY());
            }
        }
        trajStream.println();
        for (int t = 0; t < stackSize; t++) {
            trajStream.print(String.valueOf(t) + "," + String.valueOf(t * uv.getTimeRes() / 60.0) + ",");
            dialog.updateProgress(t, stackSize);
            ColorProcessor trajOutput = new ColorProcessor(width, height);
            trajOutput.setColor(StaticVariables.BACKGROUND);
            trajOutput.fill();
            int d = uv.getVisLineWidth();
            int r = (int) Math.floor(d / 2.0);
            for (int n = 0; n < N; n++) {
                trajOutput.setColor(colors[n]);
                int start = cellData.get(n).getStartFrame();
                int end = cellData.get(n).getEndFrame();
                int length = cellData.get(n).getLength();
                if (length > minLength) {
                    if (t + 1 >= start && t < end) {
                        Region[] allRegions = cellData.get(n).getCellRegions();
                        Region current = allRegions[t];
                        ArrayList<Pixel> centres = current.getCentres();
                        int c = centres.size();
                        double x = centres.get(c - 1).getPrecX();
                        double y = centres.get(c - 1).getPrecY();
                        trajOutput.fillOval((int) Math.round(x + xc - origins[n][0]) - r,
                                (int) Math.round(y + yc - origins[n][1]) - r, d, d);
                        trajStream.print(String.valueOf(x) + "," + String.valueOf(y) + ",");
                        if (t + 1 > start) {
                            Region last = allRegions[t - 1];
                            ArrayList<Pixel> lastCentres = last.getCentres();
                            int lc = lastCentres.size();
                            double lx = lastCentres.get(lc - 1).getPrecX();
                            double ly = lastCentres.get(lc - 1).getPrecY();
                            distances[n] += Utils.calcDistance(x, y, lx, ly) * uv.getSpatialRes();
                        }
                    } else {
                        trajStream.print(",,");
                    }
                }
            }
            IJ.saveAs((new ImagePlus("", trajOutput)), "PNG", trajDirName.getAbsolutePath() + delimiter + numFormat.format(t));
            trajStream.println();
        }
        trajStream.print("\nMean Velocity (" + IJ.micronSymbol + "m/min):,");
        for (int n = 0; n < N; n++) {
            int l = cellData.get(n).getLength();
            if (l > minLength) {
                trajStream.print(String.valueOf(distances[n] * uv.getTimeRes() / l) + ",,");
            }
        }
        trajStream.print("\nDirectionality:,");
        for (int n = 0; n < N; n++) {
            int l = cellData.get(n).getLength();
            if (l > minLength) {
                Region current = cellData.get(n).getCellRegions()[cellData.get(n).getEndFrame() - 1];
                ArrayList<Pixel> centres = current.getCentres();
                Pixel centre = centres.get(centres.size() - 1);
                trajStream.print(String.valueOf(uv.getSpatialRes() * Utils.calcDistance(origins[n][0], origins[n][1], centre.getX(), centre.getY())
                        / distances[n]) + ",,");
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

    void findProtrusionsBasedOnVel(CellData cellData) {
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
    }

//    void findProtrusionsBasedOnCurve(CellData cellData) {
////        double minDuration = uv.getBlebDurThresh() / 1.0 / (uv.getTimeRes() / 60.0);
//        ArrayList<BoundaryPixel>[] curvatureMaxima = CurveMapAnalyser.findAllCurvatureExtrema(cellData,
//                cellData.getStartFrame(), cellData.getEndFrame(), false, uv.getMaxCurveThresh(), uv.getCurveRange(), uv);
//        int length = curvatureMaxima.length;
//        ArrayList<Roi> rois = new ArrayList();
//        ArrayList<Integer> indices = new ArrayList();
//        for (int i = 0; i < length; i++) {
//            ArrayList<BoundaryPixel> currentMax = curvatureMaxima[i];
//            if (currentMax != null) {
//                int size = currentMax.size();
//                for (int j = 0; j < size; j++) {
//                    BoundaryPixel pix = currentMax.get(j);
//                    int id = pix.getID();
//                    if (!indices.contains(id)) {
//                        indices.add(id);
//                        int x = pix.getTime();
//                        int y = pix.getPos();
//                        int hh = CurveMapAnalyser.calcScaledCurveRange(CurveMapAnalyser.curveSearchRangeFactor * uv.getCurveRange(),
//                                cellData.getScaleFactors()[x]);
//                        rois.add(new Roi(x, y - hh, 1, 2 * hh + 1));
//                    }
//                }
//            }
//        }
//        Roi output[] = new Roi[rois.size()];
//        cellData.setVelRois(rois.toArray(output));
//    }
    ImageStack findProtrusionsBasedOnMorph(CellData cellData, int reps, int start, int stop) {
        Region regions[] = cellData.getCellRegions();
        ImageStack cyto2 = new ImageStack(stacks[0].getWidth(), stacks[0].getHeight());
        for (int f = start - 1; f < stacks[0].getSize() && f <= stop - 1; f++) {
            ImageProcessor mask = new ByteProcessor(stacks[0].getWidth(), stacks[0].getHeight());
            mask.setColor(Region.BACKGROUND);
            mask.fill();
            if (regions[f] != null) {
                mask = regions[f].getMask();
                ImageProcessor mask2 = mask.duplicate();
                for (int j = 0; j < reps; j++) {
                    mask2.erode();
                }
                for (int j = 0; j < reps; j++) {
                    mask2.dilate();
                }
                mask.invert();
                ByteBlitter bb = new ByteBlitter((ByteProcessor) mask);
                mask2.invert();
                bb.copyBits(mask2, 0, 0, Blitter.SUBTRACT);
            }
            double minArea = getMinArea();
            ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES + ParticleAnalyzer.SHOW_MASKS,
                    0, null, minArea, Double.POSITIVE_INFINITY);
            mask.invert();
            analyzeDetections(null, mask, analyzer);
            ImageProcessor analyzerMask = analyzer.getOutputImage().getProcessor();
            analyzerMask.invertLut();
            cyto2.addSlice(analyzerMask);
        }
        return cyto2;
    }

    void calcSigThresh(CellData cellData) {
        if (uv.isUseSigThresh()) {
            ImageProcessor scaledSigMap = cellData.getGreySigMap().duplicate();
            ImageStatistics sigStats = ImageStatistics.getStatistics(scaledSigMap,
                    Measurements.MEAN + Measurements.STD_DEV, null);
            cellData.setSigThresh(sigStats.mean + uv.getSigThreshFact() * sigStats.stdDev);
        } else {
            cellData.setSigThresh(0.0);
        }
    }

    void analyzeDetections(RoiManager manager, ImageProcessor binmap, ParticleAnalyzer analyzer) {
        ParticleAnalyzer.setRoiManager(manager);
        if (!analyzer.analyze(new ImagePlus("", binmap))) {
            IJ.error("Protrusion analysis failed.");
//            return;
        }
        hideWindows();
    }

    void hideWindows() {
        if (WindowManager.getImageCount() > 0) {
            WindowManager.getImage(WindowManager.getIDList()[WindowManager.getImageCount() - 1]).hide();
        }
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
    private ArrayList<Region> findCellRegions(ImageProcessor inputProc, double threshold, ArrayList<CellData> cellData) {
        int outVal = 1;
        ImageProcessor inputFloatProc = (new TypeConverter(inputProc, true)).convertToFloat(null);
        ImageProcessor inputDup = inputFloatProc.duplicate();
        int width = inputDup.getWidth();
        int height = inputDup.getHeight();
        int n = cellData.size();
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
            Region region = cellData.get(i).getInitialRegion();
            if (region != null) {
                ImageProcessor mask = region.getMask();
                mask.invert();
                mask.multiply((i + 1) / 255.0);
                bb.copyBits(mask, 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
            }
            singleImageRegions.add(region);
            outVal++;
        }
        intermediate = singleImageRegions.size() + 1;
        terminal = intermediate + 1;
        /*
         * Filter image to be used as basis for region growing.
         */
//        (new GaussianBlur()).blurGaussian(inputDup, uv.getGaussRad(), uv.getGaussRad(), 0.01);
        growRegions(indexedRegions, inputDup, singleImageRegions, uv.isSimple(), threshold);
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
        while (totChange) {
            totChange = false;
            ByteProcessor expandedImage = new ByteProcessor(regionImage.getWidth(), regionImage.getHeight());
            expandedImage.setColor(Region.BACKGROUND);
            expandedImage.fill();
            expandedImage.setColor(Region.FOREGROUND);
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
                                    inputImage, cell, thispix, intermediate, threshold, i + 1, expandedImage)
                                    || thisChange;
                        }
                    }
                    cell.setActive(thisChange);
                    totChange = thisChange || totChange;
                }
//                regionImageStack.addSlice(ref.duplicate());
//                }
            }
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
        ImageProcessor texture = inputImage.duplicate();
        texture.findEdges();
        blurrer.blurGaussian(texture, filtRad, filtRad, 0.01);
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
                ImageProcessor mask = cell.getMask();
                LinkedList<Pixel> borderPix = cell.getBorderPix();
                ArrayList<Pixel> centres = cell.getCentres();
                Pixel centre = centres.get(0);
                Region cellcopy = new Region(inputImage.getWidth(), inputImage.getHeight(), centre);
                /*
                 * Copy initial pixels and border pixels to cell copy for distance
                 * map construction. This can probably be replaced with a clone
                 * method.
                 */
                Rectangle bounds = cell.getBounds();
                for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
                    for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
                        if (mask.getPixel(i, j) == 0) {
                            distancemaps[n][i][j] = 0.0f;
                        }
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
                tempRegions.add(cellcopy);
                regionImages[n] = (ByteProcessor) regionImage.duplicate();
            } else {
                tempRegions.add(new Region(inputImage.getWidth(), inputImage.getHeight(), null));
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
                - gradient.getPixelValue(x, y), 2.0) + uv.getLambda()) / (1.0 + uv.getLambda()));
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

    private boolean simpleDilate(ByteProcessor regionImage, ImageProcessor greys, Region cell, Pixel point, int intermediate, double greyThresh, int index, ByteProcessor expandedImage) {
        int x = point.getX(), y = point.getY();
        if (regionImage.getPixel(x, y) > intermediate) {
            cell.addExpandedBorderPix(point);
            expandedImage.drawPixel(x, y);
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
                        Pixel p = new Pixel(i, j, index);
                        regionImage.drawPixel(i, j);
                        dilate = true;
                        if (expandedImage.getPixel(i, j) != Region.FOREGROUND) {
                            cell.addExpandedBorderPix(p);
                            expandedImage.drawPixel(i, j);
                        }
                    }
                    r = regionImage.getPixel(i, j);
                    remove = (r == intermediate || r == index) && remove;
                }
            }
        }
        if (!remove) {
            cell.addExpandedBorderPix(point);
            expandedImage.drawPixel(point.getX(), point.getY());
            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
                cell.setEdge(true);
            }
        } else if (Utils.isEdgePixel(x, y, width, height, 1)) {
            cell.addExpandedBorderPix(point);
            expandedImage.drawPixel(point.getX(), point.getY());
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
                            Pixel p = new Pixel(i, j, index);
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
        }
        return dilate;
    }

    /*
     * Values are added to distanceMaps in the neighbourhood of the specified
     * point. Returns false if no values added, true otherwise.
     */
    boolean buildDistanceMaps(ByteProcessor regionImage, ImageProcessor greys, Region region, Pixel point, float[][] distancemap, double thresh, ImageProcessor gradient, int index) {
        int x = point.getX();
        int y = point.getY();
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
                if (r == StaticVariables.BACKGROUND && (g > thresh)) {
                    float dist = calcDistance(point, i, j, gradient);
                    /*
                     * Dilation will only occur at point minimally distant from
                     * seed
                     */
                    if (dist < minDist) {
                        minDist = dist;
                        p = new Pixel(i, j, index);
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
        }
        if (!remove) {
            region.addExpandedBorderPix(point);
            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
                region.setEdge(true);
            }
        }
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
        cellData.setCurvatureMinima(CurveMapAnalyser.findAllCurvatureExtrema(cellData, cellData.getStartFrame(), cellData.getEndFrame(), true, uv.getMinCurveThresh(), uv.getCurveRange(), uv, trajMin));
//        cellData.setCurvatureMaxima(CurveMapAnalyser.findAllCurvatureExtrema(cellData, cellData.getStartFrame(), cellData.getEndFrame(), false, uv.getMaxCurveThresh(), uv.getCurveRange(), uv, trajMin));
//        CurveMapAnalyser.drawAllExtrema(cellData, uv.getTimeRes(), uv.getSpatialRes(),
//                stacks[0], cellData.getStartFrame(), cellData.getEndFrame(), 0.0);
        ImageProcessor velMapWithDetections = cellData.getGreyVelMap().duplicate(); // Regions of interest will be drawn on
        cellData.getGreyVelMap().resetRoi();
        cellData.setVelMapWithDetections(velMapWithDetections);
        File thisMeanData, blebCount;
        PrintWriter thisDataStream, blebCountStream;
        File plotDataDir = GenUtils.createDirectory(childDir + delimiter + BLEB_DATA_FILES, false);
        File detectDir = GenUtils.createDirectory(childDir + delimiter + "Detection_Visualisation", false);
        File mapDir = GenUtils.createDirectory(childDir + delimiter + "Bleb_Signal_Maps", false);
        String pdLabel = protMode ? "Plotting filopodia data..." : "Plotting cell data...";
        ProgressDialog dialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
        dialog.setVisible(true);
        ImageStack detectionStack = new ImageStack(stacks[0].getWidth(),
                stacks[0].getHeight());
        for (int s = 0; s < stacks[0].getSize(); s++) {
            ColorProcessor detectionSlice = new ColorProcessor(detectionStack.getWidth(), detectionStack.getHeight());
            detectionSlice.setChannel(1, (ByteProcessor) ((new TypeConverter(stacks[0].getProcessor(s + 1), true)).convertToByte()));
            if (stacks[1] != null) {
                detectionSlice.setChannel(2, (ByteProcessor) ((new TypeConverter(stacks[1].getProcessor(s + 1), true)).convertToByte()));
            }
            detectionStack.addSlice(detectionSlice);
        }
        /*
         * Cycle through all sigrois and calculate, as functions of time, mean
         * velocity, mean signal strength for all sigrois (all protrusions).
         */
        try {
            blebCount = new File(childDir + delimiter + "BlebsVersusTime.csv");
            blebCountStream = new PrintWriter(new FileOutputStream(blebCount));
            blebCountStream.println("Frame,Number of Blebs");
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
            return;
        }
        int blebFrameCount[] = new int[stacks[0].getSize()];
        Arrays.fill(blebFrameCount, 0);
        int count = 0;
        for (int i = 0; i < cellData.getVelRois().length; i++) {
            if (cellData.getVelRois()[i] != null) {
                Rectangle bounds = cellData.getVelRois()[i].getBounds();
                /*
                 * Ignore this protrusion if it is too small
                 */
                if (((double) bounds.height / cellData.getGreyVelMap().getHeight()) > uv.getBlebLenThresh()
                        && bounds.width > uv.getBlebDurThresh()) {
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
                    if (stacks[1] != null && BlebAnalyser.extractAreaSignalData(currentBleb, cellData,
                            count, stacks, uv)) {
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
                        for (int d = 0; d < StaticVariables.DATA_STREAM_HEADINGS.length; d++) {
                            thisDataStream.print(StaticVariables.DATA_STREAM_HEADINGS[d] + ",");
                        }
                        thisDataStream.println();
                        IJ.saveAs(new ImagePlus("", BlebAnalyser.drawBlebSigMap(currentBleb,
                                uv.getSpatialRes(), uv.isUseSigThresh())),
                                "TIF", mapDir + delimiter + "detection_" + numFormat.format(count) + "_map.tif");
                        for (int z = 0; z < meanVel.size(); z++) {
                            meanVel.set(z, meanVel.get(z) / protrusionLength.get(z)); //Divide by protrusion length to get mean
                        }
                        double time0 = bounds.x * 60.0 / uv.getTimeRes();
                        for (int z = 0; z < meanVel.size(); z++) {
                            int t = z + bounds.x;
//                            double normFactor = bounds.height * uv.getSpatialRes() * cellData.getScaleFactors()[t];
                            double time = t * 60.0 / uv.getTimeRes();
                            double currentMeanSig;
                            currentMeanSig = sumSig.get(z) / protrusionLength.get(z);
                            thisDataStream.print(String.valueOf(time - time0) + ", "
                                    + String.valueOf(meanVel.get(z)) + ", "
                                    + String.valueOf(sumSig.get(z)) + ", "
                                    + String.valueOf(currentMeanSig) + ", "
                                    + String.valueOf(protrusionLength.get(z)) + ", "
                                    + String.valueOf(protrusionLength.get(z) / protrusionLength.get(0)));
                            thisDataStream.println();
                            blebFrameCount[t]++;
                        }
                        thisDataStream.close();
                        count++;
                    }
                    IJ.freeMemory();
                }
            }
        }
        for (int b = 0; b < blebFrameCount.length; b++) {
            blebCountStream.println(b + "," + blebFrameCount[b]);
        }
        blebCountStream.close();
        Utils.saveStackAsSeries(detectionStack, detectDir + delimiter, "JPEG", numFormat);
        dialog.dispose();

        IJ.saveAs(new ImagePlus("", velMapWithDetections), "PNG", childDir + delimiter + "Velocity_Map_with_Detected_Regions.png");
    }

    void generateDetectionStack(Bleb currentBleb, int index) {
        int cortexRad = (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes());
        Rectangle bounds = currentBleb.getBounds();
        int duration = currentBleb.getBlebPerimSigs().size();
        ArrayList<Polygon> polys = currentBleb.getPolys();
        ImageStack detectionStack = currentBleb.getDetectionStack();
//        Random r = new Random();
//        int red = r.nextInt(256);
//        int green = r.nextInt(256);
//        int blue = r.nextInt(256);
        for (int timeIndex = bounds.x; timeIndex - bounds.x < duration && timeIndex < detectionStack.getSize(); timeIndex++) {
            ColorProcessor detectionSlice = (ColorProcessor) detectionStack.getProcessor(timeIndex + 1);
            Polygon poly = polys.get(timeIndex - bounds.x);
            ByteProcessor blebMask = BlebAnalyser.drawBlebMask(poly, cortexRad, stacks[0].getWidth(), stacks[0].getHeight(), 255, 0);
            blebMask.invert();
            blebMask.outline();
            blebMask.invert();
//            ImageStack redC = new ImageStack(blebMask.getWidth(), blebMask.getHeight());
//            redC.addSlice(blebMask.duplicate());
//            redC.getProcessor(1).multiply(red / 255.0);
//            ImageStack greenC = new ImageStack(blebMask.getWidth(), blebMask.getHeight());
//            greenC.addSlice(blebMask.duplicate());
//            greenC.getProcessor(1).multiply(green / 255.0);
//            ImageStack blueC = new ImageStack(blebMask.getWidth(), blebMask.getHeight());
//            blueC.addSlice(blebMask.duplicate());
//            blueC.getProcessor(1).multiply(blue / 255.0);
//            ImageStack merged = RGBStackMerge.mergeStacks(redC, greenC, blueC, false);
            ColorBlitter blitter = new ColorBlitter(detectionSlice);
            blitter.copyBits(blebMask, 0, 0, Blitter.COPY_ZERO_TRANSPARENT);
            Rectangle box = poly.getBounds();
            int sx = box.x + box.width / 2;
            int sy = box.y + box.height / 2;
            detectionSlice.setColor(Color.yellow);
            detectionSlice.drawString(String.valueOf(index), sx, sy);
        }
    }

    /**
     * Generates preview segmentation of the image frame specified by sliceIndex
     *
     * @param sliceIndex Frame number of stack to be previewed
     */
    public void generatePreview(int sliceIndex) {
        cellData = new ArrayList();
        ImageProcessor cytoProc = GenUtils.convertStackTo8Bit(stacks[0]).getProcessor(sliceIndex);
        (new GaussianBlur()).blurGaussian(cytoProc, uv.getGaussRad(), uv.getGaussRad(), 0.01);
        int threshold = getThreshold(cytoProc, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
        int nCell = initialiseROIs(sliceIndex, null, -1, sliceIndex, cytoProc);
        Region[][] allRegions = new Region[nCell][stacks[0].getSize()];
        ArrayList<Region> detectedRegions = findCellRegions(cytoProc, threshold, cellData);
        for (int k = 0; k < nCell; k++) {
            allRegions[k][sliceIndex - 1] = detectedRegions.get(k);
            cellData.get(k).setCellRegions(allRegions[k]);
            cellData.get(k).setEndFrame(sliceIndex);
        }
        if (uv.isAnalyseProtrusions()) {
            for (int i = 0; i < nCell; i++) {
                buildOutput(i, 1, true);
                cellData.get(i).setCurvatureMinima(CurveMapAnalyser.findAllCurvatureExtrema(cellData.get(i),
                        sliceIndex, sliceIndex, true, uv.getMinCurveThresh(),
                        uv.getCurveRange(), uv, 0.0));
//                cellData.get(i).setCurvatureMaxima(CurveMapAnalyser.findAllCurvatureExtrema(cellData.get(i),
//                        sliceIndex, sliceIndex, false, uv.getMaxCurveThresh(),
//                        uv.getCurveRange(), uv, 0.0));
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
                ArrayList<Pixel> centres = region.getCentres();
                Pixel centre = centres.get(centres.size() - 1);
                LinkedList<Pixel> border = region.getBorderPix();
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
                    ImageProcessor origMask = region.getMask();
                    ImageProcessor shrunkMask = origMask.duplicate();
                    ImageProcessor enlargedMask = origMask.duplicate();
                    int erosions = (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes());
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
                    int esize = enlargedBorder.size();
                    for (int i = 0; i < channels; i++) {
                        for (int eb = 0; eb < esize; eb++) {
                            Pixel eCurrent = enlargedBorder.get(eb);
                            regionsOutput[i].drawDot(eCurrent.getX(), eCurrent.getY());
                        }
                    }
                }
                if (uv.isAnalyseProtrusions()) {
                    if (uv.isBlebDetect()) {
                        ArrayList<BoundaryPixel> minPos[] = cellData.get(r).getCurvatureMinima();
//                        ArrayList<BoundaryPixel> maxPos[] = cellData.get(r).getCurvatureMaxima();
                        for (int i = 0; i < channels; i++) {
                            if (minPos[0] != null) {
                                regionsOutput[i].setColor(Color.yellow);
                                int minpSize = minPos[0].size();
                                for (int j = 0; j < minpSize; j++) {
                                    BoundaryPixel currentMin = minPos[0].get(j);
                                    int x = (int) Math.round(currentMin.getPrecX());
                                    int y = (int) Math.round(currentMin.getPrecY());
                                    regionsOutput[i].drawOval(x - 4, y - 4, 9, 9);
                                }
//                                if (maxPos[0] != null) {
//                                    regionsOutput[i].setColor(Color.MAGENTA);
//                                    int maxpSize = maxPos[0].size();
//                                    for (int j = 0; j < maxpSize; j++) {
//                                        BoundaryPixel currentMax = maxPos[0].get(j);
//                                        int x = (int) Math.round(currentMax.getPrecX());
//                                        int y = (int) Math.round(currentMax.getPrecY());
//                                        regionsOutput[i].drawOval(x - 4, y - 4, 9, 9);
//                                    }
//                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < channels; i++) {
                            regionsOutput[i].setColor(Color.yellow);
                        }
                        ImageStack filoStack = findProtrusionsBasedOnMorph(cellData.get(r), (int) Math.round(uv.getFiloSize()), sliceIndex, sliceIndex);
                        ByteProcessor filoBin = (ByteProcessor) filoStack.getProcessor(1);
//                        IJ.saveAs((new ImagePlus("",filoBin)), "PNG", "C:/users/barry05/desktop/filoBin.png");
                        filoBin.outline();
                        for (int y = 0; y < filoBin.getHeight(); y++) {
                            for (int x = 0; x < filoBin.getWidth(); x++) {
                                if (filoBin.getPixel(x, y) < Region.BACKGROUND) {
                                    for (int i = 0; i < channels; i++) {
                                        regionsOutput[i].drawPixel(x, y);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        previewImages = regionsOutput;
    }

    void getSeedPoints(ByteProcessor binary, ArrayList<Pixel> pixels) {
        binary.invert();
        if (binary.isInvertedLut()) {
            binary.invertLut();
        }
        ResultsTable rt = Analyzer.getResultsTable();
        rt.reset();
        Prefs.blackBackground = false;
        double minArea = getMinArea();
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES,
                Measurements.CENTROID, rt, minArea, Double.POSITIVE_INFINITY);
        analyzeDetections(null, binary, analyzer);
        int count = rt.getCounter();
        if (count > 0) {
            float x[] = rt.getColumn(rt.getColumnIndex("X"));
            float y[] = rt.getColumn(rt.getColumnIndex("Y"));
            for (int i = 0; i < count; i++) {
                pixels.add(new Pixel(x[i], y[i]));
            }
        }
    }

    int getThreshold(ImageProcessor image, boolean auto, double thresh, String method) {
        if (auto) {
            return (new AutoThresholder()).getThreshold(method, image.getStatistics().histogram);
        } else {
            return (int) Math.round(Utils.getPercentileThresh(image, thresh));
        }
    }

    double getMinArea() {
        return uv.getMorphSizeMin() / (Math.pow(uv.getSpatialRes(), 2.0));
    }

    FloatProcessor[] getFluorDists(CellData cellData, int height) {
        Region[] regions = cellData.getCellRegions();
        FloatProcessor dists[] = new FloatProcessor[2];
        int start = cellData.getStartFrame();
        int end = cellData.getEndFrame();
        int width = 1 + end - start;
        dists[0] = new FloatProcessor(width, height);
        dists[1] = new FloatProcessor(width, height);
        FloatBlitter meanBlitter = new FloatBlitter(dists[0]);
        FloatBlitter stdBlitter = new FloatBlitter(dists[1]);
        Mean mean = new Mean();
        StandardDeviation std = new StandardDeviation();
        for (int i = start; i <= end; i++) {
            Region current = (Region) regions[i - 1].clone();
            ArrayList<Double> means = new ArrayList();
            ArrayList<Double> stds = new ArrayList();
            int index = 0;
            while (current.shrink(2, false, index)) {
                Pixel[] pix = current.buildMapCol(stacks[1].getProcessor(i), height, 3);
                double pixVals[] = new double[pix.length];
                for (int j = 0; j < height; j++) {
                    pixVals[j] = pix[j].getZ();
                }
                means.add(mean.evaluate(pixVals, 0, height));
                stds.add(std.evaluate(pixVals));
                index++;
            }
            FloatProcessor meanCol = new FloatProcessor(1, means.size());
            FloatProcessor stdCol = new FloatProcessor(1, stds.size());
            for (int k = 0; k < meanCol.getHeight(); k++) {
                meanCol.putPixelValue(0, k, means.get(k));
                stdCol.putPixelValue(0, k, stds.get(k));
            }
            meanCol.setInterpolate(true);
            meanCol.setInterpolationMethod(ImageProcessor.BILINEAR);
            stdCol.setInterpolate(true);
            stdCol.setInterpolationMethod(ImageProcessor.BILINEAR);
            meanBlitter.copyBits(meanCol.resize(1, height), i - start, 0, Blitter.COPY);
            stdBlitter.copyBits(stdCol.resize(1, height), i - start, 0, Blitter.COPY);
        }
        return dists;
    }

    void generateFluorMaps(FloatProcessor fluorMaps[]) {
        File mean, std;
        PrintWriter meanStream, stdStream;
        try {
            mean = new File(childDir + delimiter + "MeanFluorescenceIntensity.csv");
            meanStream = new PrintWriter(new FileOutputStream(mean));
            std = new File(childDir + delimiter + "STDFluorescenceIntensity.csv");
            stdStream = new PrintWriter(new FileOutputStream(std));
            meanStream.println("Normalised Distance from Cell Edge,Mean Fluorescence Intensity (AU)");
            stdStream.println("Normalised Distance from Cell Edge,Standard Deviation of Fluorescence Intensity (AU)");
            int mapHeight = fluorMaps[0].getHeight();
            for (int y = 0; y < mapHeight; y++) {
                String normDist = String.valueOf(((double) y) / (mapHeight - 1.0));
                meanStream.println(normDist + "," + fluorMaps[0].getPixelValue(0, y));
                stdStream.println(normDist + "," + fluorMaps[1].getPixelValue(0, y));
            }
            meanStream.close();
            stdStream.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.toString());
        }
    }

    public ImageProcessor[] getPreviewImages() {
        return previewImages;
    }

    public void preparePreview(int slice, UserVariables uv) {
        this.previewSlice = slice;
        this.uv = uv;
    }

    public void doWork() {
        generatePreview(previewSlice);
    }
}
