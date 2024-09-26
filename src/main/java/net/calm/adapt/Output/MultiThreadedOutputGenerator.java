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
package net.calm.adapt.Output;

import ij.IJ;
import ij.ImageStack;
import ij.gui.PointRoi;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.IO.BioFormats.BioFormatsImg;
import net.calm.iaclasslibrary.IO.BioFormats.LocationAgnosticBioFormatsImg;
import net.calm.iaclasslibrary.Process.MultiThreadedProcess;
import net.calm.iaclasslibrary.UserVariables.UserVariables;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public MultiThreadedOutputGenerator() {
        this(null, null, null, false, null, null, null, null, null, null);
    }

    public void setup(BioFormatsImg img, Properties props, String[] propLabels) {

    }

    public MultiThreadedOutputGenerator(ExecutorService exec, ArrayList<CellData> cellData,
            String parDir, boolean protMode, UserVariables uv, File childDir, ImageStack sigStack,
            ImageStack cytoStack, File directory, PointRoi roi) {
        super(null);
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
        this.exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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

    public MultiThreadedOutputGenerator duplicate() {
        MultiThreadedOutputGenerator newProcess = new MultiThreadedOutputGenerator();
        this.updateOutputDests(newProcess);
        return newProcess;
    }

    public void setup(LocationAgnosticBioFormatsImg var1, Properties var2, String[] var3){

    }
}
