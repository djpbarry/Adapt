/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import UtilClasses.Utilities;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PointRoi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;
import ui.GUI;

/**
 *
 */
public class Analyse_Batch extends Analyse_Movie {

    private boolean showGUI = true, mono = false;
    private File c1Directory, c2Directory;
//

    public Analyse_Batch() {
        super();
    }

    public Analyse_Batch(boolean showGUI, boolean mono, File c1Directory, File c2Directory, UserVariables uv) {
        super();
        this.showGUI = showGUI;
        this.mono = mono;
        this.c1Directory = c1Directory;
        this.c2Directory = c2Directory;
        this.uv = uv;
    }

    public void run(String arg) {
//        Utilities.setLookAndFeel(GUI.class);
        TITLE = TITLE + "_v" + StaticVariables.VERSION + "." + numFormat.format(Revision.Revision.revisionNumber);
        File cytoImageFiles[] = null; // Obtain file list
        File sigImageFiles[] = null;
        batchMode = true;
//        if (arg == null) {
        if (c1Directory == null) {
            directory = Utilities.getFolder(directory, "Select directory for reference channel", true);
            if (directory == null) {
                return;
            }
        } else {
            directory = c1Directory;
        }
        cytoImageFiles = directory.listFiles(); // Obtain file list
        if (c2Directory == null && !mono) {
            c2Directory = Utilities.getFolder(directory, "Select directory for second channel", false);
        }
        directory = new File(directory.getAbsolutePath() + delimiter + "..");
//        } else {
//            directory = new File(arg + delimiter + "cyto");
//            cytoImageFiles = directory.listFiles(); // Obtain file list
//            secondChannel = new File(arg + delimiter + "sig");
//            directory = new File(directory.getAbsolutePath() + delimiter + "..");
//        }
        int cytoSize = cytoImageFiles.length;
        int sigSize = 0;
        if (c2Directory != null) {
            sigImageFiles = c2Directory.listFiles();
            sigSize = sigImageFiles.length;
        }
        Arrays.sort(cytoImageFiles);
        if (sigImageFiles != null) {
            Arrays.sort(sigImageFiles);
        }
        for (int f = 0; f < cytoSize; f++) {
            ImagePlus cytoImp = new ImagePlus(cytoImageFiles[f].getAbsolutePath());
            ImageStack cytoStack = cytoImp.getImageStack();
            roi = (PointRoi) cytoImp.getRoi();
            if (cytoStack != null && cytoStack.getSize() > 0) {
                try {
                    ImageStack sigStack;
                    if (cytoSize == sigSize) {
                        ImagePlus sigImp = new ImagePlus(sigImageFiles[f].getAbsolutePath());
                        sigStack = sigImp.getImageStack();
                    } else {
                        sigStack = null;
                    }
                    stacks[0] = cytoStack;
                    stacks[1] = sigStack;
                    if (showGUI) {
                        showGUI = false;
                        GUI gui = new GUI(null, true, TITLE, stacks, roi);
                        gui.setVisible(true);
                        if (!gui.isWasOKed()) {
                            return;
                        }
                        uv = GUI.getUv();
                    }
                    analyse(cytoImageFiles[f].getName());
                } catch (Exception e) {
                    IJ.log("Failed to analyse " + cytoImageFiles[f].getName());
                }
            }
        }
        IJ.showStatus(TITLE + " done.");
    }

    public static void readParams(UserVariables uv, File input) {
        Pattern p = Pattern.compile("\\S*,\\s*");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
            br.readLine();
            br.readLine();
            br.readLine();
            uv.setAutoThreshold(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setThreshMethod(new Scanner(br.readLine()).useDelimiter(p).next());
            uv.setGreyThresh(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setSpatialRes(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setTimeRes(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setErosion(new Scanner(br.readLine()).useDelimiter(p).nextInt());
            uv.setSpatFiltRad(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setTempFiltRad(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setGaussRad(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setGenVis(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setGetMorph(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setAnalyseProtrusions(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setBlebDetect(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setCurveRange(new Scanner(br.readLine()).useDelimiter(p).nextInt());
            uv.setMinCurveThresh(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
//            uv.setMaxCurveThresh(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setBlebLenThresh(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setBlebDurThresh(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setCutOffTime(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setCortexDepth(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setUseSigThresh(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setSigThreshFact(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setSigRecoveryThresh(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
//            uv.setSimple(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
//            uv.setLambda(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setMinLength(new Scanner(br.readLine()).useDelimiter(p).nextInt());
            uv.setFiloSize(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            uv.setGetFluorDist(new Scanner(br.readLine()).useDelimiter(p).nextBoolean());
            uv.setMorphSizeMin(new Scanner(br.readLine()).useDelimiter(p).nextDouble());
            br.close();
        } catch (Exception e) {
            System.out.println("Error reading parameter file.");
        }
    }

}
