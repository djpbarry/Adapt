/* 
 * Copyright (C) 2014 David Barry <david.barry at cancer.org.uk>
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
package Adapt;

import UtilClasses.Utilities;
import IAClasses.BoundaryPixel;
import IAClasses.IsoGaussian;
import IAClasses.Utils;
import ParticleTracking.Particle;
import ParticleTracking.ParticleArray;
import ParticleTracking.ParticleTrajectory;
import ParticleTracking.Timelapse_Analysis;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.awt.Color;
import java.util.ArrayList;

/**
 * Class consisting of static methods for the analysis of cell curvature maps
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class CurveMapAnalyser {

    private final static int curveSearchRangeFactor = 4;
    private final static double maxTrajScore = 5.0;

    /**
     * Determines whether the coordinate (pos, timePoint) represents a local
     * minima within curveVals
     *
     * @param pos y-coordinate of coordinate to be tested, representing position
     * on cell boundary
     * @param range the size of the window within curveVals that will be
     * searched
     * @param timePoint the x-coordinate of coordinate to be tested,
     * representing frame number of original movie sequence
     * @param threshold curvature at a point must be below this value in order
     * to be considered a minima
     * @param curveVals array of curvature values to be searched
     * @return 0 if this is a local curvature minima, non-zero otherwise
     *
     */
    private static int isLocalCurvatureExtreme(int pos, int range, double[] curveVals, double threshold, boolean minima) {
        int factor = minima ? 1 : -1;
        double C0 = factor * curveVals[pos];
        double C1 = 0.0, C2 = 0.0;
        int length = curveVals.length;
        double min = Double.MAX_VALUE;
        for (int i = pos - range; i < pos; i++) {
            int a = Utilities.checkRange(i, 0, length);
            double c = factor * curveVals[a];
            C1 += c;
            if (c < min) {
                min = c;
            }
        }
        C1 /= range;
        for (int j = pos + 1; j <= pos + range; j++) {
            int b = Utilities.checkRange(j, 0, length);
            double c = factor * curveVals[b];
            C2 += c;
            if (c < min) {
                min = c;
            }
        }
        C2 /= range;
        if (C0 < min && C0 < C1 && C0 < C2 && C0 < -threshold) {
            return 0;
        } else if (C2 < C1) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Finds all curvature minima in the cellData's curveMap. Minima can be
     * retrieved by calling
     * {@link CellData#getCurvatureMinima() CellData.getCurvatureMinima()}
     *
     * @param cellData contains the curvature map to be analysed
     * @param startFrame the first movie frame to be analysed
     * @param endFrame the last movie frame to be analysed
     * @param minDuration the minimum duration (in seconds) for which a minima
     * must exist in order to be stored
     */
    public static ArrayList<BoundaryPixel>[] findAllCurvatureExtrema(CellData cellData, int startFrame, int endFrame, double minDuration, boolean min, double threshold, double curveRange) {
        MorphMap curveMap = cellData.getCurveMap();
        double[][] curveVals = curveMap.smoothMap(1.0, 1.0);
        double[][] xvals = curveMap.getxCoords();
        double[][] yvals = curveMap.getyCoords();
        int posLength = curveVals[0].length;
        int tLength = 1 + endFrame - startFrame;
        ParticleArray extrema = new ParticleArray(tLength);
        for (int t = startFrame; t <= endFrame; t++) {
            int currentIndex = t - startFrame;
            int range = calcScaledCurveRange(curveRange, cellData.getScaleFactors()[currentIndex]);
            for (int pos = 0; pos < posLength; pos++) {
                if (CurveMapAnalyser.isLocalCurvatureExtreme(pos, range, curveVals[currentIndex], threshold, min) == 0) {
                    extrema.addDetection(currentIndex,
                            new Particle(t,
                                    new IsoGaussian(xvals[currentIndex][pos]
                                            * UserVariables.getSpatialRes(),
                                            yvals[currentIndex][pos]
                                            * UserVariables.getSpatialRes(),
                                            -1.0 * curveVals[currentIndex][pos],
                                            1.0, 1.0, 1.0),
                                    null, null, pos));
                }
            }
        }
        Timelapse_Analysis ta = new Timelapse_Analysis();
        ta.updateTrajectories(extrema, UserVariables.getTimeRes(), maxTrajScore, UserVariables.getSpatialRes(), false);
        ArrayList<ParticleTrajectory> trajectories = ta.getTrajectories();
        ArrayList<BoundaryPixel> extPos[] = new ArrayList[tLength];
        int tSize = trajectories.size();
        for (int j = 0; j < tSize; j++) {
            ParticleTrajectory currentTraj = trajectories.get(j);
            int cSize = currentTraj.getSize();
            if (!(cSize < minDuration)) {
                Particle currentParticle = currentTraj.getEnd();
                int lastFrame = currentParticle.getTimePoint() + 1;
                while (currentParticle != null) {
                    int frame = currentParticle.getTimePoint();
                    for (int k = lastFrame - 1; k >= frame; k--) {
                        int currentIndex = k - startFrame;
                        if (extPos[currentIndex] == null) {
                            extPos[currentIndex] = new ArrayList<BoundaryPixel>();
                        }
                        BoundaryPixel currentPos = new BoundaryPixel((int) currentParticle.getX(),
                                (int) currentParticle.getY(), currentParticle.getiD(), j, k);
                        extPos[currentIndex].add(currentPos);
                    }
                    currentParticle = currentParticle.getLink();
                    lastFrame = frame;
                }
            }
        }
//        cellData.setCurvatureMinima(minPos);
        return extPos;
    }

    public static int calcScaledCurveRange(double curveRange, double scaleFactor) {
        return (int) Math.round(curveRange * scaleFactor * curveSearchRangeFactor);
    }

    public static void drawAllExtrema(CellData cellData, double timeRes, double spatialRes, ImageStack cytoStack, int startFrame, int endFrame, double minDuration) {
        ImageStack detectionStack = new ImageStack(cytoStack.getWidth(), cytoStack.getHeight());
        int tLength = 1 + endFrame - startFrame;
        MorphMap curveMap = cellData.getCurveMap();
        double[][] xvals = curveMap.getxCoords();
        double[][] yvals = curveMap.getyCoords();
        ArrayList<BoundaryPixel> minPos[] = cellData.getCurvatureMinima();
        ArrayList<BoundaryPixel> maxPos[] = cellData.getCurvatureMaxima();
        for (int i = 0; i < tLength; i++) {
            ImageProcessor detectionSlice = (new TypeConverter(cytoStack.getProcessor(i + 1), true)).convertToRGB();
            detectionSlice.setLineWidth(8);
            if (minPos[i] != null) {
                int mpSize = minPos[i].size();
                detectionSlice.setColor(Color.yellow);
                for (int j = 0; j < mpSize; j++) {
                    BoundaryPixel currentMin = minPos[i].get(j);
                    int pos = currentMin.getPos();
                    int x = (int) Math.round(xvals[i][pos]);
                    int y = (int) Math.round(yvals[i][pos]);
                    detectionSlice.drawString("" + currentMin.getID(), x, y);
                }
            }
            if (maxPos[i] != null) {
                int mpSize = maxPos[i].size();
                detectionSlice.setColor(Color.magenta);
                for (int j = 0; j < mpSize; j++) {
                    BoundaryPixel currentMax = maxPos[i].get(j);
                    int pos = currentMax.getPos();
                    int x = (int) Math.round(xvals[i][pos]);
                    int y = (int) Math.round(yvals[i][pos]);
                    detectionSlice.drawString("" + currentMax.getID(), x, y);
                }
            }
            detectionStack.addSlice("", detectionSlice);
        }
        IJ.saveAs(new ImagePlus("", detectionStack), "TIF", "C:/users/barry05/desktop/AllDetections.tif");
    }

    private static void findNearestMinTraj(int time, int anchor[], int maxRange, CellData cellData) {
        ArrayList<BoundaryPixel> minPos[] = cellData.getCurvatureMinima();
        MorphMap curveMap = cellData.getCurveMap();
        if (minPos[time] == null) {
            return;
        }
        int size = minPos[time].size();
        double minDist = Double.MAX_VALUE;
        double xvals[] = curveMap.getxCoords()[time];
        double yvals[] = curveMap.getyCoords()[time];
        double x1 = xvals[anchor[0]];
        double y1 = yvals[anchor[0]];
        for (int i = 0; i < size; i++) {
            int thisPos = minPos[time].get(i).getPos();
            double x2 = xvals[thisPos];
            double y2 = yvals[thisPos];
            double dist = Utils.calcDistance(x1, y1, x2, y2);
            if (dist < minDist) {
                minDist = dist;
                if (dist < maxRange) {
                    anchor[1] = minPos[time].get(i).getID();
                }
            }
        }
    }

    private static void findMinTrajPos(ArrayList<BoundaryPixel>[] minPos, int time, int anchor[], int maxRange) {
        if (!(anchor[1] > -1)) {
            return;
        }
        ArrayList<BoundaryPixel> poss = minPos[time];
        if (poss != null) {
            int size = poss.size();
            for (int i = 0; i < size; i++) {
                if (poss.get(i).getID() == anchor[1]) {
                    anchor[0] = poss.get(i).getPos();
                    return;
                }
            }
        }
    }

    /**
     * Updates the location of the specified bleb anchor point
     *
     * @param time the frame number at which the update is applied
     * @param anchor the anchor point to be updated, specified as {position on
     * cell boundary, curvature minimum trajectory index}
     * @param maxRange the maximum range, in pixels, over which the search for a
     * curvature minima should be conducted
     * @param cellData contains relevant curvature map and curvature minima
     * locations
     */
    public static void updateAnchorPoint(int time, int anchor[], int maxRange, CellData cellData) {
        ArrayList<BoundaryPixel> minPos[] = cellData.getCurvatureMinima();
        findTraj(minPos, time, anchor);
        if (!(anchor[1] > -1)) {
            findNearestMinTraj(time, anchor, maxRange, cellData);
        }
        findMinTrajPos(minPos, time, anchor, maxRange);
    }

    private static void findTraj(ArrayList<BoundaryPixel>[] minPos, int time, int anchor[]) {
        int length = minPos.length;
        for (int t = time; t < length; t++) {
            if (minPos[t] != null) {
                int size = minPos[t].size();
                for (int i = 0; i < size; i++) {
                    if (minPos[t].get(i).getID() == anchor[1]) {
                        return;
                    }
                }
            }
        }
        anchor[1] = -1;
    }

    /**
     * Returns the separation (in pixels) between two map indices. Index pos2 is
     * always considered to be further 'south' than pos1, so an offset,
     * mapLength, is added if pos2 is less than pos1.
     *
     * @param pos1
     * @param pos2
     * @param mapLength
     * @return
     */
//    public static int getSeparation(int pos1, int pos2, int mapLength) {
//        if (pos2 > pos1) {
//            return pos2 - pos1;
//        } else {
//            return pos2 + mapLength - pos1;
//        }
//    }
//    public static void findAllBlebs(ArrayList<Bleb> blebs, CellData cellData) {
//        ArrayList<BoundaryPixel> minPos[] = cellData.getCurvatureMinima();
//        int frames = minPos.length;
//        for (int f = 0; f < frames; f++) {
//            if (minPos[f] != null) {
//                int M = minPos[f].size();
//                BoundaryPixel currentMin[] = new BoundaryPixel[M];
//                currentMin = minPos[f].toArray(currentMin);
//                Arrays.sort(currentMin);
//                for (int m = 0; m < M; m++) {
//                    int i = m;
//                    int j = m + 1;
//                    if (j >= M) {
//                        j -= M;
//                    }
//                    int startPos[] = new int[2];
//                    startPos[0] = currentMin[i].getPos();
//                    startPos[1] = f;
//                    int endPos[] = new int[2];
//                    endPos[0] = currentMin[j].getPos();
//                    endPos[1] = f;
//                    Bleb newBleb = new Bleb();
//                    newBleb.addStartPos(startPos);
//                    newBleb.addEndPos(endPos);
//                    int index = blebs.indexOf(newBleb);
//                    if (!(index > -1)) {
//                        blebs.add(newBleb);
//                    } else {
//                        Bleb bleb = blebs.get(index);
//                        bleb.addStartPos(startPos);
//                        bleb.addEndPos(endPos);
//                    }
//                }
//            }
//        }
//    }
//    public static ImageStack drawAllBlebs(CellData cellData, ArrayList<Bleb> blebs, ImageStack cytoStack) {
//        ImageStack detectionStack = new ImageStack(cytoStack.getWidth(),
//                cytoStack.getHeight());
//        MorphMap curveMap = cellData.getCurveMap();
//        double[][] xCoords = curveMap.getxCoords();
//        double[][] yCoords = curveMap.getyCoords();
//        int size = blebs.size();
//        int stackSize = cytoStack.getSize();
//        Random rand = new Random();
//        for (int i = 0; i < stackSize; i++) {
//            detectionStack.addSlice(new ColorProcessor(cytoStack.getWidth(), cytoStack.getHeight()));
//        }
//        for (int j = 0; j < size; j++) {
//            Bleb currentBleb = blebs.get(j);
//            ArrayList<int[]> starts = currentBleb.getStartpos();
//            ArrayList<int[]> ends = currentBleb.getEndpos();
//            if (starts != null && ends != null) {
//                Color thiscolor = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
//                int blebsize = starts.size();
//                for (int f = 0; f < blebsize; f++) {
//                    int timeIndex = starts.get(f)[1];
//                    ImageProcessor slice = detectionStack.getProcessor(timeIndex + 1);
//                    slice.setColor(thiscolor);
//                    slice.setLineWidth(3);
////                    double sumX = 0.0;
////                    double sumY = 0.0;
//                    int y1 = starts.get(f)[0];
//                    int y2 = ends.get(f)[0];
//                    if (y2 < y1) {
//                        y2 += curveMap.getHeight();
//                    }
//                    for (int yIndex = y1; yIndex <= y2; yIndex++) {
//                        int ypos = yIndex;
//                        if (ypos >= curveMap.getHeight()) {
//                            ypos -= curveMap.getHeight();
//                        }
//                        int x = (int) Math.round(xCoords[timeIndex][ypos]);
//                        int y = (int) Math.round(yCoords[timeIndex][ypos]);
////                        sumX += x;
////                        sumY += y;
//                        slice.drawPixel(x, y);
//                    }
////                    slice.setColor(Color.white);
////                    slice.drawString("" + j, (int) Math.round(sumX / (y2 - y1)),
////                            (int) Math.round(sumY / (y2 - y1)), thiscolor);
//                }
//            }
//        }
//        return detectionStack;
//    }
}
