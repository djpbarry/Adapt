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

import ij.*;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
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
import ij.process.*;
import net.calm.adapt.output.MultiThreadedOutputGenerator;
import net.calm.adapt.visualisation.MultiThreadedVisualisationGenerator;
import net.calm.adapt.ui.GUI;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.Cell.MorphMap;
import net.calm.iaclasslibrary.Curvature.CurveAnalyser;
import net.calm.iaclasslibrary.DateAndTime.Time;
import net.calm.iaclasslibrary.IAClasses.*;
import net.calm.adapt.output.CsvWriter;
import net.calm.adapt.output.ParameterWriter;
import net.calm.iaclasslibrary.Segmentation.RegionGrower;
import net.calm.iaclasslibrary.TimeAndDate.TimeAndDate;
import net.calm.iaclasslibrary.Trajectory.TrajectoryAnalysis;
import net.calm.iaclasslibrary.UserVariables.UserVariables;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.iaclasslibrary.UtilClasses.GenVariables;
import net.calm.iaclasslibrary.UtilClasses.Utilities;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Analyse_Movie is designed to quantify cell membrane dynamics and correlate
 * membrane velocity with signal dynamics. It takes as input two movies. One
 * represents the cell cytosol, which should depict a uniform cell against a
 * relatively uniform background. The second movie contains a signal of interest
 * that the user wishes to correlate with membrane dynamics.
 */
public class Analyse_Movie extends NotificationThread implements PlugIn {

