/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import IAClasses.Pixel;
import IAClasses.Region;
import ij.ImageStack;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class Analyse_MovieTest {
    
    public Analyse_MovieTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of initialise method, of class Analyse_Movie.
     */
    @Test
    public void testInitialise() {
        System.out.println("initialise");
        Analyse_Movie instance = new Analyse_Movie();
        instance.initialise();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of run method, of class Analyse_Movie.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        String arg = "";
        Analyse_Movie instance = new Analyse_Movie();
        instance.run(arg);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of analyse method, of class Analyse_Movie.
     */
    @Test
    public void testAnalyse() {
        System.out.println("analyse");
        String imageName = "";
        Analyse_Movie instance = new Analyse_Movie();
        instance.analyse(imageName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of initialiseROIs method, of class Analyse_Movie.
     */
    @Test
    public void testInitialiseROIs() {
        System.out.println("initialiseROIs");
        int slice = 0;
        ByteProcessor masks = null;
        int threshold = 0;
        int start = 0;
        ImageProcessor input = null;
        Analyse_Movie instance = new Analyse_Movie();
        int expResult = 0;
        int result = instance.initialiseROIs(slice, masks, threshold, start, input);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of buildOutput method, of class Analyse_Movie.
     */
    @Test
    public void testBuildOutput() {
        System.out.println("buildOutput");
        int index = 0;
        int length = 0;
        boolean preview = false;
        Analyse_Movie instance = new Analyse_Movie();
        instance.buildOutput(index, length, preview);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMorphologyData method, of class Analyse_Movie.
     */
    @Test
    public void testGetMorphologyData() {
        System.out.println("getMorphologyData");
        ArrayList<CellData> cellData = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.getMorphologyData(cellData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMaxBoundaryLength method, of class Analyse_Movie.
     */
    @Test
    public void testGetMaxBoundaryLength() {
        System.out.println("getMaxBoundaryLength");
        CellData cellData = null;
        Region[] allRegions = null;
        int index = 0;
        Analyse_Movie instance = new Analyse_Movie();
        int expResult = 0;
        int result = instance.getMaxBoundaryLength(cellData, allRegions, index);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of prepareOutputFiles method, of class Analyse_Movie.
     */
    @Test
    public void testPrepareOutputFiles() {
        System.out.println("prepareOutputFiles");
        PrintWriter trajStream = null;
        PrintWriter segStream = null;
        int size = 0;
        int dim = 0;
        Analyse_Movie instance = new Analyse_Movie();
        boolean expResult = false;
        boolean result = instance.prepareOutputFiles(trajStream, segStream, size, dim);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of printParamFile method, of class Analyse_Movie.
     */
    @Test
    public void testPrintParamFile() {
        System.out.println("printParamFile");
        PrintWriter paramStream = null;
        Analyse_Movie instance = new Analyse_Movie();
        boolean expResult = false;
        boolean result = instance.printParamFile(paramStream);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of buildVelSigMaps method, of class Analyse_Movie.
     */
    @Test
    public void testBuildVelSigMaps() {
        System.out.println("buildVelSigMaps");
        int index = 0;
        Region[] allRegions = null;
        PrintWriter trajStream = null;
        PrintWriter segStream = null;
        CellData cellData = null;
        int total = 0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.buildVelSigMaps(index, allRegions, trajStream, segStream, cellData, total);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of generateMaps method, of class Analyse_Movie.
     */
    @Test
    public void testGenerateMaps() {
        System.out.println("generateMaps");
        double[][] smoothVelocities = null;
        CellData cellData = null;
        int index = 0;
        int total = 0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.generateMaps(smoothVelocities, cellData, index, total);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of genCurveVelVis method, of class Analyse_Movie.
     */
    @Test
    public void testGenCurveVelVis() {
        System.out.println("genCurveVelVis");
        ArrayList<CellData> cellDatas = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.genCurveVelVis(cellDatas);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of genSimpSegVis method, of class Analyse_Movie.
     */
    @Test
    public void testGenSimpSegVis() {
        System.out.println("genSimpSegVis");
        ArrayList<CellData> cellDatas = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.genSimpSegVis(cellDatas);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of generateCellTrajectories method, of class Analyse_Movie.
     */
    @Test
    public void testGenerateCellTrajectories() {
        System.out.println("generateCellTrajectories");
        ArrayList<CellData> cellDatas = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.generateCellTrajectories(cellDatas);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of generateScaleBar method, of class Analyse_Movie.
     */
    @Test
    public void testGenerateScaleBar() {
        System.out.println("generateScaleBar");
        double max = 0.0;
        double min = 0.0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.generateScaleBar(max, min);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getColor method, of class Analyse_Movie.
     */
    @Test
    public void testGetColor() {
        System.out.println("getColor");
        double val = 0.0;
        double promax = 0.0;
        double retmax = 0.0;
        Analyse_Movie instance = new Analyse_Movie();
        Color expResult = null;
        Color result = instance.getColor(val, promax, retmax);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findProtrusionsBasedOnVel method, of class Analyse_Movie.
     */
    @Test
    public void testFindProtrusionsBasedOnVel() {
        System.out.println("findProtrusionsBasedOnVel");
        CellData cellData = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.findProtrusionsBasedOnVel(cellData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of findProtrusionsBasedOnMorph method, of class Analyse_Movie.
     */
    @Test
    public void testFindProtrusionsBasedOnMorph() {
        System.out.println("findProtrusionsBasedOnMorph");
        CellData cellData = null;
        int reps = 0;
        Analyse_Movie instance = new Analyse_Movie();
        ImageStack expResult = null;
        ImageStack result = instance.findProtrusionsBasedOnMorph(cellData, reps);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of calcSigThresh method, of class Analyse_Movie.
     */
    @Test
    public void testCalcSigThresh() {
        System.out.println("calcSigThresh");
        CellData cellData = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.calcSigThresh(cellData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of analyzeDetections method, of class Analyse_Movie.
     */
    @Test
    public void testAnalyzeDetections() {
        System.out.println("analyzeDetections");
        RoiManager manager = null;
        ImageProcessor binmap = null;
        ParticleAnalyzer analyzer = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.analyzeDetections(manager, binmap, analyzer);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of constructFlippedBinMap method, of class Analyse_Movie.
     */
    @Test
    public void testConstructFlippedBinMap() {
        System.out.println("constructFlippedBinMap");
        ByteProcessor input1 = null;
        ByteProcessor input2 = null;
        ByteProcessor output = null;
        Analyse_Movie instance = new Analyse_Movie();
        int expResult = 0;
        int result = instance.constructFlippedBinMap(input1, input2, output);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of copyRoisWithOffset method, of class Analyse_Movie.
     */
    @Test
    public void testCopyRoisWithOffset() {
        System.out.println("copyRoisWithOffset");
        RoiManager manager = null;
        RoiManager manager2 = null;
        int offset = 0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.copyRoisWithOffset(manager, manager2, offset);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of initDistanceMaps method, of class Analyse_Movie.
     */
    @Test
    public void testInitDistanceMaps() {
        System.out.println("initDistanceMaps");
        ImageProcessor inputImage = null;
        ByteProcessor regionImage = null;
        ArrayList<Region> singleImageRegions = null;
        float[][][] distancemaps = null;
        ByteProcessor[] regionImages = null;
        int width = 0;
        double filtRad = 0.0;
        double thresh = 0.0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.initDistanceMaps(inputImage, regionImage, singleImageRegions, distancemaps, regionImages, width, filtRad, thresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of calcDistance method, of class Analyse_Movie.
     */
    @Test
    public void testCalcDistance() {
        System.out.println("calcDistance");
        Pixel point = null;
        int x = 0;
        int y = 0;
        ImageProcessor gradient = null;
        Analyse_Movie instance = new Analyse_Movie();
        float expResult = 0.0F;
        float result = instance.calcDistance(point, x, y, gradient);
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sdImage method, of class Analyse_Movie.
     */
    @Test
    public void testSdImage() {
        System.out.println("sdImage");
        ImageProcessor image = null;
        int window = 0;
        Analyse_Movie instance = new Analyse_Movie();
        ImageProcessor expResult = null;
        ImageProcessor result = instance.sdImage(image, window);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dijkstraDilate method, of class Analyse_Movie.
     */
    @Test
    public void testDijkstraDilate() {
        System.out.println("dijkstraDilate");
        ByteProcessor regionImage = null;
        Region region = null;
        Pixel point = null;
        float[][][] distanceMaps = null;
        int intermediate = 0;
        int index = 0;
        Analyse_Movie instance = new Analyse_Movie();
        boolean expResult = false;
        boolean result = instance.dijkstraDilate(regionImage, region, point, distanceMaps, intermediate, index);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of buildDistanceMaps method, of class Analyse_Movie.
     */
    @Test
    public void testBuildDistanceMaps() {
        System.out.println("buildDistanceMaps");
        ByteProcessor regionImage = null;
        ImageProcessor greys = null;
        Region region = null;
        Pixel point = null;
        float[][] distancemap = null;
        double thresh = 0.0;
        ImageProcessor gradient = null;
        int index = 0;
        Analyse_Movie instance = new Analyse_Movie();
        boolean expResult = false;
        boolean result = instance.buildDistanceMaps(regionImage, greys, region, point, distancemap, thresh, gradient, index);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of sigmoidFilter method, of class Analyse_Movie.
     */
    @Test
    public void testSigmoidFilter() {
        System.out.println("sigmoidFilter");
        ImageProcessor image = null;
        double t = 0.0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.sigmoidFilter(image, t);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of expandRegions method, of class Analyse_Movie.
     */
    @Test
    public void testExpandRegions_4args() {
        System.out.println("expandRegions");
        ArrayList<Region> regions = null;
        ByteProcessor regionImage = null;
        int N = 0;
        int terminal = 0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.expandRegions(regions, regionImage, N, terminal);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of expandRegions method, of class Analyse_Movie.
     */
    @Test
    public void testExpandRegions_3args() {
        System.out.println("expandRegions");
        ArrayList<Region> regions = null;
        ByteProcessor[] regionImage = null;
        int N = 0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.expandRegions(regions, regionImage, N);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of correlativePlot method, of class Analyse_Movie.
     */
    @Test
    public void testCorrelativePlot() {
        System.out.println("correlativePlot");
        CellData cellData = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.correlativePlot(cellData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of generateDetectionStack method, of class Analyse_Movie.
     */
    @Test
    public void testGenerateDetectionStack() {
        System.out.println("generateDetectionStack");
        Bleb currentBleb = null;
        int index = 0;
        Analyse_Movie instance = new Analyse_Movie();
        instance.generateDetectionStack(currentBleb, index);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

//    /**
//     * Test of generatePreview method, of class Analyse_Movie.
//     */
//    @Test
//    public void testGeneratePreview() {
//        System.out.println("generatePreview");
//        int sliceIndex = 0;
//        Analyse_Movie instance = new Analyse_Movie();
//        ImageProcessor[] expResult = null;
//        ImageProcessor[] result = instance.generatePreview(sliceIndex);
//        assertArrayEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of getSeedPoints method, of class Analyse_Movie.
     */
    @Test
    public void testGetSeedPoints() {
        System.out.println("getSeedPoints");
        ByteProcessor binary = null;
        ArrayList<Pixel> pixels = null;
        Analyse_Movie instance = new Analyse_Movie();
        instance.getSeedPoints(binary, pixels);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getThreshold method, of class Analyse_Movie.
     */
    @Test
    public void testGetThreshold() {
        System.out.println("getThreshold");
        ImageProcessor image = null;
        boolean auto = false;
        double thresh = 0.0;
        String method = "";
        Analyse_Movie instance = new Analyse_Movie();
        int expResult = 0;
        int result = instance.getThreshold(image, auto, thresh, method);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMinArea method, of class Analyse_Movie.
     */
    @Test
    public void testGetMinArea() {
        System.out.println("getMinArea");
        Analyse_Movie instance = new Analyse_Movie();
        double expResult = 0.0;
        double result = instance.getMinArea();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFluorDists method, of class Analyse_Movie.
     */
    @Test
    public void testGetFluorDists() {
        System.out.println("getFluorDists");
        CellData cellData = null;
        int height = 0;
        Analyse_Movie instance = new Analyse_Movie();
        FloatProcessor[] expResult = null;
        FloatProcessor[] result = instance.getFluorDists(cellData, height);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
