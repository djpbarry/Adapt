/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import UserVariables.UserVariables;
import Cell.CellData;
import ij.ImageStack;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class CurveMapAnalyserTest {
    
    public CurveMapAnalyserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of findAllCurvatureExtrema method, of class CurveMapAnalyser.
     */
    @Test
    public void testFindAllCurvatureExtrema() {
        System.out.println("findAllCurvatureExtrema");
        CellData cellData = null;
        int startFrame = 0;
        int endFrame = 0;
        boolean min = false;
        double threshold = 0.0;
        double curveRange = 0.0;
        UserVariables uv = null;
        double minDuration = 0.0;
        ArrayList[] expResult = null;
        ArrayList[] result = CurveMapAnalyser.findAllCurvatureExtrema(cellData, startFrame, endFrame, min, threshold, curveRange, uv, minDuration);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of calcScaledCurveRange method, of class CurveMapAnalyser.
     */
    @Test
    public void testCalcScaledCurveRange() {
        System.out.println("calcScaledCurveRange");
        double curveRange = 0.0;
        double scaleFactor = 0.0;
        int expResult = 0;
        int result = CurveMapAnalyser.calcScaledCurveRange(curveRange, scaleFactor);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of drawAllExtrema method, of class CurveMapAnalyser.
     */
    @Test
    public void testDrawAllExtrema() {
        System.out.println("drawAllExtrema");
        CellData cellData = null;
        double timeRes = 0.0;
        double spatialRes = 0.0;
        ImageStack cytoStack = null;
        int startFrame = 0;
        int endFrame = 0;
        double minDuration = 0.0;
        CurveMapAnalyser.drawAllExtrema(cellData, timeRes, spatialRes, cytoStack, startFrame, endFrame, minDuration);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of updateAnchorPoint method, of class CurveMapAnalyser.
     */
    @Test
    public void testUpdateAnchorPoint() {
        System.out.println("updateAnchorPoint");
        int time = 0;
        int[] anchor = null;
        int maxRange = 0;
        CellData cellData = null;
        CurveMapAnalyser.updateAnchorPoint(time, anchor, maxRange, cellData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
