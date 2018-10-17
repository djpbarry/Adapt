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
package Output;

import Cell.CellData;
import Process.MultiThreadedProcess;
import UserVariables.UserVariables;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImageStack;
import ij.gui.PointRoi;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class MultiThreadedOutputGenerator extends MultiThreadedProcess {

    private ArrayList<CellData> cellData;
    String parDir;
    boolean protMode;
    UserVariables uv;
    File childDir;
    ImageStack sigStack;
    ImageStack cytoStack;
    File directory;
    PointRoi roi;
    private final ArrayList<ArrayList<ArrayList<Double>>> fluorData;

    public void setup() {

    }

    public MultiThreadedOutputGenerator(ExecutorService exec, ArrayList<CellData> cellData,
            String parDir, boolean protMode, UserVariables uv, File childDir, ImageStack sigStack,
            ImageStack cytoStack, File directory, PointRoi roi) {
        super(null, null);
        this.cellData = cellData;
        this.parDir = parDir;
        this.protMode = protMode;
        this.uv = uv;
        this.childDir = childDir;
        this.sigStack = sigStack;
        this.cytoStack = cytoStack;
        this.directory = directory;
        this.roi = roi;
        this.fluorData = new ArrayList();
    }

    @Override
    public void run() {
        IJ.log("Building individual cell outputs...");
        double minLength = protMode ? uv.getBlebLenThresh() : uv.getMinLength();
        for (int index = 0; index < cellData.size(); index++) {
            int length = cellData.get(index).getLength();
            fluorData.add(new ArrayList());
            if (length > minLength) {
                childDir = new File(GenUtils.openResultsDirectory(String.format("%s%s%d", parDir, File.separator, index)));
                exec.submit(new RunnableOutputGenerator(cellData, parDir,
                        protMode, uv, childDir, sigStack,
                        cytoStack, index, length, directory, roi, fluorData.get(index)));
            }
        }
        terminate("Error generating outputs.");
        IJ.log("\nAll cells done.\n");
    }

    public ArrayList<ArrayList<ArrayList<Double>>> getFluorData() {
        return fluorData;
    }

}
