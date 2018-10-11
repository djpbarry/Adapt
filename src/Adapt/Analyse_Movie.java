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

import Cell.MorphMap;
import UserVariables.UserVariables;
import Cell.CellData;
import Curvature.CurveAnalyser;
import IAClasses.BoundaryPixel;
import IAClasses.CrossCorrelation;
import IAClasses.DSPProcessor;
import IAClasses.ProgressDialog;
import IAClasses.Region;
import IAClasses.Utils;
import IO.DataWriter;
import IO.PropertyWriter;
import Output.MultiThreadedOutputGenerator;
import Segmentation.RegionGrower;
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
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ColorBlitter;
import ij.process.ColorProcessor;
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.io.FilenameUtils;
import ui.GUI;
import UtilClasses.GenVariables;
import Visualisation.MultiThreadedVisualisationGenerator;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;

/**
 * Analyse_Movie is designed to quantify cell membrane dynamics and correlate
 * membrane velocity with signal dynamics. It takes as input two movies. One
 * represents the cell cytosol, which should depict a uniform cell against a
 * relatively uniform background. The second movie contains a signal of interest
 * that the user wishes to correlate with membrane dynamics.
 */
public class Analyse_Movie extends NotificationThread implements PlugIn {

    protected static File directory = new File("D:\\debugging\\adapt_debug\\output"); // root directory
    protected File childDir, // root output directory
            parDir, // output directory for each cell
            velDirName, curvDirName, segDirName;
    protected String TITLE = StaticVariables.TITLE;
    final String BLEB_DATA_FILES = "Bleb_Data_Files";
    protected final String delimiter = GenUtils.getDelimiter(); // delimiter in directory strings
    private final String channelLabels[] = {"Cytoplasmic channel", "Signal to be correlated"};
    protected DecimalFormat numFormat = StaticVariables.numFormat; // For formatting results
    protected PointRoi roi = null; // Points used as seeds for cell detection
    private ArrayList<CellData> cellData;
    protected ImageStack stacks[] = new ImageStack[2];
    private final double trajMin = 5.0;
    protected boolean batchMode = false;
    protected boolean protMode = false;
    protected UserVariables uv;
    private double minLength;
    private int previewSlice;
    private ImageProcessor[] previewImages;
    private boolean selectiveOutput = false;
    private Properties props;

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
        this.selectiveOutput = this.roi != null;
    }

    /*
     * For debugging - images loaded from file
     */
    void initialise() {
        ImagePlus imp1 = IJ.openImage();
        stacks[0] = imp1.getImageStack();
        ImagePlus imp2 = IJ.openImage();
        if (imp2 != null) {
            stacks[1] = imp2.getImageStack();
        }
    }

    /**
     * Opens GUIs for user to specify directory for output then runs analysis
     *
     * @param arg redundant
     */
    @Override
    public void run(String arg) {
//        MacroWriter.write();
        TITLE = TITLE + "_v" + StaticVariables.VERSION + "." + numFormat.format(Revision.Revision.revisionNumber);
        if (IJ.getInstance() != null && WindowManager.getIDList() == null) {
            IJ.error("No Images Open.");
            return;
        }
        try {
            if (!batchMode) {
                directory = Utilities.getFolder(directory, "Specify directory for output files...", true); // Specify directory for output
            }
        } catch (Exception e) {
            IJ.log(e.toString());
        }
        if (directory == null) {
            return;
        }
        analyse(arg);
        try {
            PropertyWriter.printProperties(props, parDir.getAbsolutePath(), TITLE, true);
        } catch (IOException e) {
            IJ.log("Failed to create properties file.");
        }
        IJ.showStatus(TITLE + " done.");
    }

    public void analyse(String imageName) {
        int cytoSize, sigSize;
        ImageStack cytoStack;
        ImagePlus cytoImp = new ImagePlus(), sigImp;
        if (IJ.getInstance() == null || batchMode || protMode) {
            cytoStack = stacks[0];
            cytoSize = cytoStack.getSize();
//            roi = new PointRoi(new float[]{288, 244, 956}, new float[]{532, 346, 364});
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
        if (roi != null) {
            selectiveOutput = true;
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
            parDirName = GenUtils.openResultsDirectory(directory + delimiter + TITLE + delimiter + FilenameUtils.getBaseName(imageName));
        } else if (!protMode) {
            parDirName = GenUtils.openResultsDirectory(directory + delimiter + TITLE + delimiter + cytoImp.getShortTitle());
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
        cytoStack = GenUtils.convertStack(stacks[0], 8);
        stacks[0] = cytoStack;
        if (!(batchMode || protMode)) {
            GUI gui = new GUI(null, true, TITLE, stacks, roi);
            gui.setVisible(true);
            if (!gui.isWasOKed()) {
                return;
            }
            uv = GUI.getUv();
            props = gui.getProperties();
        }
        minLength = protMode ? uv.getBlebLenThresh() : uv.getMinLength();
        String pdLabel = protMode ? "Segmenting Filopodia..." : "Segmenting Cells...";
        ProgressDialog segDialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
        segDialog.setVisible(true);
        cellData = new ArrayList<>();
        ImageProcessor cytoImage = cytoStack.getProcessor(1).duplicate();
        (new GaussianBlur()).blurGaussian(cytoImage, uv.getGaussRad(), uv.getGaussRad(), 0.01);
        RegionGrower.initialiseROIs(null, -1, 1, cytoImage, roi, stacks[0].getWidth(), stacks[0].getHeight(), stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
//        if (initialiseROIs(1, null, -1, 1, cytoImage) < 1) {
//            IJ.error(TITLE, "No cells detected!");
//            segDialog.dispose();
//            return;
//        }
        roi = null;
        /*
         * Cycle through all images in stack and detect cells in each. All
         * detected regions are stored (in order) in stackRegions.
         */
        int thresholds[] = new int[cytoSize];
        ArrayList<ArrayList<Region>> allRegions = new ArrayList<>();
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
//            if (allMasks != null) {
//                IJ.saveAs(new ImagePlus("", allMasks), "PNG", String.format("D:\\debugging\\adapt_debug\\output\\%s_%d.png", "AllMasksPreErode", i));
//            }
            segDialog.updateProgress(i, cytoSize);
            cytoImage = cytoStack.getProcessor(i + 1).duplicate();
            (new GaussianBlur()).blurGaussian(cytoImage, uv.getGaussRad(), uv.getGaussRad(), 0.01);
            thresholds[i] = RegionGrower.getThreshold(cytoImage, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
            int N = cellData.size();
            if (cytoImage != null) {
                allRegions.add(RegionGrower.findCellRegions(cytoImage, thresholds[i], cellData));
            }
            int fcount = 0;
            for (int j = 0; j < N; j++) {
                Region current = allRegions.get(i).get(j);
                if (current != null) {
                    fcount++;
                    /*
                     * Mask from last segmentation used to initialise next
                     * segmentation
                     */
                    ImageProcessor mask = current.getMask();
//                    IJ.saveAs(new ImagePlus("", mask), "PNG", String.format("D:\\debugging\\adapt_debug\\output\\%s_%d_%d.png", "MaskPreErode", i, j));
                    current.calcCentroid(mask);
                    Rectangle bounds = current.getBounds();
                    bounds.grow(2, 2);
                    mask.setRoi(bounds);
                    int e = uv.getErosion();
                    for (int k = 0; k < e; k++) {
                        mask.erode();
                    }
//                    IJ.saveAs(new ImagePlus("", mask), "PNG", String.format("D:\\debugging\\adapt_debug\\output\\%s_%d_%d.png", "MaskPostErode", i, j));
                    short seed[] = current.findSeed(mask);
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
            allMasks = new ByteProcessor(width, height);
            allMasks.setColor(Region.MASK_FOREGROUND);
            allMasks.fill();
            ByteBlitter bb = new ByteBlitter(allMasks);
            for (int k = 0; k < allRegions.get(i).size(); k++) {
                Region current = allRegions.get(i).get(k);
                if (current != null) {
                    ImageProcessor currentMask = current.getMask();
//                    IJ.saveAs(new ImagePlus("", currentMask), "PNG", String.format("D:\\debugging\\adapt_debug\\output\\%s_%d_%d.png", "MaskPostErode2", i, k));
                    currentMask.invert();
                    bb.copyBits(currentMask, 0, 0, Blitter.ADD);
                    current.setFinalMask();
                }
            }
//                IJ.saveAs(new ImagePlus("", allMasks), "PNG", String.format("D:\\debugging\\adapt_debug\\output\\%s_%d.png", "AllMasksPostErode", i));
            if (i > 0) {
                RegionGrower.initialiseROIs(allMasks, thresholds[i], i + 2, cytoImage, roi, stacks[0].getWidth(), stacks[0].getHeight(), stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
            }
        }
        if (protMode) {
            filoStream.close();
        }
        for (int i = 0; i < cellData.size(); i++) {
            Region regions[] = new Region[cytoSize];
            for (int j = 0; j < cytoSize; j++) {
                if (allRegions.get(j).size() > i) {
                    regions[j] = allRegions.get(j).get(i);
                }
            }
            cellData.get(i).setCellRegions(regions);
            cellData.get(i).setGreyThresholds(thresholds);
        }
        segDialog.dispose();
        if (selectiveOutput) {
            ArrayList<CellData> filteredCells = filterCells(cellData);
            cellData = filteredCells;
        }
        /*
         * Analyse the dynamics of each cell, represented by a series of
         * detected regions.
         */
        if ((uv.isGenVis() || uv.isGetFluorDist()) && !protMode) {
            MultiThreadedOutputGenerator outGen = new MultiThreadedOutputGenerator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), cellData,
                    parDir.getAbsolutePath(), protMode, uv, childDir, stacks[1],
                    stacks[0], directory, roi);
            outGen.run();
            saveFluorData(outGen.getFluorData());
            velDirName = GenUtils.createDirectory(parDir + delimiter + "Velocity_Visualisation", false);
            curvDirName = GenUtils.createDirectory(parDir + delimiter + "Curvature_Visualisation", false);
            genCurveVelVis(cellData);
        } else {
            segDirName = GenUtils.createDirectory(parDir + delimiter + "Segmentation_Visualisation", false);
            genSimpSegVis(cellData);
        }
        if (uv.isGetMorph()) {
            try {
                getMorphologyData(cellData, true, -1, null, 0.0);
            } catch (IOException e) {
                IJ.log("Could not save morphological data file.");
            }
        }
        try {
            generateCellTrajectories(cellData);
        } catch (Exception e) {
            GenUtils.logError(e, "Error: Failed to create cell trajectories file.");
        }
//        File paramFile;
//        PrintWriter paramStream;
//        try {
//            paramFile = new File(parDir + delimiter + "params.csv");
//            paramStream = new PrintWriter(new FileOutputStream(paramFile));
//        } catch (FileNotFoundException e) {
//            System.out.println("Error: Failed to create parameter file.\n");
//            System.out.println(e.toString());
//            return;
//        }
//        if (!printParamFile(paramStream)) {
//            return;
//        }
//        paramStream.close();
    }

    ArrayList<CellData> filterCells(ArrayList<CellData> originalCells) {
        ArrayList<CellData> filteredCells = new ArrayList<>();
        for (CellData cell : originalCells) {
            if (cell.isOutput()) {
                filteredCells.add(cell);
            }
        }
        return filteredCells;
    }

    @Deprecated
    void buildOutput(int index, int length, boolean preview) {
        Region[] allRegions = cellData.get(index).getCellRegions();
        ImageStack sigStack = stacks[1];
        File segPointsFile;
        PrintWriter segStream;
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
//                trajFile = new File(childDir + delimiter + "trajectory.csv");
                segPointsFile = new File(childDir + delimiter + "cell_boundary_points.csv");
//                trajStream = new PrintWriter(new FileOutputStream(trajFile));
                segStream = new PrintWriter(new FileOutputStream(segPointsFile));
            } catch (FileNotFoundException e) {
                System.out.println("Error: Failed to create parameter files.\n");
                System.out.println(e.toString());
                return;
            }
            if (!prepareOutputFiles(null, segStream, length, 3)) {
                return;
            }
            cellData.get(index).setVelMap(velMap);
            cellData.get(index).setSigMap(sigMap);
            cellData.get(index).setScaleFactors(scaleFactors);
            buildVelSigMaps(index, allRegions, null, segStream, cellData.get(index), cellData.size());
//            trajStream.close();
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
            cellData.get(index).setGreyVelMap(greyVelMap);
            cellData.get(index).setGreyCurveMap(greyCurvMap);
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

    public void getMorphologyData(ArrayList<CellData> cellData, boolean saveFile, int measurements, ImageProcessor redirectImage, double blurRadius) throws IOException {
        ResultsTable rt = Analyzer.getResultsTable();
        if (redirectImage != null) {
            new GaussianBlur().blurGaussian(redirectImage, blurRadius);
            redirectImage.subtract(redirectImage.getMin());
            redirectImage.multiply(1.0 / redirectImage.getMax());
            Analyzer.setRedirectImage(new ImagePlus("", redirectImage));
        }
        rt.reset();
        Prefs.blackBackground = false;
        if (measurements < 0) {
            measurements = Integer.MAX_VALUE;
        }
        for (int index = 0; index < cellData.size(); index++) {
            int length = cellData.get(index).getLength();
            if (length > minLength) {
                Region[] allRegions = cellData.get(index).getCellRegions();
                int start = cellData.get(index).getStartFrame();
                int end = cellData.get(index).getEndFrame();
                for (int h = start - 1; h < end; h++) {
                    Region current = allRegions[h];
                    ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS,
                            measurements, rt, 0.0, Double.POSITIVE_INFINITY);
                    ImagePlus maskImp = new ImagePlus(String.valueOf(index) + "_" + String.valueOf(h),
                            current.getMask());
                    analyzer.analyze(maskImp);
                    saveRegionMorph(current, rt);
                    rt.addValue("Cell_ID", index);
                    rt.addValue("Frame", h);
                }
            }
        }
        if (saveFile) {
            DataWriter.saveResultsTable(rt, new File(String.format("%s%s%s", parDir.getAbsolutePath(), File.separator, "morphology.csv")));
        }
    }

    void saveRegionMorph(Region region, ResultsTable rt) {
        String result = rt.getRowAsString(0);
        Scanner scan = new Scanner(result).useDelimiter("\t");
        while (scan.hasNext()) {
            try {
                region.addMorphMeasure(scan.nextDouble());
            } catch (InputMismatchException e) {
                scan.next();
                region.addMorphMeasure(Double.NaN);
            }
        }
    }

    @Deprecated
    int getMaxBoundaryLength(CellData cellData, Region[] allRegions, int index) {
        int size = allRegions.length;
        int maxBoundary = 0;
        for (int h = 0; h < size; h++) {
            Region current = allRegions[h];
            if (current != null) {
                ArrayList<float[]> centres = current.getCentres();
                float[] centre = centres.get(centres.size() - 1);
                int length = (current.getOrderedBoundary(stacks[0].getWidth(),
                        stacks[0].getHeight(), current.getMask(),
                        new short[]{(short) Math.round(centre[0]), (short) Math.round(centre[1])})).length;
                if (length > maxBoundary) {
                    maxBoundary = length;
                }
            }
        }
        return maxBoundary;
    }

    @Deprecated
    boolean prepareOutputFiles(PrintWriter trajStream, PrintWriter segStream, int size, int dim) {
        segStream.println("FRAMES " + String.valueOf(size));
        segStream.println("DIM " + String.valueOf(dim));
//        trajStream.println("Time(s), X (" + String.valueOf(GenUtils.mu) + "m), Y (" + String.valueOf(GenUtils.mu) + "m)");
        return true;
    }

//    boolean printParamFile(PrintWriter paramStream) {
//        paramStream.println(TITLE);
//        paramStream.println(Utilities.getDate("dd/MM/yyyy HH:mm:ss"));
//        paramStream.println();
//        paramStream.println(StaticVariables.AUTO_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isAutoThreshold()));
//        paramStream.println(StaticVariables.THRESH_METHOD.replaceAll("\\s", "_") + ", " + uv.getThreshMethod());
//        paramStream.println(StaticVariables.GREY_SENS.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getGreyThresh()));
//        paramStream.println(StaticVariables.SPAT_RES.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSpatialRes()));
//        paramStream.println(StaticVariables.TIME_RES.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getTimeRes()));
//        paramStream.println(StaticVariables.EROSION.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getErosion()));
//        paramStream.println(StaticVariables.SPAT_FILT_RAD.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSpatFiltRad()));
//        paramStream.println(StaticVariables.TEMP_FILT_RAD.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getTempFiltRad()));
//        paramStream.println(StaticVariables.GAUSS_RAD.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getGaussRad()));
//        paramStream.println(StaticVariables.GEN_VIS.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isGenVis()));
//        paramStream.println(StaticVariables.GET_MORPH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isGetMorph()));
//        paramStream.println(StaticVariables.ANA_PROT.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isAnalyseProtrusions()));
//        paramStream.println(StaticVariables.DETECT_BLEB.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isBlebDetect()));
//        paramStream.println(StaticVariables.MIN_CURVE_RANGE.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getCurveRange()));
//        paramStream.println(StaticVariables.MIN_CURVE_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMinCurveThresh()));
//        paramStream.println(StaticVariables.PROT_LEN_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getBlebLenThresh()));
//        paramStream.println(StaticVariables.PROT_DUR_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getBlebDurThresh()));
//        paramStream.println(StaticVariables.CUT_OFF.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getCutOffTime()));
//        paramStream.println(StaticVariables.CORTEX_DEPTH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getCortexDepth()));
//        paramStream.println(StaticVariables.USE_SIG_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isUseSigThresh()));
//        paramStream.println(StaticVariables.SIG_THRESH_FACT.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSigThreshFact()));
//        paramStream.println(StaticVariables.SIG_REC_THRESH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getSigRecoveryThresh()));
//        paramStream.println(StaticVariables.MIN_TRAJ_LENGTH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMinLength()));
//        paramStream.println(StaticVariables.FILO_MAX_SIZE.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getFiloSizeMax()));
//        paramStream.println(StaticVariables.FILO_MIN_SIZE.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getFiloSizeMin()));
//        paramStream.println(StaticVariables.GEN_SIG_DIST.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isGetFluorDist()));
//        paramStream.println(StaticVariables.MIN_MORPH_AREA.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getMorphSizeMin()));
//        paramStream.println(StaticVariables.VIS_LINE_WIDTH.replaceAll("\\s", "_") + ", " + String.valueOf(uv.getVisLineWidth()));
//        paramStream.println(StaticVariables.DISPLAY_PLOTS.replaceAll("\\s", "_") + ", " + String.valueOf(uv.isDisplayPlots()));
//        return true;
//    }
    @Deprecated
    void buildVelSigMaps(int index, Region[] allRegions, PrintWriter trajStream, PrintWriter segStream, CellData cellData, int total) {
        ImageStack cytoStack = stacks[0];
        ImageStack sigStack = stacks[1];
        MorphMap velMap = cellData.getVelMap();
        MorphMap sigMap = cellData.getSigMap();
        int width = velMap.getWidth();
        int height = velMap.getHeight();
        for (int i = cellData.getStartFrame() - 1; i < width; i++) {
            Region current = allRegions[i];
//            ArrayList<float[]> centres = current.getCentres();
//            double xc = centres.get(0)[0];
//            double yc = centres.get(0)[1];
//            trajStream.println(String.valueOf(i * 60.0 / uv.getTimeRes())
//                    + ", " + String.valueOf(xc * uv.getSpatialRes())
//                    + ", " + String.valueOf(yc * uv.getSpatialRes()));
            /*
             * Get points for one column (time-point) of map
             */
            float vmPoints[][] = current.buildMapCol(current.buildVelImage(cytoStack, i + 1,
                    uv.getTimeRes(), uv.getSpatialRes(), cellData.getGreyThresholds()), height,
                    (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes()));
            float smPoints[][] = null;
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
                x[j] = vmPoints[j][0];
                y[j] = vmPoints[j][1];
                vmz[j] = vmPoints[j][2];
                segStream.println(String.valueOf(x[j]) + ", " + String.valueOf(y[j]) + ", " + String.valueOf(i));
            }
            if (smPoints != null) {
                for (int j = 0; j < height; j++) {
                    smz[j] = smPoints[j][2];
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
    }

    @Deprecated
    private void buildCurveMap(Region[] allRegions, CellData cellData) {
        MorphMap curveMap = cellData.getCurveMap();
        int height = curveMap.getHeight();
        int start = cellData.getStartFrame();
        int end = cellData.getEndFrame();
        for (int i = start - 1; i < end; i++) {
            int index = i + 1 - start;
            Region current = allRegions[i];
            ArrayList<float[]> centres = current.getCentres();
            short xc = (short) Math.round(centres.get(0)[0]);
            short yc = (short) Math.round(centres.get(0)[1]);
            /*
             * Get points for one column (time-point) of map
             */
            short vmPoints[][] = current.getOrderedBoundary(stacks[0].getWidth(), stacks[0].getHeight(),
                    current.getMask(), new short[]{xc, yc});
            double x[] = new double[vmPoints.length];
            double y[] = new double[vmPoints.length];
            /*
             * Build arrays for (x,y) coordinates and velocity/signal values
             * from pixel data
             */
            for (int j = 0; j < vmPoints.length; j++) {
                x[j] = vmPoints[j][0];
                y[j] = vmPoints[j][1];
            }
            /*
             * Upscale all columns to maxBoundary length before adding to maps
             */
            double upX[] = DSPProcessor.upScale(x, height, false);
            double upY[] = DSPProcessor.upScale(y, height, false);
            curveMap.addColumn(upX, upY, DSPProcessor.upScale(CurveAnalyser.calcCurvature(vmPoints,
                    uv.getCurveRange()), height, false), index);
            cellData.getScaleFactors()[index] = ((double) height) / vmPoints.length;
        }
    }

    @Deprecated
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
        (new MultiThreadedVisualisationGenerator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                cellData, protMode, stacks[0], uv, velDirName, curvDirName)).run();
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
                    short[][] border = current.getOrderedBoundary(width, height, current.getMask(), current.getCentre());
                    output.setColor(Color.yellow);
                    int bsize = border.length;
                    for (int i = 0; i < bsize; i++) {
                        short[] pix = border[i];
                        output.drawDot(pix[0], pix[1]);
                    }
                    output.setColor(Color.blue);
                    ArrayList<float[]> centres = current.getCentres();
                    int cl = centres.size();
                    int xc = (int) Math.round(centres.get(cl - 1)[0]);
                    int yc = (int) Math.round(centres.get(cl - 1)[1]);
                    output.fillOval(xc - 1, yc - 1, 3, 3);
                    output.drawString(String.valueOf(n + 1), xc + 2, yc + 2);
                }
            }
            IJ.saveAs((new ImagePlus("", output)), "PNG", segDirName.getAbsolutePath() + delimiter + numFormat.format(t));
        }
        dialog.dispose();
    }

    void generateCellTrajectories(ArrayList<CellData> cellDatas) throws FileNotFoundException, IOException {
        int N = cellDatas.size();
        ImageStack cytoStack = stacks[0];
        String pdLabel = protMode ? "Building Filopodia Trajectories..." : "Building Cell Trajectories...";
        ProgressDialog dialog = new ProgressDialog(null, pdLabel, false, TITLE, false);
        dialog.setVisible(true);
        int stackSize = cytoStack.getSize();
        int origins[][] = new int[N][2];
        double distances[] = new double[N];
        Color colors[] = new Color[N];
        Random rand = new Random();
        Arrays.fill(distances, 0.0);
        ArrayList<ArrayList<Double>> trajData = new ArrayList<ArrayList<Double>>();
        String[] trajDataHeadings = new String[5];
        trajDataHeadings[0] = "Frame";
        trajDataHeadings[1] = "Time (s)";
        trajDataHeadings[2] = "Cell ID";
        trajDataHeadings[3] = String.format("Cell_X (%cm)", IJ.micronSymbol);
        trajDataHeadings[4] = String.format("Cell_Y (%cm)", IJ.micronSymbol);
        for (int n = 0; n < N; n++) {
            colors[n] = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            if (cellData.get(n).getLength() > minLength) {
                Region[] allRegions = cellData.get(n).getCellRegions();
                Region current = allRegions[cellData.get(n).getStartFrame() - 1];
                ArrayList<float[]> centres = current.getCentres();
                int cl = centres.size();
                origins[n][0] = (int) Math.round(centres.get(cl - 1)[0]);
                origins[n][1] = (int) Math.round(centres.get(cl - 1)[1]);
            }
        }
        for (int n = 0; n < N; n++) {
            for (int t = 0; t < stackSize; t++) {
                dialog.updateProgress(n, N);
                int start = cellData.get(n).getStartFrame();
                int end = cellData.get(n).getEndFrame();
                int length = cellData.get(n).getLength();
                if (length > minLength) {
                    while (trajData.size() < 5) {
                        trajData.add(new ArrayList<Double>());
                    }
                    if (t + 1 >= start && t < end) {
                        Region[] allRegions = cellData.get(n).getCellRegions();
                        Region current = allRegions[t];
                        ArrayList<float[]> centres = current.getCentres();
                        int c = centres.size();
                        double x = centres.get(c - 1)[0];
                        double y = centres.get(c - 1)[1];
                        trajData.get(0).add((double) t);
                        trajData.get(1).add(t * 60.0 / uv.getTimeRes());
                        trajData.get(2).add((double) n);
                        trajData.get(3).add(x * uv.getSpatialRes());
                        trajData.get(4).add(y * uv.getSpatialRes());
                        if (t + 1 > start) {
                            Region last = allRegions[t - 1];
                            ArrayList<float[]> lastCentres = last.getCentres();
                            int lc = lastCentres.size();
                            double lx = lastCentres.get(lc - 1)[0];
                            double ly = lastCentres.get(lc - 1)[1];
                            distances[n] += Utils.calcDistance(x, y, lx, ly) * uv.getSpatialRes();
                        }
                    } else {
                    }

                }
            }
        }
        DataWriter.saveValues(trajData, new File(String.format("%s%s%s", parDir.getAbsolutePath(), File.separator, "trajectories.csv")), trajDataHeadings, null, false);
        dialog.dispose();
    }

    /*
     * Generate graphic scalebar and output to child directory
     */
    @Deprecated
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
    @Deprecated
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

    @Deprecated
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
        RegionGrower.analyzeDetections(manager, binmap, analyzer);
        ByteProcessor binmapnoedge = (ByteProcessor) analyzer.getOutputImage().getProcessor();
        ByteProcessor flippedBinMap = new ByteProcessor(binmap.getWidth(), binmap.getHeight());
        int offset = constructFlippedBinMap(binmap, binmapnoedge, flippedBinMap);
        RoiManager manager2 = new RoiManager(true);
        RegionGrower.analyzeDetections(manager2, flippedBinMap, analyzer);
        copyRoisWithOffset(manager, manager2, offset);
        cellData.setVelRois(manager.getRoisAsArray());
    }

    @Deprecated
    ImageStack findProtrusionsBasedOnMorph(CellData cellData, int reps, int start, int stop) {
        Region regions[] = cellData.getCellRegions();
        ImageStack cyto2 = new ImageStack(stacks[0].getWidth(), stacks[0].getHeight());
        for (int f = start - 1; f < stacks[0].getSize() && f <= stop - 1; f++) {
            ImageProcessor mask = new ByteProcessor(stacks[0].getWidth(), stacks[0].getHeight());
            mask.setColor(Region.MASK_BACKGROUND);
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
            double minArea = RegionGrower.getMinFilArea(uv);
            ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES + ParticleAnalyzer.SHOW_MASKS,
                    0, null, minArea, Double.POSITIVE_INFINITY);
            mask.invert();
            RegionGrower.analyzeDetections(null, mask, analyzer);
            ImageProcessor analyzerMask = analyzer.getOutputImage().getProcessor();
            analyzerMask.invertLut();
            cyto2.addSlice(analyzerMask);
        }
        return cyto2;
    }

    @Deprecated
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

    @Deprecated
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

    @Deprecated
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

