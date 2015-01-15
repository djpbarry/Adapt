/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import IAClasses.Region;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class CellDataTest {
    
    public CellDataTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getCellRegions method, of class CellData.
     */
    @Test
    public void testGetCellRegions() {
        System.out.println("getCellRegions");
        CellData instance = new CellData();
        Region[] expResult = null;
        Region[] result = instance.getCellRegions();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCellRegions method, of class CellData.
     */
    @Test
    public void testSetCellRegions() {
        System.out.println("setCellRegions");
        Region[] cellRegions = null;
        CellData instance = new CellData();
        instance.setCellRegions(cellRegions);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGreyCurveMap method, of class CellData.
     */
    @Test
    public void testGetGreyCurveMap() {
        System.out.println("getGreyCurveMap");
        CellData instance = new CellData();
        FloatProcessor expResult = null;
        FloatProcessor result = instance.getGreyCurveMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGreyCurveMap method, of class CellData.
     */
    @Test
    public void testSetGreyCurveMap() {
        System.out.println("setGreyCurveMap");
        FloatProcessor greyCurveMap = null;
        CellData instance = new CellData();
        instance.setGreyCurveMap(greyCurveMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMaxVel method, of class CellData.
     */
    @Test
    public void testGetMaxVel() {
        System.out.println("getMaxVel");
        CellData instance = new CellData();
        double expResult = 0.0;
        double result = instance.getMaxVel();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMaxVel method, of class CellData.
     */
    @Test
    public void testSetMaxVel() {
        System.out.println("setMaxVel");
        double maxVel = 0.0;
        CellData instance = new CellData();
        instance.setMaxVel(maxVel);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMinVel method, of class CellData.
     */
    @Test
    public void testGetMinVel() {
        System.out.println("getMinVel");
        CellData instance = new CellData();
        double expResult = 0.0;
        double result = instance.getMinVel();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMinVel method, of class CellData.
     */
    @Test
    public void testSetMinVel() {
        System.out.println("setMinVel");
        double minVel = 0.0;
        CellData instance = new CellData();
        instance.setMinVel(minVel);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getScaleFactors method, of class CellData.
     */
    @Test
    public void testGetScaleFactors() {
        System.out.println("getScaleFactors");
        CellData instance = new CellData();
        double[] expResult = null;
        double[] result = instance.getScaleFactors();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setScaleFactors method, of class CellData.
     */
    @Test
    public void testSetScaleFactors() {
        System.out.println("setScaleFactors");
        double[] scaleFactors = null;
        CellData instance = new CellData();
        instance.setScaleFactors(scaleFactors);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCurvatureMinima method, of class CellData.
     */
    @Test
    public void testGetCurvatureMinima() {
        System.out.println("getCurvatureMinima");
        CellData instance = new CellData();
        ArrayList[] expResult = null;
        ArrayList[] result = instance.getCurvatureMinima();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCurvatureMinima method, of class CellData.
     */
    @Test
    public void testSetCurvatureMinima() {
        System.out.println("setCurvatureMinima");
        ArrayList[] curvatureMinima = null;
        CellData instance = new CellData();
        instance.setCurvatureMinima(curvatureMinima);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getVelRois method, of class CellData.
     */
    @Test
    public void testGetVelRois() {
        System.out.println("getVelRois");
        CellData instance = new CellData();
        Roi[] expResult = null;
        Roi[] result = instance.getVelRois();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setVelRois method, of class CellData.
     */
    @Test
    public void testSetVelRois() {
        System.out.println("setVelRois");
        Roi[] velRois = null;
        CellData instance = new CellData();
        instance.setVelRois(velRois);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGreySigMap method, of class CellData.
     */
    @Test
    public void testGetGreySigMap() {
        System.out.println("getGreySigMap");
        CellData instance = new CellData();
        FloatProcessor expResult = null;
        FloatProcessor result = instance.getGreySigMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGreySigMap method, of class CellData.
     */
    @Test
    public void testSetGreySigMap() {
        System.out.println("setGreySigMap");
        FloatProcessor greySigMap = null;
        CellData instance = new CellData();
        instance.setGreySigMap(greySigMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGreyVelMap method, of class CellData.
     */
    @Test
    public void testGetGreyVelMap() {
        System.out.println("getGreyVelMap");
        CellData instance = new CellData();
        FloatProcessor expResult = null;
        FloatProcessor result = instance.getGreyVelMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGreyVelMap method, of class CellData.
     */
    @Test
    public void testSetGreyVelMap() {
        System.out.println("setGreyVelMap");
        FloatProcessor greyVelMap = null;
        CellData instance = new CellData();
        instance.setGreyVelMap(greyVelMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSigMap method, of class CellData.
     */
    @Test
    public void testGetSigMap() {
        System.out.println("getSigMap");
        CellData instance = new CellData();
        MorphMap expResult = null;
        MorphMap result = instance.getSigMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSigMap method, of class CellData.
     */
    @Test
    public void testSetSigMap() {
        System.out.println("setSigMap");
        MorphMap sigMap = null;
        CellData instance = new CellData();
        instance.setSigMap(sigMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getVelMap method, of class CellData.
     */
    @Test
    public void testGetVelMap() {
        System.out.println("getVelMap");
        CellData instance = new CellData();
        MorphMap expResult = null;
        MorphMap result = instance.getVelMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setVelMap method, of class CellData.
     */
    @Test
    public void testSetVelMap() {
        System.out.println("setVelMap");
        MorphMap velMap = null;
        CellData instance = new CellData();
        instance.setVelMap(velMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCurveMap method, of class CellData.
     */
    @Test
    public void testGetCurveMap() {
        System.out.println("getCurveMap");
        CellData instance = new CellData();
        MorphMap expResult = null;
        MorphMap result = instance.getCurveMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCurveMap method, of class CellData.
     */
    @Test
    public void testSetCurveMap() {
        System.out.println("setCurveMap");
        MorphMap curveMap = null;
        CellData instance = new CellData();
        instance.setCurveMap(curveMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSigThresh method, of class CellData.
     */
    @Test
    public void testGetSigThresh() {
        System.out.println("getSigThresh");
        CellData instance = new CellData();
        double expResult = 0.0;
        double result = instance.getSigThresh();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSigThresh method, of class CellData.
     */
    @Test
    public void testSetSigThresh() {
        System.out.println("setSigThresh");
        double sigThresh = 0.0;
        CellData instance = new CellData();
        instance.setSigThresh(sigThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getVelMapWithDetections method, of class CellData.
     */
    @Test
    public void testGetVelMapWithDetections() {
        System.out.println("getVelMapWithDetections");
        CellData instance = new CellData();
        ImageProcessor expResult = null;
        ImageProcessor result = instance.getVelMapWithDetections();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setVelMapWithDetections method, of class CellData.
     */
    @Test
    public void testSetVelMapWithDetections() {
        System.out.println("setVelMapWithDetections");
        ImageProcessor velMapWithDetections = null;
        CellData instance = new CellData();
        instance.setVelMapWithDetections(velMapWithDetections);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGreyThresholds method, of class CellData.
     */
    @Test
    public void testGetGreyThresholds() {
        System.out.println("getGreyThresholds");
        CellData instance = new CellData();
        int[] expResult = null;
        int[] result = instance.getGreyThresholds();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGreyThresholds method, of class CellData.
     */
    @Test
    public void testSetGreyThresholds() {
        System.out.println("setGreyThresholds");
        int[] greyThresholds = null;
        CellData instance = new CellData();
        instance.setGreyThresholds(greyThresholds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getColorVelMap method, of class CellData.
     */
    @Test
    public void testGetColorVelMap() {
        System.out.println("getColorVelMap");
        CellData instance = new CellData();
        ColorProcessor expResult = null;
        ColorProcessor result = instance.getColorVelMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setColorVelMap method, of class CellData.
     */
    @Test
    public void testSetColorVelMap() {
        System.out.println("setColorVelMap");
        ColorProcessor colorVelMap = null;
        CellData instance = new CellData();
        instance.setColorVelMap(colorVelMap);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNoisyVelocities method, of class CellData.
     */
    @Test
    public void testGetNoisyVelocities() {
        System.out.println("getNoisyVelocities");
        CellData instance = new CellData();
        double[][] expResult = null;
        double[][] result = instance.getNoisyVelocities();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setNoisyVelocities method, of class CellData.
     */
    @Test
    public void testSetNoisyVelocities() {
        System.out.println("setNoisyVelocities");
        double[][] noisyVelocities = null;
        CellData instance = new CellData();
        instance.setNoisyVelocities(noisyVelocities);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSmoothVelocities method, of class CellData.
     */
    @Test
    public void testGetSmoothVelocities() {
        System.out.println("getSmoothVelocities");
        CellData instance = new CellData();
        double[][] expResult = null;
        double[][] result = instance.getSmoothVelocities();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSmoothVelocities method, of class CellData.
     */
    @Test
    public void testSetSmoothVelocities() {
        System.out.println("setSmoothVelocities");
        double[][] smoothVelocities = null;
        CellData instance = new CellData();
        instance.setSmoothVelocities(smoothVelocities);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getEndFrame method, of class CellData.
     */
    @Test
    public void testGetEndFrame() {
        System.out.println("getEndFrame");
        CellData instance = new CellData();
        int expResult = 0;
        int result = instance.getEndFrame();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setEndFrame method, of class CellData.
     */
    @Test
    public void testSetEndFrame() {
        System.out.println("setEndFrame");
        int endFrame = 0;
        CellData instance = new CellData();
        instance.setEndFrame(endFrame);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getInitialRegion method, of class CellData.
     */
    @Test
    public void testGetInitialRegion() {
        System.out.println("getInitialRegion");
        CellData instance = new CellData();
        Region expResult = null;
        Region result = instance.getInitialRegion();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setInitialRegion method, of class CellData.
     */
    @Test
    public void testSetInitialRegion() {
        System.out.println("setInitialRegion");
        Region initialRegion = null;
        CellData instance = new CellData();
        instance.setInitialRegion(initialRegion);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCurvatureMaxima method, of class CellData.
     */
    @Test
    public void testGetCurvatureMaxima() {
        System.out.println("getCurvatureMaxima");
        CellData instance = new CellData();
        ArrayList[] expResult = null;
        ArrayList[] result = instance.getCurvatureMaxima();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCurvatureMaxima method, of class CellData.
     */
    @Test
    public void testSetCurvatureMaxima() {
        System.out.println("setCurvatureMaxima");
        ArrayList[] curvatureMaxima = null;
        CellData instance = new CellData();
        instance.setCurvatureMaxima(curvatureMaxima);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getImageWidth method, of class CellData.
     */
    @Test
    public void testGetImageWidth() {
        System.out.println("getImageWidth");
        CellData instance = new CellData();
        int expResult = 0;
        int result = instance.getImageWidth();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setImageWidth method, of class CellData.
     */
    @Test
    public void testSetImageWidth() {
        System.out.println("setImageWidth");
        int imageWidth = 0;
        CellData instance = new CellData();
        instance.setImageWidth(imageWidth);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getImageHeight method, of class CellData.
     */
    @Test
    public void testGetImageHeight() {
        System.out.println("getImageHeight");
        CellData instance = new CellData();
        int expResult = 0;
        int result = instance.getImageHeight();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setImageHeight method, of class CellData.
     */
    @Test
    public void testSetImageHeight() {
        System.out.println("setImageHeight");
        int imageHeight = 0;
        CellData instance = new CellData();
        instance.setImageHeight(imageHeight);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStartFrame method, of class CellData.
     */
    @Test
    public void testGetStartFrame() {
        System.out.println("getStartFrame");
        CellData instance = new CellData();
        int expResult = 0;
        int result = instance.getStartFrame();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLength method, of class CellData.
     */
    @Test
    public void testGetLength() {
        System.out.println("getLength");
        CellData instance = new CellData();
        int expResult = 0;
        int result = instance.getLength();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
