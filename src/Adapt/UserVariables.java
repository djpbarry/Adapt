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

import ij.process.AutoThresholder;

/**
 *
 */
public class UserVariables {

    private double greyThresh = 0.95; // user-specified grey-level threshold used in conditional region dilation
    private boolean genVis = true;
    private double blebDurThresh = 0.0; // minimum duration (in frames) of bleb to be considered in analysis
    private int curveRange = 10;
    private boolean useSigThresh = false;
    private double spatialRes = 1.5; //timeRes in frames/minute, spatialRes in microns/pixel;
    private double cutOffTime = 120.0;
    private double cortexDepth = 1.0;
    private boolean autoThreshold = true; // User-specifed threshold is used if set to false
    private double tempFiltRad = 5.0; // radius of Gaussian filters used to smooth velocity and signal maps
    private double sigThreshFact = 0.0;
    private double spatFiltRad = 5.0; // radius of Gaussian filters used to smooth velocity and signal maps
    private int erosion = 4;
    private boolean getMorph = true;
    private double timeRes = 0.2; // temporal resolution in frames per minute
    private double blebLenThresh = 0.0; // minimum normalised length of bleb to be considered in analysis
    private double minCurveThresh = 0.0;
//    private double maxCurveThresh = 0.0;
    private boolean analyseProtrusions = false;
    private double sigRecoveryThresh = 0.25;
    private double gaussRad = 1.0;
    private final boolean usedSmoothedVels = true;
    private final int ovalRadius = 2;
    private boolean simple = true;
    private double lambda = 10000.0; // parameter used in construction of Voronoi manifolds. See Jones et al., 2005: dx.doi.org/10.1007/11569541_54
    private int minLength = 0;
    private String threshMethod = AutoThresholder.Method.Triangle.toString();
    private boolean blebDetect = false;
    private double filoSize = 4;
    private boolean getFluorDist = false;
    private double morphSizeMin = 500.0;
    private int visLineWidth = 1;
    private boolean displayPlots=false;

    /**
     * Get the radius of circles drawn on bleb detection output movies
     *
     * @return radius of circles
     */
    public int getOvalRadius() {
        return ovalRadius;
    }

    /**
     * Returns true if velocity values are smoothed prior to being output
     *
     * @return true if velocity values are filtered
     */
    public boolean isUsedSmoothedVels() {
        return usedSmoothedVels;
    }

    /**
     * Get the minimum duration of a bleb to be considered in the analysis
     *
     * @return minimum bleb duration in frames
     */
    public double getBlebDurThresh() {
        return blebDurThresh;
    }

    public void setBlebDurThresh(double blebDurThresh) {
        this.blebDurThresh = blebDurThresh;
    }

    public void setBlebLenThresh(double blebLenThresh) {
        this.blebLenThresh = blebLenThresh;
    }

    /**
     * Get the minimum length of a bleb to be considered in the analysis
     *
     * @return minimum bleb length, expressed as (min length)/(total cell
     * length)
     */
    public double getBlebLenThresh() {
        return blebLenThresh;
    }

    /**
     * Get grey level threshold used for manual image thresholding
     *
     * @return grey level segmentation threshold
     */
    public double getGreyThresh() {
        return greyThresh;
    }

    /**
     * Set grey level threshold used for manual image thresholding
     *
     * @param greyThresh value of threshold to be used
     */
    public void setGreyThresh(double greyThresh) {
        this.greyThresh = greyThresh;
    }

    /**
     * Returns true if visualisations are to be generated
     *
     * @return true if visualisations are to be generated
     */
    public boolean isGenVis() {
        return genVis;
    }

    /**
     * Set to true if visualisations are to be generated
     *
     * @param genVis true to generate visualisations
     */
    public void setGenVis(boolean genVis) {
        this.genVis = genVis;
    }

    /**
     * Get the pixel window over which curvature minima are searched for in cell
     * curvature maps
     *
     * @return the width of the window in pixels
     */
    public int getCurveRange() {
        return curveRange;
    }

    /**
     * Set the pixel window over which curvature minima are searched for in cell
     * curvature maps
     *
     * @param curveRange the width of the window in pixels
     */
    public void setCurveRange(int curveRange) {
        this.curveRange = curveRange;
    }

    /**
     * Returns true if a signal pixel values must be above a threshold to be
     * included in analysis
     *
     * @return true if signal threshold is used
     */
    public boolean isUseSigThresh() {
        return useSigThresh;
    }

    /**
     * Set to true if a signal pixel values must be above a threshold to be
     * included in analysis
     *
     * @param useSigThresh true if signal threshold is used
     */
    public void setUseSigThresh(boolean useSigThresh) {
        this.useSigThresh = useSigThresh;
    }

    /**
     * Get spatial resolution
     *
     * @return spatial resolution in microns/pixel
     */
    public double getSpatialRes() {
        return spatialRes;
    }

    /**
     * Set spatial resolution
     *
     * @param spatialRes spatial resolution in microns/pixel
     */
    public void setSpatialRes(double spatialRes) {
        this.spatialRes = spatialRes;
    }

