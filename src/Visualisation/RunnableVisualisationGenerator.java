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
import Process.RunnableProcess;
import UserVariables.UserVariables;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import java.awt.Color;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

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
        ColorProcessor velOutput = new ColorProcessor(width, height);
        velOutput.setLineWidth(uv.getVisLineWidth());
        velOutput.setColor(Color.black);
        velOutput.fill();
        ColorProcessor curveOutput = new ColorProcessor(width, height);
        curveOutput.setLineWidth(uv.getVisLineWidth());
        curveOutput.setColor(Color.black);
        curveOutput.fill();
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
                    velOutput.setColor(getColor(smoothVelocities[index][j], maxvel, minvel));
                    velOutput.drawDot(x, y);
                    curveOutput.setColor(getColor(curvatures[index][j], maxcurve, mincurve));
                    curveOutput.drawDot(x, y);
                }
                velOutput.setColor(Color.blue);
                Region current = allRegions[t];
                ArrayList<float[]> centres = current.getCentres();
                int cl = centres.size();
                int xc = (int) Math.round(centres.get(cl - 1)[0]);
                int yc = (int) Math.round(centres.get(cl - 1)[1]);
                velOutput.fillOval(xc - 1, yc - 1, 3, 3);
                velOutput.drawString(String.valueOf(n + 1), xc + 2, yc + 2);
            }
        }
        String velFileName = String.format("%s%s%s", velDirName.getAbsolutePath(), File.separator, numFormat.format(t));
        String curveFileName = String.format("%s%s%s", curvDirName.getAbsolutePath(), File.separator, numFormat.format(t));
        IJ.log(String.format("Saving %s", velFileName));
        IJ.saveAs((new ImagePlus("", velOutput)), "PNG", velFileName);
        IJ.log(String.format("Saving %s", curveFileName));
        IJ.saveAs((new ImagePlus("", curveOutput)), "PNG", curveFileName);
    }

    /*
     * Essentially acts as a look-up table, calculated 'on the fly'. The output
     * will range somewhere between red for retmax, green for promax and yellow
     * if val=0.
     */
    Color getColor(double val, double promax, double retmax) {
        Color colour = Color.black;
        int r, g;
        if (val >= 0.0) {
            r = 255 - (int) Math.round(255 * val / promax);
            if (r < 0) {
                r = 0;
            } else if (r > 255) {
                r = 255;
            }
            colour = new Color(r, 255, 0);
        } else if (val < 0.0) {
            g = 255 - (int) Math.round(255 * val / retmax);
            if (g < 0) {
                g = 0;
            } else if (g > 255) {
                g = 255;
            }
            colour = new Color(255, g, 0);
        }
        return colour;
    }
}
