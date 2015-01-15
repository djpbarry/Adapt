/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class Analyse_BatchTest {
    
    public Analyse_BatchTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of run method, of class Analyse_Batch.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        String arg = "";
        Analyse_Batch instance = new Analyse_Batch();
        instance.run(arg);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of readParams method, of class Analyse_Batch.
     */
    @Test
    public void testReadParams() {
        System.out.println("readParams");
        UserVariables uv = null;
        File input = null;
        Analyse_Batch.readParams(uv, input);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