    /**
     * Get the maximum length of time a bleb will be tracked
     *
     * @return maximum tracking duration in seconds
     */
    public double getCutOffTime() {
        return cutOffTime;
    }

    /**
     * Set the maximum length of time a bleb will be tracked
     *
     * @param cutOffTime maximum tracking duration in seconds
     */
    public void setCutOffTime(double cutOffTime) {
        this.cutOffTime = cutOffTime;
    }

    /**
     * Get the depth below cell surface that signal pixels will be summed over
     *
     * @return cortex depth in microns
     */
    public double getCortexDepth() {
        return cortexDepth;
    }

    /**
     * Set the depth below cell surface that signal pixels will be summed over
     *
     * @param cortexDepth cortex depth in microns
     */
    public void setCortexDepth(double cortexDepth) {
        this.cortexDepth = cortexDepth;
    }

    /**
     * Returns true if images are to be auto-thresholded
     *
     * @return true if auto-thresholding is used
     */
    public boolean isAutoThreshold() {
        return autoThreshold;
    }

    /**
     * Set to true if auto-thresholding of images is to be used
     *
     * @param autoThreshold true if auto-thresholding is to be used
     */
    public void setAutoThreshold(boolean autoThreshold) {
        this.autoThreshold = autoThreshold;
    }

    /**
     * Get the radius of the smoothing filter applied to
     * {@link MorphMap MorphMaps} in the temporal dimension
     *
     * @return temporal radius of smoothing filter in seconds
     */
    public double getTempFiltRad() {
        return tempFiltRad;
    }

    /**
     * Set the radius of the smoothing filter applied to
     * {@link MorphMap MorphMaps} in the temporal dimension
     *
     * @param tempFiltRad temporal radius of smoothing filter in seconds
     */
    public void setTempFiltRad(double tempFiltRad) {
        this.tempFiltRad = tempFiltRad;
    }

    /**
     * Get the signal threshold factor. The signal threshold is calculated as
     * (mean signal value in signal map) + (signal threshold factor) x (standard
     * deviation of values in signal map)
     *
     * @return signal threshold factor
     */
    public double getSigThreshFact() {
        return sigThreshFact;
    }

    /**
     * Set the signal threshold factor. The signal threshold is calculated as
     * (mean signal value in signal map) + (signal threshold factor) x (standard
     * deviation of values in signal map)
     *
     * @param sigThreshFact signal threshold factor
     */
    public void setSigThreshFact(double sigThreshFact) {
        this.sigThreshFact = sigThreshFact;
    }

    /**
     * Get the radius of the smoothing filter applied to
     * {@link MorphMap MorphMaps} in the spatial dimension
     *
     * @return spatial radius of smoothing filter in seconds
     */
    public double getSpatFiltRad() {
        return spatFiltRad;
    }

    /**
     * Get the radius of the smoothing filter applied to
     * {@link MorphMap MorphMaps} in the spatial dimension
     *
     * @param spatFiltRad spatial radius of smoothing filter in seconds
     */
    public void setSpatFiltRad(double spatFiltRad) {
        this.spatFiltRad = spatFiltRad;
    }

    /**
     * Get the number of erosion operations applied to a segmented cell in a
     * given frame before it is used as the seed for segmentation in the next
     * frame
     *
     * @return number of erosion operations
     */
    public int getErosion() {
        return erosion;
    }

    /**
     * Set the number of erosion operations applied to a segmented cell in a
     * given frame before it is used as the seed for segmentation in the next
     * frame
     *
     * @param erosion number of erosion operations
     */
    public void setErosion(int erosion) {
        this.erosion = erosion;
    }

    /**
     * Returns true if morphological data is to be generated
     *
     * @return true if morphological data is to be generated
     */
    public boolean isGetMorph() {
        return getMorph;
    }

    /**
     * Set to true if morphological data is to be generated
     *
     * @param getMorph true if morphological data is to be generated
     */
    public void setGetMorph(boolean getMorph) {
        this.getMorph = getMorph;
    }

    /**
     * Get time resolution
     *
     * @return time resolution in frames per minute
     */
    public double getTimeRes() {
        return timeRes;
    }

    /**
     * Set the time resolution
     *
     * @param timeRes time resolution in frames per minute
     */
    public void setTimeRes(double timeRes) {
        this.timeRes = timeRes;
    }

    /**
     * Get the threshold value used to locate curvature extrema in curvature
     * maps
     *
     * @return curvature threshold - extrema must be lower than this value to be
     * considered candidates for bleb anchor points
     */
    public double getMinCurveThresh() {
        return minCurveThresh;
    }

    /**
     * Set the threshold value used to locate curvature extrema in curvature
     * maps
     *
     * @param curveThresh curvature threshold - extrema must be lower than this
     * value to be considered candidates for bleb anchor points
     */
    public void setMinCurveThresh(double curveThresh) {
        this.minCurveThresh = curveThresh;
    }