    protected static File directory = IJ.getInstance() == null
            ? new File("D:\\debugging\\ADAPT Test Data\\ADAPT Test Data\\Adapt_Test_Data")
            : new File(IJ.getDirectory("current")); // root directory
    protected File childDir, // root output directory
            parDir, // output directory for each cell
            velDir, curveDir, segDir, visDir, cellsDir, popDir;
    protected String TITLE = StaticVariables.TITLE;
    final String BLEB_DATA_FILES = "Bleb_Data_Files";
    protected final String delimiter = GenUtils.getDelimiter(); // delimiter in directory strings
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
    private Overlay previewOverlay;
    private boolean selectiveOutput = false;
    private Properties props;
    private LocalDateTime startTime;
    private final String TRAJ_FILE_NAME = "trajectories.csv";
    private static final String[][] REQUIRED_DEPENDENCIES = {
            {"inra.ijpb.binary.conncomp.FloodFillComponentsLabeling", "MorphoLibJ (IJPB-plugins update site)"},
            {"mcib3d.image3d.ImageInt", "3D ImageJ Suite (mcib3d-core)"}
    };

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
        startTime = LocalDateTime.now();
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
        IJ.log(TITLE);
        IJ.log(TimeAndDate.getCurrentTimeAndDate());
        if (IJ.getInstance() != null && WindowManager.getIDList() == null) {
            IJ.error("No Images Open.");
            return;
        }
        try {
            if (!batchMode) {
                directory = Utilities.getFolder(new File(IJ.getDirectory("current")), "Specify directory for output files...", true); // Specify directory for output
            }
        } catch (Exception e) {
            IJ.log("Failed to select output directory: " + e.getMessage());
        }
        if (directory == null) {
            return;
        }
        IJ.log(String.format("Using %d parallel processes.\n", Runtime.getRuntime().availableProcessors()));
        analyse(arg);
    }

    public boolean analyse(String imageName) {
        if (!checkDependencies()) {
            return false;
        }
        ImagePlus cytoImp = new ImagePlus();
        if (IJ.getInstance() == null || batchMode || protMode) {
            // stacks[0]/stacks[1] are already populated (batch / protrusion / preview paths)
        } else {
            cytoImp = WindowManager.getCurrentImage();
            if (cytoImp == null) {
                IJ.error("No active image.");
                return false;
            }
            if (!cytoImp.isHyperStack() || cytoImp.getNSlices() != 1 || cytoImp.getNFrames() < 2) {
                IJ.error("The active image must be a hyperstack with a single z-slice and multiple frames (T > 1).");
                return false;
            }
            roi = (PointRoi) cytoImp.getRoi(); // Points specified by the user indicate cells of interest
        }
        cytoImp.setTitle(cytoImp.getTitle().replace(" ", "_"));
        if (roi != null) {
            selectiveOutput = true;
        }
        if (!createOutputDirectories(cytoImp, imageName)) {
            return false;
        }
        if (!(batchMode || protMode)) {
            GUI gui = new GUI(null, false, TITLE, cytoImp, roi);
            gui.setOnRun(() -> {
                stacks = gui.getSelectedStacks();
                uv = gui.getUv();
                props = gui.getProperties();
                new Thread(this::finishAnalysis).start();
            });
            gui.setVisible(true);
            return true;
        }
        return runPipeline();
    }

    /**
     * Extracts a single channel from a multi-channel hyperstack as a 2D+time
     * stack (Z is assumed to be 1). {@code channel} is 1-based.
     */
    public static ImageStack extractChannel(ImagePlus hyperstack, int channel) {
        int nChannels = hyperstack.getNChannels();
        int nFrames = hyperstack.getNFrames();
        ImageStack source = hyperstack.getImageStack();
        ImageStack out = new ImageStack(hyperstack.getWidth(), hyperstack.getHeight());
        for (int t = 0; t < nFrames; t++) {
            out.addSlice(source.getProcessor(t * nChannels + channel));
        }
        return out;
    }

    private boolean checkDependencies() {
        for (String[] dependency : REQUIRED_DEPENDENCIES) {
            try {
                Class.forName(dependency[0]);
            } catch (ClassNotFoundException e) {
                IJ.error("Required dependency is missing: " + dependency[1]
                        + ".\nInstall it from the Fiji update site before running ADAPT.");
                return false;
            }
        }
        return true;
    }

    private boolean runPipeline() {
        /*
         * Convert cyto channel to 8-bit for faster segmentation
         */
        stacks[0] = GenUtils.convertStack(stacks[0], 8);
        if (stacks[0].getProcessor(1) instanceof ColorProcessor
                || (stacks[1] != null && stacks[1].getProcessor(1) instanceof ColorProcessor)) {
            IJ.showMessage("Warning: greyscale images should be used for optimal results.");
        }
        if (stacks[0].getSize() < 2) {
            IJ.error("Timelapse dataset required - aborting.");
            return false;
        }
        ImageStack cytoStack = stacks[0];
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        int cytoSize = cytoStack.getSize();
        if (!segmentCells(cytoStack, width, height, cytoSize)) {
            return false;
        }
        if (cellData.size() < 1) return false;
        if (selectiveOutput) {
            ArrayList<CellData> filteredCells = filterCells(cellData);
            cellData = filteredCells;
        }
        generateOutputs();
        if (uv.isGetMorph()) {
            try {
                getMorphologyData(cellData, true, -1, null, 0.0);
            } catch (IOException e) {
                GenUtils.logError(e, "Could not save morphological data file.");
            }
        }
        try {
            generateCellTrajectories(cellData);
        } catch (Exception e) {
            GenUtils.logError(e, "Error: Failed to create cell trajectories file.");
        }
        return true;
    }

    private void finishAnalysis() {
        if (!runPipeline()) {
            return;
        }
        TrajectoryAnalysis ta = new TrajectoryAnalysis(0.0, 0.0, uv.getTimeRes() / 60.0, 0, false, false, false, true, false, new int[]{3, 4, 0, 2});
        ta.run(String.format("%s%s%s", popDir.getAbsolutePath(), File.separator, TRAJ_FILE_NAME));
        try {
            ParameterWriter.saveParameters(props, parDir);
            ParameterWriter.saveReadme(parDir, TITLE);
        } catch (IOException e) {
            IJ.log("Failed to create output metadata files.");
        }
        IJ.showStatus(TITLE + " done.");
        IJ.log(Time.getDurationAsString(startTime));
    }

    private boolean createOutputDirectories(ImagePlus cytoImp, String imageName) {
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
            return false;
        }
        cellsDir = GenUtils.createDirectory(String.format("%s%s%s", parDir.getAbsolutePath(), File.separator, "images"), false);
        popDir = GenUtils.createDirectory(String.format("%s%s%s", parDir.getAbsolutePath(), File.separator, "tables"), false);
        return true;
    }

    private boolean segmentCells(ImageStack cytoStack, int width, int height, int cytoSize) {
        minLength = protMode ? uv.getBlebLenThresh() : uv.getMinLength();
        String pdLabel = protMode ? "Segmenting filopodia..." : "Segmenting cells...";
        cellData = new ArrayList<>();
        ImageProcessor cytoImage = cytoStack.getProcessor(1).duplicate();
        (new GaussianBlur()).blurGaussian(cytoImage, uv.getGaussRad(), uv.getGaussRad(), 0.01);
        RegionGrower.initialiseROIs(null, -1, 1, cytoImage, roi, stacks[0].getWidth(), stacks[0].getHeight(), stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
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
                filoData = new File(popDir + delimiter + "FilopodiaVersusTime.csv");
                filoStream = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filoData), GenVariables.UTF8));
                filoStream.println("Frame,Number of Filopodia");
            } catch (FileNotFoundException e) {
                IJ.log("Failed to create FilopodiaVersusTime.csv: " + e.getMessage());
                return false;
            }
        }
        IJ.log(pdLabel);
        for (int i = 0; i < cytoSize; i++) {
            IJ.showStatus(String.format("Segmenting %d%%", (int) Math.round(i * 100.0 / cytoSize)));
            cytoImage = cytoStack.getProcessor(i + 1).duplicate();
            (new GaussianBlur()).blurGaussian(cytoImage, uv.getGaussRad(), uv.getGaussRad(), 0.01);
            thresholds[i] = RegionGrower.getThreshold(cytoImage, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
            int N = cellData.size();
            if (cytoImage != null) {
                allRegions.add(RegionGrower.watershedRegions(cytoImage, thresholds[i], cellData));

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
                    current.calcCentroid(mask);
                    Rectangle bounds = current.getBounds();
                    bounds.grow(2, 2);
                    mask.setRoi(bounds);
                    int e = uv.getErosion();
                    for (int k = 0; k < e; k++) {
                        mask.erode();
                    }
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
                    currentMask.invert();
                    bb.copyBits(currentMask, 0, 0, Blitter.ADD);
                    current.setFinalMask();
                }
            }
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
        IJ.log(String.format("%d cells found.\n", cellData.size()));
        return true;
    }

    private void generateOutputs() {
        /*
         * Analyse the dynamics of each cell, represented by a series of
         * detected regions.
         */
        if ((uv.isGenVis() || uv.isGetFluorDist()) && !protMode) {
            MultiThreadedOutputGenerator outGen = new MultiThreadedOutputGenerator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), cellData,
                    cellsDir.getAbsolutePath(), protMode, uv, childDir, stacks[1],
                    stacks[0], directory, roi);
            outGen.run();
            if (stacks[1] != null && uv.isGetFluorDist()) {
                saveFluorData(outGen.getFluorData());
            }
            velDir = cellsDir;
            curveDir = cellsDir;
            genCurveVelVis(cellData);
        } else {
            visDir = GenUtils.createDirectory(parDir + delimiter + "Visualisations", false);
            segDir = GenUtils.createDirectory(visDir + delimiter + "Segmentation_Visualisation", false);
            genSimpSegVis(cellData);
        }
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
                segPointsFile = new File(childDir + delimiter + "cell_boundary_points.csv");
                segStream = new PrintWriter(new OutputStreamWriter(new FileOutputStream(segPointsFile), GenVariables.UTF8));
            } catch (FileNotFoundException e) {
                IJ.log("Failed to create cell boundary points file: " + e.getMessage());
                return;
            }
            if (!prepareOutputFiles(null, segStream, length, 3)) {
                return;
            }
            cellData.get(index).setVelMap(velMap);
            cellData.get(index).setSigMap(sigMap);
            cellData.get(index).setScaleFactors(scaleFactors);
            buildVelSigMaps(index, allRegions, null, segStream, cellData.get(index), cellData.size());
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
        IJ.log("Generating cell morphology data...\n");
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
                    IJ.showStatus(String.format("%d%% morphological analysis done for cell %d of %d",
                            (int) Math.round((h - start + 1) * 100.0 / (end - start + 1)), (index + 1), cellData.size()));
                    Region current = allRegions[h];
                    ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_RESULTS,
                            measurements, rt, 0.0, Double.POSITIVE_INFINITY);
                    ImagePlus maskImp = new ImagePlus(String.valueOf(index) + "_" + String.valueOf(h),
                            current.getMask());
                    analyzer.analyze(maskImp);
                    saveRegionMorph(current, rt);
                    rt.addValue("cell_id", index);
                    rt.addValue("Frame", h);
                }
            }
        }
        if (saveFile) {
            CsvWriter.saveResultsTable(rt, new File(String.format("%s%s%s", popDir.getAbsolutePath(), File.separator, "morphology.csv")), false, true);
        }
        Window w = WindowManager.getWindow(rt.getTitle());
        if (w != null) {
            w.dispose();
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
        return true;
    }

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
            velStatWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(velStats), GenVariables.UTF8));
            velStatWriter.println("frame,%_protruding,%_retracting,mean_protrusion_velocity_um_min,mean_retraction_velocity_um_min");
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
            IJ.log("Failed to create VelocityAnalysis.csv: " + e.getMessage());
        }
    }

    void genCurveVelVis(ArrayList<CellData> cellDatas) {
        (new MultiThreadedVisualisationGenerator(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                cellData, protMode, stacks[0], uv, velDir, curveDir)).run();
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
                    if (border == null) continue;
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
            IJ.saveAs((new ImagePlus("", output)), "PNG", segDir.getAbsolutePath() + delimiter + numFormat.format(t));
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
        String[] trajDataHeadings = new String[]{"frame", "time_s", "cell_id", "x_um", "y_um"};
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
        if (trajData.size() > 0)
            CsvWriter.saveValues(trajData, new File(String.format("%s%s%s", popDir.getAbsolutePath(), File.separator, TRAJ_FILE_NAME)), trajDataHeadings, null, false);
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
                        thisDataStream.write(String.join(",", StaticVariables.DATA_STREAM_HEADINGS) + "\n");
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
        PointRoi seeds = roi;
        if (seeds == null) {
            seeds = detectSeedPoints(cytoProc, threshold, RegionGrower.getMinCellArea(uv));
        }
        int nCell = RegionGrower.initialiseROIs(null, -1, sliceIndex, cytoProc, seeds, stacks[0].getWidth(), stacks[0].getHeight(), stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
        Region[][] allRegions = new Region[nCell][stacks[0].getSize()];
        ArrayList<Region> detectedRegions = RegionGrower.watershedRegions(cytoProc, threshold, cellData);
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
         * Build an overlay of the segmentation preview for the original
         * ImageWindows (non-destructive).
         */
        int channels = (stacks[1] == null) ? 1 : 2;
        Overlay overlay = new Overlay();
        for (int r = 0; r < nCell; r++) {
            Region region = detectedRegions.get(r);
            if (region != null) {
                ArrayList<float[]> centres = region.getCentres();
                float[] c = centres.get(centres.size() - 1);
                short[] centre = new short[]{(short) Math.round(c[0]), (short) Math.round(c[1])};
                short[][] borderPix = region.getOrderedBoundary(width, height, region.getMask(), centre);

                overlay.add(toPolygonRoi(borderPix, Color.red, sliceIndex));

                OvalRoi centreRoi = new OvalRoi(centre[0] - 3, centre[1] - 3, 6, 6);
                centreRoi.setStrokeColor(Color.blue);
                centreRoi.setStrokeWidth(uv.getVisLineWidth());
                centreRoi.setPosition(sliceIndex);
                overlay.add(centreRoi);

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
                        overlay.add(toPolygonRoi(shrunkBorder, Color.green, sliceIndex));
                    }
                    if (enlargedBorder != null) {
                        overlay.add(toPolygonRoi(enlargedBorder, Color.green, sliceIndex));
                    }
                }
                if (uv.isAnalyseProtrusions() && uv.isBlebDetect()) {
                    ArrayList<ArrayList<BoundaryPixel>> minPos = cellData.get(r).getCurvatureMinima();
                    if (minPos != null && minPos.get(0) != null) {
                        for (BoundaryPixel currentMin : minPos.get(0)) {
                            int x = (int) Math.round(currentMin.getX());
                            int y = (int) Math.round(currentMin.getY());
                            OvalRoi blebRoi = new OvalRoi(x - 4, y - 4, 9, 9);
                            blebRoi.setStrokeColor(Color.yellow);
                            blebRoi.setStrokeWidth(uv.getVisLineWidth());
                            blebRoi.setPosition(sliceIndex);
                            overlay.add(blebRoi);
                        }
                    }
                }
            }
        }
        previewOverlay = overlay;
    }

    private Roi toPolygonRoi(short[][] points, Color color, int sliceIndex) {
        int n = points.length;
        int[] x = new int[n];
        int[] y = new int[n];
        for (int i = 0; i < n; i++) {
            x[i] = points[i][0];
            y[i] = points[i][1];
        }
        PolygonRoi roi = new PolygonRoi(x, y, n, Roi.POLYGON);
        roi.setStrokeColor(color);
        roi.setStrokeWidth(uv.getVisLineWidth());
        roi.setPosition(sliceIndex);
        return roi;
    }

    /**
     * Auto-detects cell seed points without the side effect of
     * {@code RegionGrower.analyzeDetections()} (which calls
     * {@code RegionGrower.hideWindows()} and hides the most recently opened
     * image window).
     */
    private PointRoi detectSeedPoints(ImageProcessor ip, double threshold, double minArea) {
        ByteProcessor binary = (ByteProcessor) ip.convertToByteProcessor(true);
        binary.threshold((int) Math.round(threshold));
        binary.invert();
        if (binary.isInvertedLut()) {
            binary.invertLut();
        }
        ResultsTable rt = new ResultsTable();
        Prefs.blackBackground = false;
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES,
                Measurements.CENTROID, rt, minArea, Double.POSITIVE_INFINITY);
        ParticleAnalyzer.setRoiManager(null);
        analyzer.analyze(new ImagePlus("", binary));
        PointRoi seeds = new PointRoi();
        int count = rt.getCounter();
        if (count > 0) {
            float[] x = rt.getColumn(rt.getColumnIndex("X"));
            float[] y = rt.getColumn(rt.getColumnIndex("Y"));
            for (int i = 0; i < count; i++) {
                seeds.addPoint(x[i], y[i]);
            }
        }
        return seeds;
    }

    @Deprecated
    double getMaxFilArea() {
        return Math.sqrt(uv.getFiloSizeMax() / (Math.pow(uv.getSpatialRes(), 2.0)));
    }

    public Overlay getPreviewOverlay() {
        return previewOverlay;
    }

    public int getPreviewSlice() {
        return previewSlice;
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
        if (fluorData.size() < 1) {
            IJ.log("No fluorescence distribution data found.");
            return;
        }
        IJ.showStatus("Saving fluorescence data");
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
            if (convertedData.size() > 0)
                CsvWriter.saveValues(convertedData, new File(String.format("%s%s%s", popDir, File.separator, "fluorescence.csv")),
                        FluorescenceDistAnalyser.PARAM_HEADINGS, null, false);
        } catch (IOException e) {
            GenUtils.logError(e, "Failed to save fluorescence information file.");
        }
    }

}
