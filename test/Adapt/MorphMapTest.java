/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import IAClasses.CrossCorrelation;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class MorphMapTest {
    
    public MorphMapTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getHeight method, of class MorphMap.
     */
    @Test
    public void testGetHeight() {
        System.out.println("getHeight");
        MorphMap instance = new MorphMap();
        int expResult = 0;
        int result = instance.getHeight();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getWidth method, of class MorphMap.
     */
    @Test
    public void testGetWidth() {
        System.out.println("getWidth");
        MorphMap instance = new MorphMap();
        int expResult = 0;
        int result = instance.getWidth();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of smoothMap method, of class MorphMap.
     */
    @Test
    public void testSmoothMap() {
        System.out.println("smoothMap");
        double tempFiltRad = 0.0;
        double spatFiltRad = 0.0;
        MorphMap instance = new MorphMap();
        double[][] expResult = null;
        double[][] result = instance.smoothMap(tempFiltRad, spatFiltRad);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addColumn method, of class MorphMap.
     */
    @Test
    public void testAddColumn() {
        System.out.println("addColumn");
        double[] x = null;
        double[] y = null;
        double[] z = null;
        int t = 0;
        MorphMap instance = new MorphMap();
        instance.addColumn(x, y, z, t);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getxCoords method, of class MorphMap.
     */
    @Test
    public void testGetxCoords() {
        System.out.println("getxCoords");
        MorphMap instance = new MorphMap();
        double[][] expResult = null;
        double[][] result = instance.getxCoords();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getyCoords method, of class MorphMap.
     */
    @Test
    public void testGetyCoords() {
        System.out.println("getyCoords");
        MorphMap instance = new MorphMap();
        double[][] expResult = null;
        double[][] result = instance.getyCoords();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of allignMap method, of class MorphMap.
     */
    @Test
    public void testAllignMap() {
        System.out.println("allignMap");
        MorphMap instance = new MorphMap();
        instance.allignMap();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of allignMapMaxDistToPoint method, of class MorphMap.
     */
    @Test
    public void testAllignMapMaxDistToPoint() {
        System.out.println("allignMapMaxDistToPoint");
        int p = 0;
        CellData cellData = null;
        MorphMap instance = new MorphMap();
        instance.allignMapMaxDistToPoint(p, cellData);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getzVals method, of class MorphMap.
     */
    @Test
    public void testGetzVals() {
        System.out.println("getzVals");
        MorphMap instance = new MorphMap();
        double[][] expResult = null;
        double[][] result = instance.getzVals();
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of calcRateOfChange method, of class MorphMap.
     */
    @Test
    public void testCalcRateOfChange() {
        System.out.println("calcRateOfChange");
        ImageProcessor input = null;
        MorphMap instance = new MorphMap();
        ImageProcessor expResult = null;
        ImageProcessor result = instance.calcRateOfChange(input);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
