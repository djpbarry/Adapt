/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import java.io.File;

/**
 *
 */
public class Main {

//    public static void main(String args[]) {
//        Analyse_Movie am = new Analyse_Movie();
//        am.initialise();
//        am.run(null);
//        System.exit(0);
//    }
    public static void main(String args[]) {
//        Random r = new Random();
//        for (int i = 0; i < 20; i++) {
//            UserVariables.setMinCurveRange(4 + r.nextInt(3));
//            UserVariables.setCortexDepth(0.48 + r.nextDouble() * 0.24);
//            UserVariables.setTempFiltRad(4.0 + r.nextDouble() * 2.0);
//            UserVariables.setSpatFiltRad(4.0 + r.nextDouble() * 2.0);
//            UserVariables.setSigThreshFact(0.0 + r.nextDouble() * 0.5);
//            UserVariables.setMinCurveThresh(0.0 + r.nextDouble() * 5.0);
//            UserVariables.setSigRecoveryThresh(0.2 + r.nextDouble() * 0.1);
        Analyse_Batch am =new Analyse_Batch(false, true,
                "C:\\Users\\barry05\\Desktop\\Blebbing\\Tests\\Test\\Adapt_v1.100_Output\\params.csv",
                new File("C:\\Users\\barry05\\Desktop\\Incucyte Movies\\2014.10.22_Incucyte\\Background_Subtracted"),
                null);
        am.run(null);
//        }
        System.exit(0);
    }
}
