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
package Visualisation;

import Adapt.StaticVariables;
import Cell.CellData;
import Overlay.OverlayToRoi;
import Process.MultiThreadedProcess;
import UserVariables.UserVariables;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.plugin.frame.RoiManager;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class MultiThreadedVisualisationGenerator extends MultiThreadedProcess {

    ArrayList<CellData> cellData;
    boolean protMode;
    ImageStack cytoStack;
    UserVariables uv;
    File velDirName;
    File curvDirName;
    protected DecimalFormat numFormat = StaticVariables.numFormat;
    private final Overlay labels;

    public void setup() {

    }

    public MultiThreadedVisualisationGenerator(ExecutorService exec, ArrayList<CellData> cellData, boolean protMode, ImageStack cytoStack, UserVariables uv, File velDirName, File curvDirName) {
        super(null, null);
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

        int stackSize = cytoStack.getSize();
        for (int t = 0; t < stackSize; t++) {
            exec.submit(new RunnableVisualisationGenerator(cellData, protMode, cytoStack, uv, velDirName, curvDirName, numFormat, t, labels));
        }
        terminate("Error generating visualisations.");
        saveOverlays();
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

}
