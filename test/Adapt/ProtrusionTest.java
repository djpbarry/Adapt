/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import java.awt.Polygon;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class ProtrusionTest {
    
    public ProtrusionTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getMaxExtent method, of class Protrusion.
     */
    @Test
    public void testGetMaxExtent() {
        System.out.println("getMaxExtent");
        Protrusion instance = new Protrusion();
        int expResult = 0;
        int result = instance.getMaxExtent();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMaxExtent method, of class Protrusion.
     */
    @Test
    public void testSetMaxExtent() {
        System.out.println("setMaxExtent");
        int maxExtent = 0;
        Protrusion instance = new Protrusion();
        instance.setMaxExtent(maxExtent);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getPolys method, of class Protrusion.
     */
    @Test
    public void testGetPolys() {
        System.out.println("getPolys");
        Protrusion instance = new Protrusion();
        ArrayList<Polygon> expResult = null;
        ArrayList<Polygon> result = instance.getPolys();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setPolys method, of class Protrusion.
     */
    @Test
    public void testSetPolys() {
        System.out.println("setPolys");
        ArrayList<Polygon> polys = null;
        Protrusion instance = new Protrusion();
        instance.setPolys(polys);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSumSig method, of class Protrusion.
     */
    @Test
    public void testGetSumSig() {
        System.out.println("getSumSig");
        Protrusion instance = new Protrusion();
        ArrayList<Double> expResult = null;
        ArrayList<Double> result = instance.getSumSig();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSumSig method, of class Protrusion.
     */
    @Test
    public void testSetSumSig() {
        System.out.println("setSumSig");
        ArrayList<Double> sumSig = null;
        Protrusion instance = new Protrusion();
        instance.setSumSig(sumSig);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMeanVel method, of class Protrusion.
     */
    @Test
    public void testGetMeanVel() {
        System.out.println("getMeanVel");
        Protrusion instance = new Protrusion();
        ArrayList<Double> expResult = null;
        ArrayList<Double> result = instance.getMeanVel();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMeanVel method, of class Protrusion.
     */
    @Test
    public void testSetMeanVel() {
        System.out.println("setMeanVel");
        ArrayList<Double> meanVel = null;
        Protrusion instance = new Protrusion();
        instance.setMeanVel(meanVel);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
