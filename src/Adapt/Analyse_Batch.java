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
import java.io.File;
import java.util.Arrays;
import ui.GUI;

/**
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class Analyse_Batch extends Analyse_Movie {

    private boolean showGUI = true;
//
//    public static void main(String args[]) {
//        Analyse_Batch am = new Analyse_Batch();
//        am.run(null);
//        System.exit(0);
//    }
    
    public Analyse_Batch() {
        super();
    }

    public void run(String arg) {
        Utilities.setLookAndFeel(GUI.class);
        TITLE = TITLE + "_v" + StaticVariables.VERSION + "." + numFormat.format(Revision.Revision.revisionNumber);
        directory = Utilities.getFolder(directory, null);
        if (directory == null) {
            return;
        }
        batchMode = true;
        File cytoImageFiles[] = (new File(directory.getPath() + delimiter + StaticVariables.CYTO)).listFiles(); // Obtain file list
        int cytoSize = cytoImageFiles.length;
        File sigImageFiles[] = (new File(directory.getPath() + delimiter + StaticVariables.SIG)).listFiles(); // Obtain file list
        int sigSize = sigImageFiles.length;
        Arrays.sort(cytoImageFiles);
        Arrays.sort(sigImageFiles);
        int numFiles = cytoImageFiles.length;
        for (int f = 0; f < numFiles; f++) {
            ImagePlus cytoImp = new ImagePlus(cytoImageFiles[f].getAbsolutePath());
            ImageStack cytoStack = cytoImp.getImageStack();
            if (cytoStack != null) {
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
                    GUI gui = new GUI(null, true, TITLE, stacks, this);
                    gui.setVisible(true);
                    if (!gui.isWasOKed()) {
                        return;
                    }
                }
                analyse(arg);
                IJ.showStatus(TITLE + " done.");
            }
        }
    }
}
