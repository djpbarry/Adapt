/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class UserVariablesTest {
    
    public UserVariablesTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of getOvalRadius method, of class UserVariables.
     */
    @Test
    public void testGetOvalRadius() {
        System.out.println("getOvalRadius");
        UserVariables instance = new UserVariables();
        int expResult = 0;
        int result = instance.getOvalRadius();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isUsedSmoothedVels method, of class UserVariables.
     */
    @Test
    public void testIsUsedSmoothedVels() {
        System.out.println("isUsedSmoothedVels");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isUsedSmoothedVels();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBlebDurThresh method, of class UserVariables.
     */
    @Test
    public void testGetBlebDurThresh() {
        System.out.println("getBlebDurThresh");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getBlebDurThresh();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setBlebDurThresh method, of class UserVariables.
     */
    @Test
    public void testSetBlebDurThresh() {
        System.out.println("setBlebDurThresh");
        double blebDurThresh = 0.0;
        UserVariables instance = new UserVariables();
        instance.setBlebDurThresh(blebDurThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setBlebLenThresh method, of class UserVariables.
     */
    @Test
    public void testSetBlebLenThresh() {
        System.out.println("setBlebLenThresh");
        double blebLenThresh = 0.0;
        UserVariables instance = new UserVariables();
        instance.setBlebLenThresh(blebLenThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBlebLenThresh method, of class UserVariables.
     */
    @Test
    public void testGetBlebLenThresh() {
        System.out.println("getBlebLenThresh");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getBlebLenThresh();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGreyThresh method, of class UserVariables.
     */
    @Test
    public void testGetGreyThresh() {
        System.out.println("getGreyThresh");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getGreyThresh();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGreyThresh method, of class UserVariables.
     */
    @Test
    public void testSetGreyThresh() {
        System.out.println("setGreyThresh");
        double greyThresh = 0.0;
        UserVariables instance = new UserVariables();
        instance.setGreyThresh(greyThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isGenVis method, of class UserVariables.
     */
    @Test
    public void testIsGenVis() {
        System.out.println("isGenVis");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isGenVis();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGenVis method, of class UserVariables.
     */
    @Test
    public void testSetGenVis() {
        System.out.println("setGenVis");
        boolean genVis = false;
        UserVariables instance = new UserVariables();
        instance.setGenVis(genVis);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCurveRange method, of class UserVariables.
     */
    @Test
    public void testGetCurveRange() {
        System.out.println("getCurveRange");
        UserVariables instance = new UserVariables();
        int expResult = 0;
        int result = instance.getCurveRange();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCurveRange method, of class UserVariables.
     */
    @Test
    public void testSetCurveRange() {
        System.out.println("setCurveRange");
        int curveRange = 0;
        UserVariables instance = new UserVariables();
        instance.setCurveRange(curveRange);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isUseSigThresh method, of class UserVariables.
     */
    @Test
    public void testIsUseSigThresh() {
        System.out.println("isUseSigThresh");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isUseSigThresh();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setUseSigThresh method, of class UserVariables.
     */
    @Test
    public void testSetUseSigThresh() {
        System.out.println("setUseSigThresh");
        boolean useSigThresh = false;
        UserVariables instance = new UserVariables();
        instance.setUseSigThresh(useSigThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSpatialRes method, of class UserVariables.
     */
    @Test
    public void testGetSpatialRes() {
        System.out.println("getSpatialRes");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getSpatialRes();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSpatialRes method, of class UserVariables.
     */
    @Test
    public void testSetSpatialRes() {
        System.out.println("setSpatialRes");
        double spatialRes = 0.0;
        UserVariables instance = new UserVariables();
        instance.setSpatialRes(spatialRes);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCutOffTime method, of class UserVariables.
     */
    @Test
    public void testGetCutOffTime() {
        System.out.println("getCutOffTime");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getCutOffTime();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCutOffTime method, of class UserVariables.
     */
    @Test
    public void testSetCutOffTime() {
        System.out.println("setCutOffTime");
        double cutOffTime = 0.0;
        UserVariables instance = new UserVariables();
        instance.setCutOffTime(cutOffTime);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCortexDepth method, of class UserVariables.
     */
    @Test
    public void testGetCortexDepth() {
        System.out.println("getCortexDepth");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getCortexDepth();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCortexDepth method, of class UserVariables.
     */
    @Test
    public void testSetCortexDepth() {
        System.out.println("setCortexDepth");
        double cortexDepth = 0.0;
        UserVariables instance = new UserVariables();
        instance.setCortexDepth(cortexDepth);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isAutoThreshold method, of class UserVariables.
     */
    @Test
    public void testIsAutoThreshold() {
        System.out.println("isAutoThreshold");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isAutoThreshold();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAutoThreshold method, of class UserVariables.
     */
    @Test
    public void testSetAutoThreshold() {
        System.out.println("setAutoThreshold");
        boolean autoThreshold = false;
        UserVariables instance = new UserVariables();
        instance.setAutoThreshold(autoThreshold);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getTempFiltRad method, of class UserVariables.
     */
    @Test
    public void testGetTempFiltRad() {
        System.out.println("getTempFiltRad");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getTempFiltRad();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setTempFiltRad method, of class UserVariables.
     */
    @Test
    public void testSetTempFiltRad() {
        System.out.println("setTempFiltRad");
        double tempFiltRad = 0.0;
        UserVariables instance = new UserVariables();
        instance.setTempFiltRad(tempFiltRad);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSigThreshFact method, of class UserVariables.
     */
    @Test
    public void testGetSigThreshFact() {
        System.out.println("getSigThreshFact");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getSigThreshFact();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSigThreshFact method, of class UserVariables.
     */
    @Test
    public void testSetSigThreshFact() {
        System.out.println("setSigThreshFact");
        double sigThreshFact = 0.0;
        UserVariables instance = new UserVariables();
        instance.setSigThreshFact(sigThreshFact);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSpatFiltRad method, of class UserVariables.
     */
    @Test
    public void testGetSpatFiltRad() {
        System.out.println("getSpatFiltRad");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getSpatFiltRad();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSpatFiltRad method, of class UserVariables.
     */
    @Test
    public void testSetSpatFiltRad() {
        System.out.println("setSpatFiltRad");
        double spatFiltRad = 0.0;
        UserVariables instance = new UserVariables();
        instance.setSpatFiltRad(spatFiltRad);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getErosion method, of class UserVariables.
     */
    @Test
    public void testGetErosion() {
        System.out.println("getErosion");
        UserVariables instance = new UserVariables();
        int expResult = 0;
        int result = instance.getErosion();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setErosion method, of class UserVariables.
     */
    @Test
    public void testSetErosion() {
        System.out.println("setErosion");
        int erosion = 0;
        UserVariables instance = new UserVariables();
        instance.setErosion(erosion);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isGetMorph method, of class UserVariables.
     */
    @Test
    public void testIsGetMorph() {
        System.out.println("isGetMorph");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isGetMorph();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGetMorph method, of class UserVariables.
     */
    @Test
    public void testSetGetMorph() {
        System.out.println("setGetMorph");
        boolean getMorph = false;
        UserVariables instance = new UserVariables();
        instance.setGetMorph(getMorph);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getTimeRes method, of class UserVariables.
     */
    @Test
    public void testGetTimeRes() {
        System.out.println("getTimeRes");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getTimeRes();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setTimeRes method, of class UserVariables.
     */
    @Test
    public void testSetTimeRes() {
        System.out.println("setTimeRes");
        double timeRes = 0.0;
        UserVariables instance = new UserVariables();
        instance.setTimeRes(timeRes);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMinCurveThresh method, of class UserVariables.
     */
    @Test
    public void testGetMinCurveThresh() {
        System.out.println("getMinCurveThresh");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getMinCurveThresh();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMinCurveThresh method, of class UserVariables.
     */
    @Test
    public void testSetMinCurveThresh() {
        System.out.println("setMinCurveThresh");
        double curveThresh = 0.0;
        UserVariables instance = new UserVariables();
        instance.setMinCurveThresh(curveThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isAnalyseProtrusions method, of class UserVariables.
     */
    @Test
    public void testIsAnalyseProtrusions() {
        System.out.println("isAnalyseProtrusions");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isAnalyseProtrusions();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAnalyseProtrusions method, of class UserVariables.
     */
    @Test
    public void testSetAnalyseProtrusions() {
        System.out.println("setAnalyseProtrusions");
        boolean analyseProtrusions = false;
        UserVariables instance = new UserVariables();
        instance.setAnalyseProtrusions(analyseProtrusions);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSigRecoveryThresh method, of class UserVariables.
     */
    @Test
    public void testGetSigRecoveryThresh() {
        System.out.println("getSigRecoveryThresh");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getSigRecoveryThresh();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSigRecoveryThresh method, of class UserVariables.
     */
    @Test
    public void testSetSigRecoveryThresh() {
        System.out.println("setSigRecoveryThresh");
        double sigRecoveryThresh = 0.0;
        UserVariables instance = new UserVariables();
        instance.setSigRecoveryThresh(sigRecoveryThresh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGaussRad method, of class UserVariables.
     */
    @Test
    public void testGetGaussRad() {
        System.out.println("getGaussRad");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getGaussRad();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGaussRad method, of class UserVariables.
     */
    @Test
    public void testSetGaussRad() {
        System.out.println("setGaussRad");
        double gaussRad = 0.0;
        UserVariables instance = new UserVariables();
        instance.setGaussRad(gaussRad);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLambda method, of class UserVariables.
     */
    @Test
    public void testGetLambda() {
        System.out.println("getLambda");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getLambda();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setLambda method, of class UserVariables.
     */
    @Test
    public void testSetLambda() {
        System.out.println("setLambda");
        double lambda = 0.0;
        UserVariables instance = new UserVariables();
        instance.setLambda(lambda);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMinLength method, of class UserVariables.
     */
    @Test
    public void testGetMinLength() {
        System.out.println("getMinLength");
        UserVariables instance = new UserVariables();
        int expResult = 0;
        int result = instance.getMinLength();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMinLength method, of class UserVariables.
     */
    @Test
    public void testSetMinLength() {
        System.out.println("setMinLength");
        int minLength = 0;
        UserVariables instance = new UserVariables();
        instance.setMinLength(minLength);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getThreshMethod method, of class UserVariables.
     */
    @Test
    public void testGetThreshMethod() {
        System.out.println("getThreshMethod");
        UserVariables instance = new UserVariables();
        String expResult = "";
        String result = instance.getThreshMethod();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setThreshMethod method, of class UserVariables.
     */
    @Test
    public void testSetThreshMethod() {
        System.out.println("setThreshMethod");
        String threshMethod = "";
        UserVariables instance = new UserVariables();
        instance.setThreshMethod(threshMethod);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isBlebDetect method, of class UserVariables.
     */
    @Test
    public void testIsBlebDetect() {
        System.out.println("isBlebDetect");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isBlebDetect();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setBlebDetect method, of class UserVariables.
     */
    @Test
    public void testSetBlebDetect() {
        System.out.println("setBlebDetect");
        boolean velDetect = false;
        UserVariables instance = new UserVariables();
        instance.setBlebDetect(velDetect);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFiloSize method, of class UserVariables.
     */
    @Test
    public void testGetFiloSize() {
        System.out.println("getFiloSize");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getFiloSize();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFiloSize method, of class UserVariables.
     */
    @Test
    public void testSetFiloSize() {
        System.out.println("setFiloSize");
        double filoSize = 0.0;
        UserVariables instance = new UserVariables();
        instance.setFiloSize(filoSize);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isGetFluorDist method, of class UserVariables.
     */
    @Test
    public void testIsGetFluorDist() {
        System.out.println("isGetFluorDist");
        UserVariables instance = new UserVariables();
        boolean expResult = false;
        boolean result = instance.isGetFluorDist();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setGetFluorDist method, of class UserVariables.
     */
    @Test
    public void testSetGetFluorDist() {
        System.out.println("setGetFluorDist");
        boolean getFluorDist = false;
        UserVariables instance = new UserVariables();
        instance.setGetFluorDist(getFluorDist);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMorphSizeMin method, of class UserVariables.
     */
    @Test
    public void testGetMorphSizeMin() {
        System.out.println("getMorphSizeMin");
        UserVariables instance = new UserVariables();
        double expResult = 0.0;
        double result = instance.getMorphSizeMin();
        assertEquals(expResult, result, 0.0);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMorphSizeMin method, of class UserVariables.
     */
    @Test
    public void testSetMorphSizeMin() {
        System.out.println("setMorphSizeMin");
        double morphSizeMin = 0.0;
        UserVariables instance = new UserVariables();
        instance.setMorphSizeMin(morphSizeMin);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clone method, of class UserVariables.
     */
    @Test
    public void testClone() {
        System.out.println("clone");
        UserVariables instance = new UserVariables();
        Object expResult = null;
        Object result = instance.clone();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
