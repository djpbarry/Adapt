/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class BlebAnalyserTest {
    
    public BlebAnalyserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of extractAreaSignalData method, of class BlebAnalyser.
     */
    @Test
    public void testExtractAreaSignalData() {
        System.out.println("extractAreaSignalData");
        Bleb currentBleb = null;
        CellData cellData = null;
        int index = 0;
        ImageStack[] stacks = null;
        UserVariables uv = null;
        boolean expResult = false;
        boolean result = BlebAnalyser.extractAreaSignalData(currentBleb, cellData, index, stacks, uv);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of drawBlebSigMap method, of class BlebAnalyser.
     */
    @Test
    public void testDrawBlebSigMap() {
        System.out.println("drawBlebSigMap");
        Bleb currentBleb = null;
        double spatialRes = 0.0;
        boolean useSigThresh = false;
        ImageProcessor expResult = null;
        ImageProcessor result = BlebAnalyser.drawBlebSigMap(currentBleb, spatialRes, useSigThresh);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of drawBlebMask method, of class BlebAnalyser.
     */
    @Test
    public void testDrawBlebMask() {
        System.out.println("drawBlebMask");
        Polygon poly = null;
        int radius = 0;
        int width = 0;
        int height = 0;
        int fg = 0;
        int bg = 0;
        ByteProcessor expResult = null;
        ByteProcessor result = BlebAnalyser.drawBlebMask(poly, radius, width, height, fg, bg);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
