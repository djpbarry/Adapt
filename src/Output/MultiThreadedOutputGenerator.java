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
import ij.ImageStack;
import ij.gui.PointRoi;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author David Barry <david.barry at crick dot ac dot uk>
 */
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

    public MultiThreadedOutputGenerator(ExecutorService exec, ArrayList<CellData> cellData,
            String parDir, boolean protMode, UserVariables uv, File childDir, ImageStack sigStack,
            ImageStack cytoStack, File directory, PointRoi roi) {
        super(null, exec);
        this.cellData = cellData;
        this.parDir = parDir;
        this.protMode = protMode;
        this.uv = uv;
        this.childDir = childDir;
        this.sigStack = sigStack;
        this.cytoStack = cytoStack;
        this.directory = directory;
        this.roi = roi;
    }

    @Override
    public void run() {
        double minLength = protMode ? uv.getBlebLenThresh() : uv.getMinLength();
        for (int index = 0; index < cellData.size(); index++) {
            String childDirName = GenUtils.openResultsDirectory(parDir + File.separator + index);
            int length = cellData.get(index).getLength();
            if (length > minLength) {
                childDir = new File(childDirName);
                exec.submit(new RunnableOutputGenerator(cellData, parDir,
                        protMode, uv, childDir, sigStack,
                        cytoStack, index, length, directory, roi));
            }
        }
        terminate("Error generating outputs.");
    }
}
