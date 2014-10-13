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

import UtilClasses.GenUtils;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Contains static methods for extracting signal data from blebs
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class BlebAnalyser {

    /**
     * Using maps contained in cellData, currentBleb is tracked, beginning at
     * coordinates specified by currentBleb.getBounds(), and data fields within
     * currentBleb are updated accordingly.
     *
     * @param currentBleb the bleb to be tracked. An initial bounding box,
     * representing the location of currentBleb in the velocity map contained in
     * cellData, must be specified.
     * @param cellData data object containing various maps and parameters
     * pertaining to the analysis
     * @param index a index reference tag corresponding to the current bleb
     * @param stacks the movie(s) from which the bleb was isolated
     * @return true if currentBleb contains sufficient signal data to be
     * analysed, based on ((no. of pixels &lt; cellData.getSigThresh()) / (total
     * pixels) &lt; UserVariables.getSigRecoveryThresh()), false otherwise
     */
    public static boolean extractAreaSignalData(Bleb currentBleb, CellData cellData, int index, ImageStack[] stacks) {
//        ImageStack cytoStack = stacks[0];
//        ImageStack sigStack = stacks[1];
        MorphMap curveMap = cellData.getCurveMap();
        double[][] xvals = curveMap.getxCoords();
        double[][] yvals = curveMap.getyCoords();
        double[][] noisyVels = cellData.getVelMap().getzVals();
        Rectangle bounds = currentBleb.getBounds();
//        int imageWidth = cytoStack.getWidth();
//        int imageHeight = cytoStack.getHeight();
        int velMapHeight = cellData.getVelMapWithDetections().getHeight();
        int halfVelMapHeight = velMapHeight / 2;
//        ArrayList<BoundaryPixel> minPos[] = cellData.getCurvatureMinima();
        int anchor1[] = new int[2];
        anchor1[0] = GenUtils.checkRange(bounds.y, velMapHeight);
        anchor1[1] = -1;
        int anchor2[] = new int[2];
        anchor2[0] = GenUtils.checkRange(bounds.y + bounds.height - 1, velMapHeight);
        anchor2[1] = -1;
        int maxExtent = bounds.height;
//        ColorProcessor detectionSlice;
        ImageProcessor velMapImage = cellData.getGreyVelMap();
        double cutoff = UserVariables.getCutOffTime() * (UserVariables.getTimeRes() / 60.0);
        int totalCount = 0, zeroCount = 0;
        int cortexRad = (int) Math.round(UserVariables.getCortexDepth() / UserVariables.getSpatialRes());
        boolean done = false, negvel = false, posvel = false;
        for (int timeIndex = bounds.x;
                (timeIndex - bounds.x <= cutoff) && (timeIndex < velMapImage.getWidth() && !done);
                timeIndex++) {
            int searchRange = (int) Math.round(UserVariables.getCurveRange() * cellData.getScaleFactors()[timeIndex]);
            double currentMeanVel = 0.0;
            double currentProtrusionLength;
//            detectionSlice.setChannel(1, (ByteProcessor) ((new TypeConverter(cytoStack.getProcessor(timeIndex + 1), true)).convertToByte()));
//            if (sigStack != null) {
//                detectionSlice.setChannel(2, (ByteProcessor) ((new TypeConverter(sigStack.getProcessor(timeIndex + 1), true)).convertToByte()));
//            }
            ImageProcessor sigProc = stacks[1].getProcessor(timeIndex + 1);
            CurveMapAnalyser.updateAnchorPoint(timeIndex, anchor1, searchRange, cellData);
            CurveMapAnalyser.updateAnchorPoint(timeIndex, anchor2, searchRange, cellData);
            /*
             * Draw protrusion output image
             */
            int y1 = anchor1[0];
            int y2 = anchor2[0];
            if (y2 < y1) {
                y2 += velMapHeight;
            }
            if (y1 != y2 && y2 - y1 < halfVelMapHeight) {
                ArrayList<Double> thisBlebPerimSig = new ArrayList<Double>();
                if (y2 - y1 > maxExtent) {
                    maxExtent = y2 - y1;
                }
                Polygon poly = new Polygon();
                boolean zero = true;
                for (int y = y1; y < y2; y++) {
                    int posIndex = y;
                    if (posIndex >= velMapHeight) {
                        posIndex -= velMapHeight;
                    }
                    int xpos = (int) Math.round(xvals[timeIndex][posIndex]);
                    int ypos = (int) Math.round(yvals[timeIndex][posIndex]);
                    if (!UserVariables.isUsedSmoothedVels()) {
                        currentMeanVel += noisyVels[timeIndex][posIndex];
                    } else {
                        currentMeanVel += velMapImage.getPixelValue(timeIndex, posIndex);
                    }
                    double val = sigProc.getPixelValue(xpos, ypos);
                    if (val > cellData.getSigThresh()) {
                        thisBlebPerimSig.add(val);
                        zero = false;
                    } else {
                        thisBlebPerimSig.add(-1.0);
                    }
                    poly.addPoint(xpos, ypos);
                }
                ByteProcessor blebMask = drawBlebMask(poly, cortexRad, stacks[0].getWidth(), stacks[0].getHeight(), 255, 0);
                Rectangle polyBounds = poly.getBounds();
                double sum = 0.0;
                for (int y = polyBounds.y - cortexRad; y < polyBounds.y + polyBounds.height + cortexRad; y++) {
                    for (int x = polyBounds.x - cortexRad; x < polyBounds.x + polyBounds.width + cortexRad; x++) {
                        if (blebMask.getPixel(x, y) > 0) {
                            double val = sigProc.getPixelValue(x, y);
                            if (val > cellData.getSigThresh()) {
                                sum += val;
                            }
                        }
                    }
                }
                currentBleb.getSumSig().add(sum);
                if (zero) {
                    zeroCount++;
                }
                totalCount++;
                currentBleb.getPolys().add(poly);
                currentProtrusionLength = anchor2[0] - anchor1[0];
                if (anchor2[0] < anchor1[0]) {
                    currentProtrusionLength += velMapImage.getHeight();
                }
                currentProtrusionLength *= UserVariables.getSpatialRes() / cellData.getScaleFactors()[timeIndex];
//                currentProtrusion.getDetectionStack().addSlice(detectionSlice);
                currentBleb.getBlebPerimSigs().add(thisBlebPerimSig);
                currentBleb.getMeanVel().add(currentMeanVel);
                currentBleb.getProtrusionLength().add(currentProtrusionLength);
                if (UserVariables.isVelDetect()) {
                    if (!(negvel && currentMeanVel >= 0.0)) {
                        if (posvel && currentMeanVel < 0.0) {
                            negvel = true;
                        } else if (currentMeanVel >= 0.0) {
                            posvel = true;
                        }
                    } else {
                        done = true;
                    }
                }
            } else {
                done = true;
            }
        }
        currentBleb.setMaxExtent(maxExtent);
        return (((double) zeroCount) / totalCount < UserVariables.getSigRecoveryThresh());
    }

    /**
     * Produces a signal map of an individual bleb
     *
     * @param currentBleb the bleb to be mapped
     * @param spatialRes the spatial resolution of the movie from which the bleb
     * was derived
     * @param useSigThresh should be set to true if currentBleb's signal values
     * have been thresholded
     * @return signal map of currentBleb in the form of a FloatProcessor
     */
    public static ImageProcessor drawBlebSigMap(Bleb currentBleb, double spatialRes, boolean useSigThresh) {
        int height = currentBleb.getMaxExtent();
        int blebDur = currentBleb.getBlebPerimSigs().size();
        FloatProcessor blebSigImage = new FloatProcessor(blebDur, height);
        blebSigImage.setValue(-1.0);
        blebSigImage.fill();
        for (int j = 0; j < blebDur; j++) {
            ArrayList<Double> thisbleb = currentBleb.getBlebPerimSigs().get(j);
            int offset = (blebSigImage.getHeight() - thisbleb.size()) / 2;
//            double sum = 0.0;
//            int indexsum = 0;
//            int indexcount = 0;
            for (int k = 0; k < thisbleb.size(); k++) {
                double val = thisbleb.get(k);
                if (!useSigThresh) {
//                    sum += val;
//                    indexcount++;
                    blebSigImage.putPixelValue(j, offset + k, val);
                } else {
                    blebSigImage.putPixelValue(j, offset + k, 0.0);
                    if (val >= 0.0) {
//                        sum += val;
//                        indexsum += k;
//                        indexcount++;
//                        if (!average) {
                        blebSigImage.putPixelValue(j, offset + k, val);
//                        }
                    }
                }
            }
//            if (average && indexcount > 0) {
//                blebSigImage.putPixelValue(j, offset + indexsum / indexcount, sum / indexcount);
//            }
        }
        return blebSigImage;
    }

//    public static void drawBlebMovie(Bleb currentProtrusion, double spatialRes, boolean useSigThresh) {
//        int height = currentProtrusion.getMaxExtent();
//        int blebDur = currentProtrusion.getBlebPerimSigs().size();
//        FloatProcessor blebSigImage = new FloatProcessor(blebDur, height);
//        blebSigImage.setValue(-1.0);
//        blebSigImage.fill();
//        double max = 30000;
//        ByteProcessor blebPlotImage = new ByteProcessor(blebDur, blebDur);
//        blebPlotImage.setValue(0.0);
//        blebPlotImage.fill();
//        for (int j = 0; j < blebDur; j++) {
//            ArrayList<Double> thisbleb = currentProtrusion.getBlebPerimSigs().get(j);
//            int offset = (blebSigImage.getHeight() - thisbleb.size()) / 2;
//            double sum = 0.0;
//            for (int k = 0; k < thisbleb.size(); k++) {
//                double val = thisbleb.get(k);
//                if (!useSigThresh) {
//                    sum += val;
//                    blebSigImage.putPixelValue(j, offset + k, val);
//                } else {
//                    blebSigImage.putPixelValue(j, offset + k, 0.0);
//                    if (val >= 0.0) {
//                        sum += val;
//                    }
//                }
//            }
//            blebPlotImage.setLineWidth(5);
//            blebPlotImage.setValue(255.0);
//            blebPlotImage.drawDot(j, blebDur - (int) Math.round(sum * blebDur / max));
//            IJ.saveAs((new ImagePlus("", blebSigImage)), "TIF", "C:\\Users\\barry05\\Desktop\\BlebSigMovie\\slice_" + j);
//            IJ.saveAs((new ImagePlus("", blebPlotImage)), "PNG", "C:\\Users\\barry05\\Desktop\\BlebPlotMovie\\slice_" + j);
//        }
//    }
    /**
     * Draw a mask image of the specified {@link java.awt.Polygon Polygon}
     * representation of a bleb
     *
     * @param poly the {@link java.awt.Polygon Polygon} to be drawn
     * @param radius the thickness of the line to be used
     * @param width the width of the output mask
     * @param height the height of the output mask
     * @param fg the foreground color to be used
     * @param bg the background color to be used
     * @return a {@link ij.process.ByteProcessor ByteProcessor} mask image of
     * the specified polygon
     */
    public static ByteProcessor drawBlebMask(Polygon poly, int radius, int width, int height, int fg, int bg) {
        ByteProcessor mask = new ByteProcessor(width, height);
        mask.setColor(bg);
        mask.fill();
        mask.setColor(fg);
        int diam = radius * 2 + 1;
        int N = poly.npoints;
        int xpoints[] = poly.xpoints;
        int ypoints[] = poly.ypoints;
        for (int n = 0; n < N; n++) {
            mask.drawOval(xpoints[n] - radius, ypoints[n] - radius, diam, diam);
        }
        return mask;
    }
}
