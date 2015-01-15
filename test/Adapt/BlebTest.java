/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import ij.ImageStack;
import java.awt.Rectangle;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class BlebTest {
    
    public BlebTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getEndpos method, of class Bleb.
     */
    @Test
    public void testGetEndpos() {
        System.out.println("getEndpos");
        Bleb instance = new Bleb();
        ArrayList expResult = null;
        ArrayList result = instance.getEndpos();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStartpos method, of class Bleb.
     */
    @Test
    public void testGetStartpos() {
        System.out.println("getStartpos");
        Bleb instance = new Bleb();
        ArrayList expResult = null;
        ArrayList result = instance.getStartpos();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addStartPos method, of class Bleb.
     */
    @Test
    public void testAddStartPos() {
        System.out.println("addStartPos");
        int[] startpos = null;
        Bleb instance = new Bleb();
        boolean expResult = false;
        boolean result = instance.addStartPos(startpos);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addEndPos method, of class Bleb.
     */
    @Test
    public void testAddEndPos() {
        System.out.println("addEndPos");
        int[] endpos = null;
        Bleb instance = new Bleb();
        boolean expResult = false;
        boolean result = instance.addEndPos(endpos);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBlebPerimSigs method, of class Bleb.
     */
    @Test
    public void testGetBlebPerimSigs() {
        System.out.println("getBlebPerimSigs");
        Bleb instance = new Bleb();
        ArrayList<ArrayList<Double>> expResult = null;
        ArrayList<ArrayList<Double>> result = instance.getBlebPerimSigs();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setBlebPerimSigs method, of class Bleb.
     */
    @Test
    public void testSetBlebPerimSigs() {
        System.out.println("setBlebPerimSigs");
        ArrayList<ArrayList<Double>> blebPerimSigs = null;
        Bleb instance = new Bleb();
        instance.setBlebPerimSigs(blebPerimSigs);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getProtrusionLength method, of class Bleb.
     */
    @Test
    public void testGetProtrusionLength() {
        System.out.println("getProtrusionLength");
        Bleb instance = new Bleb();
        ArrayList<Double> expResult = null;
        ArrayList<Double> result = instance.getProtrusionLength();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setProtrusionLength method, of class Bleb.
     */
    @Test
    public void testSetProtrusionLength() {
        System.out.println("setProtrusionLength");
        ArrayList<Double> protrusionLength = null;
        Bleb instance = new Bleb();
        instance.setProtrusionLength(protrusionLength);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBounds method, of class Bleb.
     */
    @Test
    public void testGetBounds() {
        System.out.println("getBounds");
        Bleb instance = new Bleb();
        Rectangle expResult = null;
        Rectangle result = instance.getBounds();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setBounds method, of class Bleb.
     */
    @Test
    public void testSetBounds() {
        System.out.println("setBounds");
        Rectangle bounds = null;
        Bleb instance = new Bleb();
        instance.setBounds(bounds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDetectionStack method, of class Bleb.
     */
    @Test
    public void testGetDetectionStack() {
        System.out.println("getDetectionStack");
        Bleb instance = new Bleb();
        ImageStack expResult = null;
        ImageStack result = instance.getDetectionStack();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setDetectionStack method, of class Bleb.
     */
    @Test
    public void testSetDetectionStack() {
        System.out.println("setDetectionStack");
        ImageStack detectionStack = null;
        Bleb instance = new Bleb();
        instance.setDetectionStack(detectionStack);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
