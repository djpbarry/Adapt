/*
 * Copyright (C) 2018 David Barry <david.barry at crick dot ac dot uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.calm.adapt.visualisation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import loci.formats.FormatTools;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImageWriter;
import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Lut.LUTCreator;
import net.calm.iaclasslibrary.Overlay.OverlayToRoi;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.UserVariables.UserVariables;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedVisualisationGenerator extends MultiThreadedProcess {

    ArrayList<CellData> cellData;
    boolean protMode;
    ImageStack cytoStack;
    UserVariables uv;
    File velDirName;
    File curvDirName;
    private final Overlay labels;

    public MultiThreadedVisualisationGenerator() {
        this(null, null, false, null, null, null, null);
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {

    }

    public MultiThreadedVisualisationGenerator(ExecutorService exec, ArrayList<CellData> cellData, boolean protMode, ImageStack cytoStack, UserVariables uv, File velDirName, File curvDirName) {
        super(null);
        this.cellData = cellData;
        this.protMode = protMode;
        this.cytoStack = cytoStack;
        this.uv = uv;
        this.velDirName = velDirName;
        this.curvDirName = curvDirName;
        this.labels = new Overlay();
    }

    @Override
    public void run() {
        IJ.log("Building visualisations...");
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int stackSize = cytoStack.getSize();
        IndexColorModel lut = (new LUTCreator()).getRedGreen();
        FloatProcessor[] velFrames = new FloatProcessor[stackSize];
        FloatProcessor[] curveFrames = new FloatProcessor[stackSize];
        for (int t = 0; t < stackSize; t++) {
            exec.submit(new RunnableVisualisationGenerator(cellData, protMode, cytoStack, uv, t, labels, lut, velFrames, curveFrames));
        }
        terminate("Error generating visualisations.");
        saveStacks(velFrames, curveFrames, lut);
        saveOverlays();
    }

    void saveStacks(FloatProcessor[] velFrames, FloatProcessor[] curveFrames, IndexColorModel lut) {
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        ImageStack velStack = new ImageStack(width, height);
        ImageStack curveStack = new ImageStack(width, height);
        for (int t = 0; t < velFrames.length; t++) {
            if (velFrames[t] != null) {
                velStack.addSlice(velFrames[t]);
            }
            if (curveFrames[t] != null) {
                curveStack.addSlice(curveFrames[t]);
            }
        }
        int[] dims = {width, height, 1, 1, velStack.getSize()};
        try {
            BioFormatsImageWriter.saveStack(velStack, new File(velDirName, "velocity_visualisation.tif"), lut, FormatTools.FLOAT, "XYZCT", dims, false);
            BioFormatsImageWriter.saveStack(curveStack, new File(curvDirName, "curvature_visualisation.tif"), lut, FormatTools.FLOAT, "XYZCT", dims, false);
        } catch (Exception e) {
            GenUtils.logError(e, "Failed to save visualisation stack.");
        }
    }

    void saveOverlays() {
        ImagePlus c1Imp = new ImagePlus("", cytoStack);
        c1Imp.setOverlay(labels);
        OverlayToRoi.toRoi(c1Imp);
        RoiManager rm = RoiManager.getInstance2();
        rm.runCommand("Save", String.format("%s%slabels.zip", velDirName.getParent(), File.separator));
        rm.reset();
        rm.close();
    }

    public MultiThreadedVisualisationGenerator duplicate() {
        MultiThreadedVisualisationGenerator newProcess = new MultiThreadedVisualisationGenerator();
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    public void setup(LocationAgnosticBioFormatsImg var1, Properties var2, String[] var3){

    }
}
