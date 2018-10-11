/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
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
package Output;

import Adapt.Analyse_Movie;
import Adapt.Bleb;
import Adapt.BlebAnalyser;
import Adapt.CurveMapAnalyser;
import Adapt.RegionFluorescenceQuantifier;
import Adapt.StaticVariables;
import Cell.CellData;
import Cell.MorphMap;
import Curvature.CurveAnalyser;
import DataProcessing.DataFileAverager;
import Fluorescence.FluorescenceAnalyser;
import IAClasses.CrossCorrelation;
import IAClasses.DSPProcessor;
import IAClasses.Region;
import IAClasses.Utils;
import Process.RunnableProcess;
import Segmentation.RegionGrower;
import UserVariables.UserVariables;
import UtilClasses.GenUtils;
import UtilClasses.GenVariables;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class RunnableOutputGenerator extends RunnableProcess {

    private ArrayList<CellData> cellData;
    String parDir;
    boolean protMode;
    UserVariables uv;
    File childDir;
    ImageStack sigStack, cytoStack;
    int index;
    int length;
    File directory;
    PointRoi roi;
    final String BLEB_DATA_FILES = "Bleb_Data_Files";
    private final double trajMin = 5.0;
    DecimalFormat numFormat = StaticVariables.numFormat;
    private final ArrayList<ArrayList<Double>> fluorData;

    public RunnableOutputGenerator(ArrayList<CellData> cellData, String parDir, boolean protMode, UserVariables uv, File childDir, ImageStack sigStack, ImageStack cytoStack, int index, int length, File directory, PointRoi roi, ArrayList<ArrayList<Double>> fluorData) {
        super(null);
        this.cellData = cellData;
        this.parDir = parDir;
        this.protMode = protMode;
        this.uv = uv;
        this.childDir = childDir;
        this.sigStack = sigStack;
        this.cytoStack = cytoStack;
        this.index = index;
        this.length = length;
        this.directory = directory;
        this.roi = roi;
        this.fluorData = fluorData;
    }

    @Override
    public void run() {
        IJ.log(String.format("Building outputs for cell %d", index));
        buildOutput(index, length, false);
        if (uv.isGetFluorDist()) {
            try {
                FluorescenceAnalyser.generateFluorMapsPerCellOverTime(FluorescenceAnalyser.getFluorDists(StaticVariables.FLUOR_MAP_HEIGHT, sigStack, ImageProcessor.MAX, Integer.MAX_VALUE, 2, cellData.get(index).getCellRegions(), cellData.get(index).getStartFrame(), cellData.get(index).getEndFrame()), childDir);
                quantifyCellFluorescence();
            } catch (Exception e) {
                GenUtils.logError(e, "Error during fluorescence distribution quantification.");
            }
        }
        if (!protMode && uv.isAnalyseProtrusions()) {
            calcSigThresh(cellData.get(index));
            if (uv.isBlebDetect()) {
                findProtrusionsBasedOnVel(cellData.get(index));
                try {
                    correlativePlot(cellData.get(index));
                } catch (IOException e) {
                    IJ.log(e.toString());
                }
                String normHeadings[] = new String[]{StaticVariables.TOTAL_SIGNAL, StaticVariables.MEAN_SIGNAL};
                (new DataFileAverager(StaticVariables.DATA_STREAM_HEADINGS,
                        normHeadings, uv.isDisplayPlots(), StaticVariables.VELOCITY,
                        StaticVariables.TIME, GenVariables.UTF8)).run(childDir + File.separator + BLEB_DATA_FILES);
            } else {
                ImageStack protStacks[] = new ImageStack[2];
                protStacks[0] = findProtrusionsBasedOnMorph(cellData.get(index), (int) Math.round(getMaxFilArea()), 1, sigStack.size());
                protStacks[1] = sigStack;
                UserVariables protUV = (UserVariables) uv.clone();
                protUV.setAnalyseProtrusions(false);
                protUV.setErosion(2);
                Analyse_Movie protAM = new Analyse_Movie(protStacks,
                        true, false, protUV,
                        new File(GenUtils.openResultsDirectory(childDir + File.separator + "Protrusions")), roi);
                protAM.analyse(null);
            }
        }
    }

    void buildOutput(int index, int length, boolean preview) {
        Region[] allRegions = cellData.get(index).getCellRegions();
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
            cellData.get(index).setVelMap(velMap);
            cellData.get(index).setSigMap(sigMap);
            cellData.get(index).setScaleFactors(scaleFactors);
            buildVelSigMaps(index, allRegions, cellData.get(index), cellData.size());
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
//            ColorProcessor colorVelMap = new ColorProcessor(smoothVelocities.length, upLength);
//            DataStatistics velstats = new DataStatistics(0.05, smoothVelocities, smoothVelocities.length * smoothVelocities[0].length);
//            double maxvel = velstats.getUpper99(); // Max and min velocity values (for colourmap) based on upper.lower 99th percentile boundaries
//            double minvel = velstats.getLower99();
//            generateScaleBar(uv.getMaxVel(), uv.getMinVel());
            cellData.get(index).setGreyVelMap(greyVelMap);
            cellData.get(index).setGreyCurveMap(greyCurvMap);
//            cellData.get(index).setMaxVel(uv.getMaxVel());
//            cellData.get(index).setMinVel(uv.getMinVel());
            cellData.get(index).setGreySigMap(greySigMap);
//            cellData.get(index).setColorVelMap(colorVelMap);
            cellData.get(index).setSmoothVelocities(smoothVelocities);
            generateMaps(smoothVelocities, cellData.get(index), index, cellData.size());
            IJ.saveAs(new ImagePlus("", greyVelMap), "TIF", childDir + File.separator + "VelocityMap.tif");
            IJ.saveAs(new ImagePlus("", greyCurvMap), "TIF", childDir + File.separator + "CurvatureMap.tif");
//            IJ.saveAs(new ImagePlus("", colorVelMap), "PNG", childDir + File.separator + "ColorVelocityMap.png");
            IJ.saveAs(CrossCorrelation.periodicity2D(greyVelMap, greyVelMap, 100), "TIF",
                    childDir + File.separator + "VelMap_AutoCorrelation.tif");
            if (sigStack != null) {
                IJ.saveAs(new ImagePlus("", greySigMap), "TIF", childDir + File.separator
                        + "SignalMap.tif");
                IJ.saveAs(CrossCorrelation.periodicity2D(greySigMap, greyVelMap, 100), "TIF",
                        childDir + File.separator + "VelMap_SigMap_CrossCorrelation.tif");
                ImageProcessor rateOfSigChange = sigMap.calcRateOfChange(greySigMap);
                IJ.saveAs(new ImagePlus("", rateOfSigChange), "TIF", childDir + File.separator
                        + "ChangeInSignalMap.tif");
                IJ.saveAs(CrossCorrelation.periodicity2D(rateOfSigChange, greyVelMap, 100), "TIF",
                        childDir + File.separator + "VelMap_ChangeInSigMap_CrossCorrelation.tif");
            }
        }
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

    void correlativePlot(CellData cellData) throws IOException, FileNotFoundException {
        cellData.setCurvatureMinima(CurveMapAnalyser.findAllCurvatureExtrema(cellData, cellData.getStartFrame(), cellData.getEndFrame(), true, uv.getMinCurveThresh(), uv.getCurveRange(), uv, trajMin));
        ImageProcessor velMapWithDetections = cellData.getGreyVelMap().duplicate(); // Regions of interest will be drawn on
        cellData.getGreyVelMap().resetRoi();
        cellData.setVelMapWithDetections(velMapWithDetections);
        File thisMeanData, blebCount;
        OutputStreamWriter thisDataStream, blebCountStream;
        File plotDataDir = GenUtils.createDirectory(childDir + File.separator + BLEB_DATA_FILES, false);
        File detectDir = GenUtils.createDirectory(childDir + File.separator + "Detection_Visualisation", false);
        File mapDir = GenUtils.createDirectory(childDir + File.separator + "Bleb_Signal_Maps", false);
        ImageStack detectionStack = new ImageStack(cytoStack.getWidth(),
                cytoStack.getHeight());
        for (int s = 0; s < cytoStack.getSize(); s++) {
            ColorProcessor detectionSlice = new ColorProcessor(detectionStack.getWidth(), detectionStack.getHeight());
            detectionSlice.setChannel(1, (ByteProcessor) ((new TypeConverter(cytoStack.getProcessor(s + 1), true)).convertToByte()));
            if (sigStack != null) {
                detectionSlice.setChannel(2, (ByteProcessor) ((new TypeConverter(sigStack.getProcessor(s + 1), true)).convertToByte()));
            }
            detectionStack.addSlice(detectionSlice);
        }
        /*
         * Cycle through all sigrois and calculate, as functions of time, mean
         * velocity, mean signal strength for all sigrois (all protrusions).
         */
        blebCount = new File(childDir + File.separator + "BlebsVersusTime.csv");
        blebCountStream = new OutputStreamWriter(new FileOutputStream(blebCount), GenVariables.UTF8);
        blebCountStream.write("Frame,Number of Blebs\n");
        int blebFrameCount[] = new int[cytoStack.getSize()];
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
                    if (sigStack != null && BlebAnalyser.extractAreaSignalData(currentBleb, cellData,
                            count, new ImageStack[]{cytoStack, sigStack}, uv)) {
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
                        thisMeanData = new File(plotDataDir + File.separator + "bleb_data_" + count + ".csv");
                        thisDataStream = new OutputStreamWriter(new FileOutputStream(thisMeanData), GenVariables.UTF8);
                        thisDataStream.write(directory.getAbsolutePath() + "_" + count + "\n");
                        for (int d = 0; d < StaticVariables.DATA_STREAM_HEADINGS.length; d++) {
                            thisDataStream.write(StaticVariables.DATA_STREAM_HEADINGS[d] + ",");
                        }
                        thisDataStream.write("\n");
                        IJ.saveAs(new ImagePlus("", BlebAnalyser.drawBlebSigMap(currentBleb,
                                uv.getSpatialRes(), uv.isUseSigThresh())),
                                "TIF", mapDir + File.separator + "detection_" + numFormat.format(count) + "_map.tif");
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
        Utils.saveStackAsSeries(detectionStack, detectDir + File.separator, "JPEG", numFormat);

        IJ.saveAs(new ImagePlus("", velMapWithDetections), "PNG", childDir + File.separator + "Velocity_Map_with_Detected_Regions.png");
    }

    double getMaxFilArea() {
        return Math.sqrt(uv.getFiloSizeMax() / (Math.pow(uv.getSpatialRes(), 2.0)));
    }

    ImageStack findProtrusionsBasedOnMorph(CellData cellData, int reps, int start, int stop) {
        Region regions[] = cellData.getCellRegions();
        ImageStack cyto2 = new ImageStack(cytoStack.getWidth(), cytoStack.getHeight());
        for (int f = start - 1; f < cytoStack.getSize() && f <= stop - 1; f++) {
            ImageProcessor mask = new ByteProcessor(cytoStack.getWidth(), cytoStack.getHeight());
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

    int getMaxBoundaryLength(CellData cellData, Region[] allRegions, int index) {
        int size = allRegions.length;
        int maxBoundary = 0;
        for (int h = 0; h < size; h++) {
            Region current = allRegions[h];
            if (current != null) {
                ArrayList<float[]> centres = current.getCentres();
                float[] centre = centres.get(centres.size() - 1);
                int length = (current.getOrderedBoundary(cytoStack.getWidth(),
                        cytoStack.getHeight(), current.getMask(),
                        new short[]{(short) Math.round(centre[0]), (short) Math.round(centre[1])})).length;
                if (length > maxBoundary) {
                    maxBoundary = length;
                }
            }
        }
        return maxBoundary;
    }

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
            short vmPoints[][] = current.getOrderedBoundary(cytoStack.getWidth(), cytoStack.getHeight(),
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

    void buildVelSigMaps(int index, Region[] allRegions, CellData cellData, int total) {
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

    /*
     * Generate graphic scalebar and output to child directory
     */
//    void generateScaleBar(double max, double min) {
//        ColorProcessor scaleBar = new ColorProcessor(90, 480);
//        scaleBar.setColor(Color.white);
//        scaleBar.fill();
//        double step = (max - min) / (scaleBar.getHeight() - 1);
//        for (int j = 0; j < scaleBar.getHeight(); j++) {
//            double val = max - j * step;
//            Color thiscolor = getColor(val, max, min);
//            scaleBar.setColor(thiscolor);
//            scaleBar.drawLine(0, j, scaleBar.getWidth() / 2, j);
//        }
//        DecimalFormat decformat = new DecimalFormat("0.0");
//        scaleBar.setFont(new Font("Times", Font.BOLD, 20));
//        int x = scaleBar.getWidth() - scaleBar.getFontMetrics().charWidth('0') * 4;
//        scaleBar.setColor(Color.black);
//        scaleBar.drawString(decformat.format(max), x, scaleBar.getFontMetrics().getHeight());
//        scaleBar.drawString(decformat.format(min), x, scaleBar.getHeight());
//        IJ.saveAs(new ImagePlus("", scaleBar), "PNG", childDir + File.separator + "VelocityScaleBar.png");
//    }
    void generateMaps(double[][] smoothVelocities, CellData cellData, int index, int total) {
        boolean sigNull = (cellData.getSigMap() == null);
        int l = smoothVelocities.length;
        MorphMap curveMap = cellData.getCurveMap();
        int upLength = curveMap.getHeight();
        FloatProcessor greyVelMap = cellData.getGreyVelMap();
        FloatProcessor greyCurvMap = cellData.getGreyCurveMap();
        FloatProcessor greySigMap = null;
//        ColorProcessor colorVelMap = cellData.getColorVelMap();
        double curvatures[][] = curveMap.smoothMap(0.0, 0.0);
        double sigchanges[][] = null;
        File velStats;
        PrintWriter velStatWriter;
        try {
            velStats = new File(childDir + File.separator + "VelocityAnalysis.csv");
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
//                    colorVelMap.setColor(getColor(smoothVelocities[i][j], cellData.getMaxVel(), cellData.getMinVel()));
//                    colorVelMap.drawPixel(i, j);
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

    void generateDetectionStack(Bleb currentBleb, int index) {
        int cortexRad = (int) Math.round(uv.getCortexDepth() / uv.getSpatialRes());
        Rectangle bounds = currentBleb.getBounds();
        int duration = currentBleb.getBlebPerimSigs().size();
        ArrayList<Polygon> polys = currentBleb.getPolys();
        ImageStack detectionStack = currentBleb.getDetectionStack();
        for (int timeIndex = bounds.x; timeIndex - bounds.x < duration && timeIndex < detectionStack.getSize(); timeIndex++) {
            ColorProcessor detectionSlice = (ColorProcessor) detectionStack.getProcessor(timeIndex + 1);
            Polygon poly = polys.get(timeIndex - bounds.x);
            ByteProcessor blebMask = BlebAnalyser.drawBlebMask(poly, cortexRad, cytoStack.getWidth(), cytoStack.getHeight(), 255, 0);
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

    private void quantifyCellFluorescence() throws IOException {
        File fluorFile = new File(parDir + File.separator + "fluorescence.csv");
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream(fluorFile), GenVariables.ISO), CSVFormat.EXCEL);
        RegionFluorescenceQuantifier rfq = new RegionFluorescenceQuantifier(cellData.get(index).getCellRegions(), sigStack, fluorData, index);
        rfq.doQuantification();
        printer.close();
    }
}