    /**
     * Returns true if individual blebs are to be analysed
     *
     * @return true if individual blebs are to be analysed
     */
    public boolean isAnalyseProtrusions() {
        return analyseProtrusions;
    }

    /**
     * Set to true if individual blebs are to be analysed
     *
     * @param analyseProtrusions true if individual blebs are to be analysed
     */
    public void setAnalyseProtrusions(boolean analyseProtrusions) {
        this.analyseProtrusions = analyseProtrusions;
    }

    /**
     * Get the minimum fraction of pixels within a cortical signal region that
     * must be above the signal threshold for the bleb to be included in the
     * analysis
     *
     * @return recovery threshold, defined as (number of frames where at least
     * one pixel is above threshold) / (total number of frames)
     */
    public double getSigRecoveryThresh() {
        return sigRecoveryThresh;
    }

    /**
     * Set the minimum fraction of pixels within a cortical signal region that
     * must be above the signal threshold for the bleb to be included in the
     * analysis
     *
     * @param sigRecoveryThresh recovery threshold, defined as (number of frames
     * where at least one pixel is above threshold) / (total number of frames)
     */
    public void setSigRecoveryThresh(double sigRecoveryThresh) {
        this.sigRecoveryThresh = sigRecoveryThresh;
    }

    /**
     * Get the radius of the Gaussian filter used to denoise individual cyto
     * frames
     *
     * @return standard deviation of Gaussian filter in pixels
     */
    public double getGaussRad() {
        return gaussRad;
    }

    /**
     * Set the radius of the Gaussian filter used to denoise individual cyto
     * frames
     *
     * @param gaussRad standard deviation of Gaussian filter in pixels
     */
    public void setGaussRad(double gaussRad) {
        this.gaussRad = gaussRad;
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

//    public   int getMaxCurveRange() {
//        return maxCurveRange;
//    }
//
//    public   void setMaxCurveRange(int maxCurveRange) {
//        this.maxCurveRange = maxCurveRange;
//    }
//    public double getMaxCurveThresh() {
//        return maxCurveThresh;
//    }
//
//    public void setMaxCurveThresh(double maxCurveThresh) {
//        this.maxCurveThresh = maxCurveThresh;
//    }

    public String getThreshMethod() {
        return threshMethod;
    }

    public void setThreshMethod(String threshMethod) {
        this.threshMethod = threshMethod;
    }

    public boolean isBlebDetect() {
        return blebDetect;
    }

    public void setBlebDetect(boolean velDetect) {
        this.blebDetect = velDetect;
    }

    public double getFiloSize() {
        return filoSize;
    }

    public void setFiloSize(double filoSize) {
        this.filoSize = filoSize;
    }

    public boolean isGetFluorDist() {
        return getFluorDist;
    }

    public void setGetFluorDist(boolean getFluorDist) {
        this.getFluorDist = getFluorDist;
    }

    public double getMorphSizeMin() {
        return morphSizeMin;
    }

    public void setMorphSizeMin(double morphSizeMin) {
        this.morphSizeMin = morphSizeMin;
    }

    public int getVisLineWidth() {
        return visLineWidth;
    }

    public void setVisLineWidth(int visLineWidth) {
        this.visLineWidth = visLineWidth;
    }

    public boolean isDisplayPlots() {
        return displayPlots;
    }

    public void setDisplayPlots(boolean displayPlots) {
        this.displayPlots = displayPlots;
    }

    public Object clone() {
        UserVariables copy = new UserVariables();
        copy.setGreyThresh(greyThresh);
        copy.setGenVis(genVis);
        copy.setBlebDurThresh(blebDurThresh);
        copy.setCurveRange(curveRange);
        copy.setUseSigThresh(useSigThresh);
        copy.setSpatialRes(spatialRes);
        copy.setCutOffTime(cutOffTime);
        copy.setCortexDepth(cortexDepth);
        copy.setAutoThreshold(autoThreshold);
        copy.setTempFiltRad(tempFiltRad);
        copy.setSigThreshFact(sigThreshFact);
        copy.setSpatFiltRad(spatFiltRad);
        copy.setErosion(erosion);
        copy.setGetMorph(getMorph);
        copy.setTimeRes(timeRes);
        copy.setBlebLenThresh(blebLenThresh);
        copy.setMinCurveThresh(minCurveThresh);
//        copy.setMaxCurveThresh(maxCurveThresh);
        copy.setAnalyseProtrusions(analyseProtrusions);
        copy.setSigRecoveryThresh(sigRecoveryThresh);
        copy.setGaussRad(gaussRad);
        copy.setSimple(simple);
        copy.setLambda(lambda);
        copy.setMinLength(minLength);
        copy.setThreshMethod(threshMethod);
        copy.setBlebDetect(blebDetect);
        copy.setFiloSize(filoSize);
        copy.setGetFluorDist(getFluorDist);
        copy.setMorphSizeMin(morphSizeMin);
        copy.setVisLineWidth(visLineWidth);
        copy.setDisplayPlots(displayPlots);
        return copy;
    }
}
