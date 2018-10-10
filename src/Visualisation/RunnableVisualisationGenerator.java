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

import Cell.CellData;
import Cell.MorphMap;
import IAClasses.Region;
import IO.BioFormats.BioFormatsImageWriter;
import Process.RunnableProcess;
import UserVariables.UserVariables;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class RunnableVisualisationGenerator extends RunnableProcess {

    ArrayList<CellData> cellData;
    boolean protMode;
    ImageStack cytoStack;
    UserVariables uv;
    File velDirName;
    File curvDirName;
    protected DecimalFormat numFormat;
    int t;

    public RunnableVisualisationGenerator(ArrayList<CellData> cellData, boolean protMode, ImageStack cytoStack, UserVariables uv, File velDirName, File curvDirName, DecimalFormat numFormat, int t) {
        super(null);
        this.cellData = cellData;
        this.protMode = protMode;
        this.cytoStack = cytoStack;
        this.uv = uv;
        this.velDirName = velDirName;
        this.curvDirName = curvDirName;
        this.numFormat = numFormat;
        this.t = t;
    }

    @Override
    public void run() {
        int N = cellData.size();
        double minLength = protMode ? uv.getBlebLenThresh() : uv.getMinLength();
        int width = cytoStack.getWidth();
        int height = cytoStack.getHeight();
        double mincurve = -50.0, maxcurve = 50.0;
        FloatProcessor velOutput = new FloatProcessor(width, height);
//        velOutput.setLineWidth(uv.getVisLineWidth());
        velOutput.setValue(0.0);
        velOutput.fill();
        ColorProcessor curveOutput = new ColorProcessor(width, height);
        curveOutput.setLineWidth(uv.getVisLineWidth());
        curveOutput.setColor(Color.black);
        curveOutput.fill();
        Roi[] cellLabels = new Roi[N];
        Arrays.fill(cellLabels, null);
        for (int n = 0; n < N; n++) {
            int start = cellData.get(n).getStartFrame();
            int end = cellData.get(n).getEndFrame();
            int length = cellData.get(n).getLength();
            if (length > minLength && t + 1 >= start && t < end) {
                int index = t + 1 - start;
                double[][] smoothVelocities = cellData.get(n).getSmoothVelocities();
                Region[] allRegions = cellData.get(n).getCellRegions();
                MorphMap curveMap = cellData.get(n).getCurveMap();
                int upLength = curveMap.getHeight();
                double maxvel = cellData.get(n).getMaxVel();
                double minvel = cellData.get(n).getMinVel();
                double xCoords[][] = curveMap.getxCoords();
                double yCoords[][] = curveMap.getyCoords();
                double curvatures[][] = curveMap.getzVals();
                for (int j = 0; j < upLength; j++) {
                    int x = (int) Math.round(xCoords[index][j]);
                    int y = (int) Math.round(yCoords[index][j]);
//                    velOutput.setColor(getColor(smoothVelocities[index][j], maxvel, minvel));
                    velOutput.putPixelValue(x, y, smoothVelocities[index][j]);
//                    curveOutput.setColor(getColor(curvatures[index][j], maxcurve, mincurve));
                    curveOutput.drawDot(x, y);
                }
//                velOutput.setColor(Color.blue);
                Region current = allRegions[t];
                ArrayList<float[]> centres = current.getCentres();
                int cl = centres.size();
                int xc = (int) Math.round(centres.get(cl - 1)[0]);
                int yc = (int) Math.round(centres.get(cl - 1)[1]);
//                velOutput.fillOval(xc - 1, yc - 1, 3, 3);
//                velOutput.drawString(String.valueOf(n + 1), xc + 2, yc + 2);
                cellLabels[n] = new TextRoi(xc + 2, yc + 2, String.valueOf(n + 1));
            }
        }
        String velFileName = String.format("%s%s%s.tiff", velDirName.getAbsolutePath(), File.separator, numFormat.format(t));
        String curveFileName = String.format("%s%s%s", curvDirName.getAbsolutePath(), File.separator, numFormat.format(t));
        IJ.log(String.format("Saving %s", velFileName));
        try {
            BioFormatsImageWriter.saveImage(velOutput, new File(velFileName), cellLabels);
        } catch (Exception e) {
            GenUtils.logError(e, "Failed to saved visualisation image.");
        }
//        IJ.saveAs((new ImagePlus("", velOutput)), "PNG", velFileName);
        IJ.log(String.format("Saving %s", curveFileName));
        IJ.saveAs((new ImagePlus("", curveOutput)), "PNG", curveFileName);
    }
}
