/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import ij.IJ;

/**
 *
 */
public class Main {

    public static void main(String args[]) {
        Analyse_Movie am = new Analyse_Movie();
        am.initialise();
        am.run(null);
        System.exit(0);
    }
//    public static void main(String args[]) {
////        Random r = new Random();
////        for (int i = 0; i < 20; i++) {
//        UserVariables uv = new UserVariables();
////        Analyse_Batch.readParams(uv, new File("C:\\Users\\barry05\\Desktop\\Test_Data_Sets\\adapt_test_data\\Test Data For Paper\\Adapt_v1.105_Output\\params.csv"));
////            uv.setCurveRange(4 + r.nextInt(3));
////            uv.setCortexDepth(0.48 + r.nextDouble() * 0.24);
////            uv.setTempFiltRad(4.0 + r.nextDouble() * 2.0);
////            uv.setSpatFiltRad(4.0 + r.nextDouble() * 2.0);
////            uv.setSigThreshFact(0.0 + r.nextDouble() * 0.5);
////            uv.setMinCurveThresh(0.0 + r.nextDouble() * 5.0);
////            uv.setSigRecoveryThresh(0.2 + r.nextDouble() * 0.1);
////            Analyse_Batch am = new Analyse_Batch(false, true,
////                    new File("C:\\Users\\barry05\\Desktop\\Test_Data_Sets\\adapt_test_data\\Test Data For Paper\\Stacks\\cyto"),
////                    new File("C:\\Users\\barry05\\Desktop\\Test_Data_Sets\\adapt_test_data\\Test Data For Paper\\Stacks\\sig"),
////                    uv);
////            am.run(null);
////        }
////        Analyse_Batch am = new Analyse_Batch();
//        Analyse_Batch am = new Analyse_Batch(true, false, null, null, uv);
//        am.run(null);
//        System.exit(0);
//    }
}
