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

    public Analyse_Batch() {
        super();
    }

    public void run(String arg) {
        Utilities.setLookAndFeel(GUI.class);
        TITLE = TITLE + "_v" + StaticVariables.VERSION + "." + numFormat.format(Revision.Revision.revisionNumber);
        directory = Utilities.getFolder(directory, "Select directory for reference channel", true);
        if (directory == null) {
            return;
        }
        batchMode = true;
        File cytoImageFiles[] = directory.listFiles(); // Obtain file list
        int cytoSize = cytoImageFiles.length;
        File sigImageFiles[] = null;
        File secondChannel = Utilities.getFolder(directory, "Select directory for second channel", false);
        int sigSize = 0;
        if (secondChannel != null) {
            sigImageFiles = secondChannel.listFiles();
            sigSize = sigImageFiles.length;
        }
        Arrays.sort(cytoImageFiles);
        if (sigImageFiles != null) {
            Arrays.sort(sigImageFiles);
        }
        for (int f = 0; f < cytoSize; f++) {
            ImagePlus cytoImp = new ImagePlus(cytoImageFiles[f].getAbsolutePath());
            ImageStack cytoStack = cytoImp.getImageStack();
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
                        GUI gui = new GUI(null, true, TITLE, stacks, this);
                        gui.setVisible(true);
                        if (!gui.isWasOKed()) {
                            return;
                        }
                    }
                    analyse(cytoImageFiles[f].getName());
                } catch (Exception e) {
                    IJ.log("Failed to analyse " + cytoImageFiles[f].getName());
                }
            }
        }
        IJ.showStatus(TITLE + " done.");
    }
}