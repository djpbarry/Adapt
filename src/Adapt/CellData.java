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

import IAClasses.BoundaryPixel;
import IAClasses.Region;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.util.ArrayList;

/**
 * Container class for data pertaining to cellular analysis
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class CellData {

    private MorphMap velMap, curveMap, sigMap;
    private ImageProcessor velMapWithDetections;
    private ColorProcessor colorVelMap;
    private double sigThresh, minVel, maxVel;
    private FloatProcessor greyVelMap, greySigMap, greyCurveMap;
    private Roi[] velRois;
    private ArrayList<BoundaryPixel> curvatureMinima[], curvatureMaxima[];
    private double noisyVelocities[][];
    private double[][] smoothVelocities;
    private double scaleFactors[];
    private int greyThresholds[];
    private int length;
    private Region[] cellRegions;
    private Region initialRegion;
    private int imageWidth, imageHeight;

    /**
     * Get segmented cell regions derived from all input movie frames
     *
     * @return an array of {@link IAClasses.Region Regions}, where the array
     * index denotes frame number
     */
    public Region[] getCellRegions() {
        return cellRegions;
    }

    public void setCellRegions(Region[] cellRegions) {
        this.cellRegions = cellRegions;
    }

    /**
     * Get the curvature map for this cell
     *
     * @return cell curvature map in the form of a {@link ij.process.FloatProcessor
     * FloatProcessor}
     */
    public FloatProcessor getGreyCurveMap() {
        return greyCurveMap;
    }

    public void setGreyCurveMap(FloatProcessor greyCurveMap) {
        this.greyCurveMap = greyCurveMap;
    }

    /**
     * The maximum membrane velocity attained by this cell
     *
     * @return maximum membrane velocity
     */
    public double getMaxVel() {
        return maxVel;
    }

    public void setMaxVel(double maxVel) {
        this.maxVel = maxVel;
    }

    /**
     * The minimum membrane velocity attained by this cell
     *
     * @return minimum membrane velocity
     */
    public double getMinVel() {
        return minVel;
    }

    public void setMinVel(double minVel) {
        this.minVel = minVel;
    }

    /**
     * Returns an array of values corresponding to the degree of up-sampling
     * applied to each column (each movie frame) of {@link MorphMap MorphMaps}
     * for this cell
     *
     * @return array of values corresponding to scale values
     */
    public double[] getScaleFactors() {
        return scaleFactors;
    }

    public void setScaleFactors(double[] scaleFactors) {
        this.scaleFactors = scaleFactors;
    }

    /**
     * Get a list of curvature minima detected in this cell's curvature map
     *
     * @return a collection of pixels representing curvature minima, where the
     * array index denotes movie frame number
     */
    public ArrayList<BoundaryPixel>[] getCurvatureMinima() {
        return curvatureMinima;
    }

    public void setCurvatureMinima(ArrayList<BoundaryPixel>[] curvatureMinima) {
        this.curvatureMinima = curvatureMinima;
    }

    /**
     * Get a list of Rois representing the regions in this cell's velocity map
     * detected as blebs
     *
     * @return array of Rois representing bleb detections
     */
    public Roi[] getVelRois() {
        return velRois;
    }

    public void setVelRois(Roi[] velRois) {
        this.velRois = velRois;
    }

    /**
     * Get this cell's signal map as a FloatProcessor
     *
     * @return FloatProcessor representation of this cell's signal map
     */
    public FloatProcessor getGreySigMap() {
        return greySigMap;
    }

    public void setGreySigMap(FloatProcessor greySigMap) {
        this.greySigMap = greySigMap;
    }

    /**
     * Get this cell's velocity map as a FloatProcessor
     *
     * @return FloatProcessor representation of this cell's velocity map
     */
    public FloatProcessor getGreyVelMap() {
        return greyVelMap;
    }

    public void setGreyVelMap(FloatProcessor greyVelMap) {
        this.greyVelMap = greyVelMap;
    }

    /**
     * Get this cell's signal map
     *
     * @return cell signal map as a MorphMap
     */
    public MorphMap getSigMap() {
        return sigMap;
    }

    public void setSigMap(MorphMap sigMap) {
        this.sigMap = sigMap;
    }

    /**
     * Get this cell's velocity map
     *
     * @return cell velocity map as a MorphMap
     */
    public MorphMap getVelMap() {
        return velMap;
    }

    public void setVelMap(MorphMap velMap) {
        this.velMap = velMap;
    }

    /**
     * Default constructor
     */
    public CellData() {
    }

    /**
     * Get this cell's curvature map
     *
     * @return cell curvature map as a MorphMap
     */
    public MorphMap getCurveMap() {
        return curveMap;
    }

    public void setCurveMap(MorphMap curveMap) {
        this.curveMap = curveMap;
    }

    /**
     * Get the threshold value applied to this cell's signal data
     *
     * @return signal threshold, above which signal pixel values must be to be
     * included in analysis
     */
    public double getSigThresh() {
        return sigThresh;
    }

    public void setSigThresh(double sigThresh) {
        this.sigThresh = sigThresh;
    }

    /**
     * Get image of this cell's velocity map with bleb detections super-imposed
     *
     * @return image of this cell's velocity map with bleb regions superimposed
     */
    public ImageProcessor getVelMapWithDetections() {
        return velMapWithDetections;
    }

    public void setVelMapWithDetections(ImageProcessor velMapWithDetections) {
        this.velMapWithDetections = velMapWithDetections;
    }

    /**
     * Get an array of the threshold values calculated for each frame of the
     * cytosolic input
     *
     * @return an array of threshold values
     */
    public int[] getGreyThresholds() {
        return greyThresholds;
    }

    public void setGreyThresholds(int[] greyThresholds) {
        this.greyThresholds = greyThresholds;
    }

//    public ArrayList<Pixel> getInitialPix() {
//        return initialPix;
//    }
//
//    public void setInitialPix(ArrayList<Pixel> initialPix) {
//        this.initialPix = initialPix;
//    }

    public ColorProcessor getColorVelMap() {
        return colorVelMap;
    }

    public void setColorVelMap(ColorProcessor colorVelMap) {
        this.colorVelMap = colorVelMap;
    }

    public double[][] getNoisyVelocities() {
        return noisyVelocities;
    }

    public void setNoisyVelocities(double[][] noisyVelocities) {
        this.noisyVelocities = noisyVelocities;
    }

    public double[][] getSmoothVelocities() {
        return smoothVelocities;
    }

    public void setSmoothVelocities(double[][] smoothVelocities) {
        this.smoothVelocities = smoothVelocities;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Region getInitialRegion() {
        return initialRegion;
    }

    public void setInitialRegion(Region initialRegion) {
        this.initialRegion = initialRegion;
    }

    public ArrayList<BoundaryPixel>[] getCurvatureMaxima() {
        return curvatureMaxima;
    }

    public void setCurvatureMaxima(ArrayList<BoundaryPixel>[] curvatureMaxima) {
        this.curvatureMaxima = curvatureMaxima;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }
    
}