//    void initDistanceMaps(ImageProcessor inputImage, ByteProcessor regionImage, ArrayList<Region> singleImageRegions, float[][][] distancemaps, ByteProcessor[] regionImages, int width, double filtRad, double thresh) {
//        GaussianBlur blurrer = new GaussianBlur();
//        /*
//         * Image texture (and grey levels) used to control region growth.
//         * Standard deviation of grey levels is used as a simple measure of
//         * texture.
//         */
//        ImageProcessor texture = inputImage.duplicate();
//        texture.findEdges();
//        blurrer.blurGaussian(texture, filtRad, filtRad, 0.01);
//        int cellNum = singleImageRegions.size();
//        ArrayList<Region> tempRegions = new ArrayList<Region>();
//        for (int n = 0; n < cellNum; n++) {
//            /*
//             * Initialise distance maps. Any non-seed pixels are set to
//             * MAX_VALUE. Seed pixels are set to zero distance. Using these seed
//             * pixels, temporary regions are added to a temporary ArrayList.
//             */
//            for (int x = 0; x < width; x++) {
//                Arrays.fill(distancemaps[n][x], Float.MAX_VALUE);
//            }
//            Region cell = singleImageRegions.get(n);
//            if (cell != null) {
//                ImageProcessor mask = cell.getMask();
//                LinkedList<short[]> borderPix = cell.getBorderPix();
//                ArrayList<float[]> centres = cell.getCentres();
//                float[] centre = centres.get(0);
//                Region cellcopy = new Region(inputImage.getWidth(), inputImage.getHeight(),
//                        new short[]{(short) Math.round(centre[0]), (short) Math.round(centre[1])});
//                /*
//                 * Copy initial pixels and border pixels to cell copy for distance
//                 * map construction. This can probably be replaced with a clone
//                 * method.
//                 */
//                Rectangle bounds = cell.getBounds();
//                for (int i = bounds.x; i < bounds.x + bounds.width; i++) {
//                    for (int j = bounds.y; j < bounds.y + bounds.height; j++) {
//                        if (mask.getPixel(i, j) == 0) {
//                            distancemaps[n][i][j] = 0.0f;
//                        }
//                    }
//                }
//                int bordersize = borderPix.size();
//                for (int s = 0; s < bordersize; s++) {
//                    short[] pix = borderPix.get(s);
//                    int sx = pix[0];
//                    int sy = pix[1];
//                    distancemaps[n][sx][sy] = 0.0f;
//                    cellcopy.addBorderPoint(pix);
//                }
//                tempRegions.add(cellcopy);
//                regionImages[n] = (ByteProcessor) regionImage.duplicate();
//            } else {
//                tempRegions.add(new Region(inputImage.getWidth(), inputImage.getHeight(), null));
//                regionImages[n] = (ByteProcessor) regionImage.duplicate();
//            }
//        }
//        boolean totChange = true, thisChange;
//        while (totChange) {
//            totChange = false;
//            for (int i = 0; i < cellNum; i++) {
//                if (singleImageRegions.get(i) != null) {
//                    ByteProcessor tempRef = (ByteProcessor) regionImages[i].duplicate(); // Temporary reference to ensure each pixel is only considered once.
//                    Region cell = tempRegions.get(i);
//                    if (cell.isActive()) {
//                        LinkedList<short[]> borderPix = cell.getBorderPix();
//                        int borderLength = borderPix.size();
//                        thisChange = false;
//                        for (int j = 0; j < borderLength; j++) {
//                            short[] thispix = borderPix.get(j);
//                            /*
//                             * thisChange is set to true if dilation occurs at any
//                             * border pixel.
//                             */
//                            thisChange = buildDistanceMaps(tempRef, inputImage, cell,
//                                    thispix, distancemaps[i], thresh, texture, i + 1, uv.getLambda()) || thisChange;
//                        }
//                        cell.setActive(thisChange);
//                        totChange = thisChange || totChange; // if all regions cease growing, while loop will exit
//                    }
//                }
//            }
//            /*
//             * Update each region to new dilated size and update regionImages to
//             * assign index to scanned pixels
//             */
//            expandRegions(tempRegions, regionImages, cellNum);
//        }
//    }
    float calcDistance(short[] point, int x, int y, ImageProcessor gradient, double lambda) {
        return (float) ((Math.pow(gradient.getPixelValue(point[0], point[1])
                - gradient.getPixelValue(x, y), 2.0) + lambda) / (1.0 + lambda));
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

//    /*
//     * Dilate region at current point according to grey levels, texture,
//     * gradient and Dijkstra distance map.
//     */
//    boolean dijkstraDilate(ByteProcessor regionImage, Region region, Pixel point, float[][][] distanceMaps, int intermediate, int index) {
//        int width = regionImage.getWidth();
//        int height = regionImage.getHeight();
//        int x = point.getX();
//        int y = point.getY();
//        int N = distanceMaps.length;
//        boolean dilate = false;
//        boolean remove = true;
//        regionImage.setValue(intermediate); // No region in regionImage should have the index value INTERMEDIATE
//        for (int i = x - 1; i <= x + 1; i++) {
//            for (int j = y - 1; j <= y + 1; j++) {
//                if (!(Utils.isEdgePixel(i, j, width, height, 0))) {
//                    if (regionImage.getPixel(i, j) == Region.FOREGROUND && distanceMaps[index - 1][i][j] < Float.MAX_VALUE) {
//                        boolean thisdilate = true;
//                        for (int k = 0; k < N; k++) {
//                            if (k != index - 1) {
//                                /*
//                                 * Dilation will occur at the current point if
//                                 * distance to the current region's seed is less
//                                 * than all others.
//                                 */
//                                thisdilate = (distanceMaps[index - 1][i][j]
//                                        < distanceMaps[k][i][j]) && thisdilate;
//                            }
//                        }
//                        /*
//                         * If dilation is to occur, update regionImage and add a
//                         * pixel to the expandedBorder of the current region.
//                         */
//                        if (thisdilate) {
//                            Pixel p = new Pixel(i, j, index);
//                            regionImage.drawPixel(i, j);
//                            dilate = true;
//                            region.addExpandedBorderPix(p);
//                        }
//                    }
//                }
//                int r = regionImage.getPixel(i, j);
//                /*
//                 * Remove (x,y) from the borderpixel set and add it to the inner
//                 * region set if all surrounding pixels are either set to
//                 * INTERMEDIATE or already assigned to index.
//                 */
//                remove = (r == intermediate || r == index) && remove;
//            }
//        }
//        if (!remove) {
//            region.addExpandedBorderPix(point);
//            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
//                region.setEdge(true);
//            }
//        } else if (Utils.isEdgePixel(x, y, width, height, 1)) {
//            region.addExpandedBorderPix(point);
//        }
//        return dilate;
//    }
//    /*
//     * Values are added to distanceMaps in the neighbourhood of the specified
//     * point. Returns false if no values added, true otherwise.
//     */
//    boolean buildDistanceMaps(ByteProcessor regionImage, ImageProcessor greys, Region region, Pixel point, float[][] distancemap, double thresh, ImageProcessor gradient, int index, double lambda) {
//        int x = point.getX();
//        int y = point.getY();
//        boolean dilate = false;
//        boolean remove = true;
//        float minDist = Float.MAX_VALUE;
//        regionImage.setValue(intermediate); // No region in regionImage should have the index value INTERMEDIATE
//        Pixel p = null;
//        for (int i = x - 1; i <= x + 1; i++) {
//            for (int j = y - 1; j <= y + 1; j++) {
//                int r = regionImage.getPixel(i, j);
//                float g = greys.getPixelValue(i, j);
//                /*
//                 * Dilation considered if grey-level threshold exceeded
//                 */
//                if (r == Region.FOREGROUND && (g > thresh)) {
//                    float dist = calcDistance(point, i, j, gradient, lambda);
//                    /*
//                     * Dilation will only occur at point minimally distant from
//                     * seed
//                     */
//                    if (dist < minDist) {
//                        minDist = dist;
//                        p = new Pixel(i, j, index);
//                        r = index;
//                    }
//                }
//                /*
//                 * Remove (x,y) from the borderpixel set and add it to the inner
//                 * region set if all surrounding pixels are either set to
//                 * INTERMEDIATE or already assigned to index.
//                 */
//                remove = (r == intermediate || r == index) && remove;
//            }
//        }
//        if (p != null) {
//            regionImage.drawPixel(p.getX(), p.getY());
//            dilate = true;
//            region.addExpandedBorderPix(p);
//            distancemap[p.getX()][p.getY()] = distancemap[x][y] + minDist;
//        }
//        if (!remove) {
//            region.addExpandedBorderPix(point);
//            if (x < 1 || y < 1 || x >= regionImage.getWidth() - 1 || y >= regionImage.getHeight() - 1) {
//                region.setEdge(true);
//            }
//        }
//        return dilate;
//    }

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

//    void expandRegions(ArrayList<Region> regions, ByteProcessor[] regionImage, int N) {
//        for (int i = 0; i < N; i++) {
//            Region cell = regions.get(i);
//            if (cell != null) {
//                LinkedList<Pixel> pixels = cell.getExpandedBorder();
//                int borderLength = pixels.size();
//                for (int j = 0; j < borderLength; j++) {
//                    Pixel current = pixels.get(j);
//                    int x = current.getX();
//                    int y = current.getY();
//                    regionImage[i].putPixelValue(x, y, i + 1);
//                }
//                cell.expandBorder();
//            }
//        }
//    }

    /*
     * Correlates data in velImage and sigImage within the Roi's specified in
     * sigrois and velrois.
     */
    @Deprecated
    void correlativePlot(CellData cellData) throws IOException, FileNotFoundException {
        cellData.setCurvatureMinima(CurveMapAnalyser.findAllCurvatureExtrema(cellData, cellData.getStartFrame(), cellData.getEndFrame(), true, uv.getMinCurveThresh(), uv.getCurveRange(), uv, trajMin));
        ImageProcessor velMapWithDetections = cellData.getGreyVelMap().duplicate(); // Regions of interest will be drawn on
        cellData.getGreyVelMap().resetRoi();
        cellData.setVelMapWithDetections(velMapWithDetections);
        File thisMeanData, blebCount;
        OutputStreamWriter thisDataStream, blebCountStream;
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
        blebCount = new File(childDir + delimiter + "BlebsVersusTime.csv");
        blebCountStream = new OutputStreamWriter(new FileOutputStream(blebCount), GenVariables.UTF8);
        blebCountStream.write("Frame,Number of Blebs\n");
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
                        thisMeanData = new File(plotDataDir + delimiter + "bleb_data_" + count + ".csv");
                        thisDataStream = new OutputStreamWriter(new FileOutputStream(thisMeanData), GenVariables.UTF8);
                        thisDataStream.write(directory.getAbsolutePath() + "_" + count + "\n");
                        for (int d = 0; d < StaticVariables.DATA_STREAM_HEADINGS.length; d++) {
                            thisDataStream.write(StaticVariables.DATA_STREAM_HEADINGS[d] + ",");
                        }
                        thisDataStream.write("\n");
                        IJ.saveAs(new ImagePlus("", BlebAnalyser.drawBlebSigMap(currentBleb,
                                uv.getSpatialRes(), uv.isUseSigThresh())),
                                "TIF", mapDir + delimiter + "detection_" + numFormat.format(count) + "_map.tif");
                        for (int z = 0; z < meanVel.size(); z++) {
                            meanVel.set(z, meanVel.get(z) / protrusionLength.get(z)); //Divide by protrusion length to get mean
                        }
                        double time0 = bounds.x * 60.0 / uv.getTimeRes();
                        for (int z = 0; z < meanVel.size(); z++) {
                            int t = z + bounds.x;
                            double time = t * 60.0 / uv.getTimeRes();
                            double currentMeanSig;
                            currentMeanSig = sumSig.get(z) / protrusionLength.get(z);
                            thisDataStream.write(String.valueOf(time - time0) + ", "
                                    + String.valueOf(meanVel.get(z)) + ", "
                                    + String.valueOf(sumSig.get(z)) + ", "
                                    + String.valueOf(currentMeanSig) + ", "
                                    + String.valueOf(protrusionLength.get(z)) + ", "
                                    + String.valueOf(protrusionLength.get(z) / protrusionLength.get(0)));
                            thisDataStream.write("\n");
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
            blebCountStream.write(b + "," + blebFrameCount[b] + "\n");
        }
        blebCountStream.close();
        Utils.saveStackAsSeries(detectionStack, detectDir + delimiter, "JPEG", numFormat);
        dialog.dispose();

        IJ.saveAs(new ImagePlus("", velMapWithDetections), "PNG", childDir + delimiter + "Velocity_Map_with_Detected_Regions.png");
    }

    @Deprecated
    void generateDetectionStack(Bleb currentBleb, int index) {
        int cortexRad = (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes());
        Rectangle bounds = currentBleb.getBounds();
        int duration = currentBleb.getBlebPerimSigs().size();
        ArrayList<Polygon> polys = currentBleb.getPolys();
        ImageStack detectionStack = currentBleb.getDetectionStack();
        for (int timeIndex = bounds.x; timeIndex - bounds.x < duration && timeIndex < detectionStack.getSize(); timeIndex++) {
            ColorProcessor detectionSlice = (ColorProcessor) detectionStack.getProcessor(timeIndex + 1);
            Polygon poly = polys.get(timeIndex - bounds.x);
            ByteProcessor blebMask = BlebAnalyser.drawBlebMask(poly, cortexRad, stacks[0].getWidth(), stacks[0].getHeight(), 255, 0);
            blebMask.invert();
            blebMask.outline();
            blebMask.invert();
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
        cellData = new ArrayList<>();
        ImageProcessor cytoProc = stacks[0].getProcessor(sliceIndex).duplicate();
        int width = cytoProc.getWidth();
        int height = cytoProc.getHeight();
        (new GaussianBlur()).blurGaussian(cytoProc, uv.getGaussRad(), uv.getGaussRad(), 0.01);
        int threshold = RegionGrower.getThreshold(cytoProc, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
        int nCell = RegionGrower.initialiseROIs(null, -1, sliceIndex, cytoProc, roi, stacks[0].getWidth(), stacks[0].getHeight(), stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
        Region[][] allRegions = new Region[nCell][stacks[0].getSize()];
        ArrayList<Region> detectedRegions = RegionGrower.findCellRegions(cytoProc, threshold, cellData);
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
            regionsOutput[i].setLineWidth(uv.getVisLineWidth());
        }
        for (int r = 0; r < nCell; r++) {
            Region region = detectedRegions.get(r);
            if (region != null) {
                ArrayList<float[]> centres = region.getCentres();
                float[] c = centres.get(centres.size() - 1);
                short[] centre = new short[]{(short) Math.round(c[0]), (short) Math.round(c[1])};
                short[][] borderPix = region.getOrderedBoundary(width, height, region.getMask(), centre);
                for (int i = 0; i < channels; i++) {
                    regionsOutput[i].setColor(Color.red);
                    for (short[] b : borderPix) {
                        regionsOutput[i].drawDot(b[0], b[1]);
                    }
                }
                for (int i = 0; i < channels; i++) {
                    regionsOutput[i].setColor(Color.blue);
                    Utils.drawCross(regionsOutput[i], (int) Math.round(centre[0]),
                            (int) Math.round(centre[1]), 6);
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
                    short[][] shrunkBorder = shrunkRegion.getOrderedBoundary(width, height, shrunkMask, centre);
                    Region enlargedRegion = new Region(enlargedMask, centre);
                    short[][] enlargedBorder = enlargedRegion.getOrderedBoundary(width, height, enlargedMask, centre);
                    if (shrunkBorder != null) {
                        for (int i = 0; i < channels; i++) {
                            regionsOutput[i].setColor(Color.green);
                            for (short[] sCurrent : shrunkBorder) {
                                regionsOutput[i].drawDot(sCurrent[0], sCurrent[1]);
                            }
                        }
                    }
                    if (enlargedBorder != null) {
                        int esize = enlargedBorder.length;
                        for (int i = 0; i < channels; i++) {
                            regionsOutput[i].setColor(Color.green);
                            for (int eb = 0; eb < esize; eb++) {
                                short[] eCurrent = enlargedBorder[eb];
                                regionsOutput[i].drawDot(eCurrent[0], eCurrent[1]);
                            }
                        }
                    }
                }
                if (uv.isAnalyseProtrusions()) {
                    if (uv.isBlebDetect()) {
                        ArrayList<ArrayList<BoundaryPixel>> minPos = cellData.get(r).getCurvatureMinima();
                        for (int i = 0; i < channels; i++) {
                            if (minPos != null && minPos.get(0) != null) {
                                regionsOutput[i].setColor(Color.yellow);
                                int minpSize = minPos.get(0).size();
                                for (int j = 0; j < minpSize; j++) {
                                    BoundaryPixel currentMin = minPos.get(0).get(j);
                                    int x = (int) Math.round(currentMin.getX());
                                    int y = (int) Math.round(currentMin.getY());
                                    regionsOutput[i].drawOval(x - 4, y - 4, 9, 9);
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < channels; i++) {
                            regionsOutput[i].setColor(Color.yellow);
                        }
                        ImageStack filoStack = findProtrusionsBasedOnMorph(cellData.get(r), (int) Math.round(getMaxFilArea()), sliceIndex, sliceIndex);
                        ByteProcessor filoBin = (ByteProcessor) filoStack.getProcessor(1);
                        filoBin.outline();
                        for (int y = 0; y < filoBin.getHeight(); y++) {
                            for (int x = 0; x < filoBin.getWidth(); x++) {
                                if (filoBin.getPixel(x, y) < Region.MASK_BACKGROUND) {
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

    @Deprecated
    double getMaxFilArea() {
        return Math.sqrt(uv.getFiloSizeMax() / (Math.pow(uv.getSpatialRes(), 2.0)));
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

    public ArrayList<CellData> getCellData() {
        return cellData;
    }

    private void saveFluorData(ArrayList<ArrayList<ArrayList<Double>>> fluorData) {
        ArrayList<ArrayList<Double>> convertedData = new ArrayList();
        for (ArrayList<ArrayList<Double>> frameData : fluorData) {
            for (ArrayList<Double> lineData : frameData) {
                int size = lineData.size();
                while (convertedData.size() < size) {
                    convertedData.add(new ArrayList());
                }
                for (int j = 0; j < size; j++) {
                    convertedData.get(j).add(lineData.get(j));
                }
            }
        }
        try {
            DataWriter.saveValues(convertedData, new File(String.format("%s%s%s", parDir, File.separator, "fluorescence.csv")),
                    FluorescenceDistAnalyser.PARAM_HEADINGS, null, false);
        } catch (IOException e) {
            GenUtils.logError(e, "Failed to save fluorescence information file.");
        }
    }

}
