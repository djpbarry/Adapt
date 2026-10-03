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
package net.calm.adapt.adapt;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.util.Arrays;

public class FluorescenceDistAnalyser {

    ImagePlus imp;
    ImageProcessor mask;
    final int FOREGROUND = 255, BACKGROUND = 0;
    double[][] glcm;
    int offset;
    double contrast, homogeneity, energy, mean, std, skew, kurt;
//    public static final String PARAM_HEADINGS = "Contrast\tHomogeneity\tEnergy\tMean\tStandard Deviation\tSkewness\tKurtosis";
    public static final String[] PARAM_HEADINGS = {"cell_id", "Frame", "Contrast", "Homogeneity", "Energy", "Mean", "Standard Deviation", "Skewness", "Kurtosis"};

    public FluorescenceDistAnalyser(ImagePlus imp, ImageProcessor mask, int offset) {
        this.imp = imp;
        this.mask = mask;
        this.offset = offset;
    }

    public void doAnalysis() {
        constructGLCM();
        calcGlcmStats();
        setStats();
    }

    void checkMaskNull() {
        if (mask != null) {
            return;
        } else {
            mask = new ByteProcessor(imp.getWidth(), imp.getHeight());
        }
        mask.setValue(BACKGROUND);
        mask.fill();
    }

    void constructGLCM() {
        checkMaskNull();
        int width = imp.getWidth();
        int height = imp.getHeight();
        ImageProcessor ip = imp.getProcessor();
        glcm = new double[256][256];
        int count = 0;
        for (int i = 0; i < glcm.length; i++) {
            Arrays.fill(glcm[i], 0);
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width - offset; x++) {
                if (mask.getPixel(x, y) != BACKGROUND && mask.getPixel(x + offset, y) != BACKGROUND) {
                    glcm[ip.getPixel(x, y)][ip.getPixel(x + offset, y)]++;
                    count++;
                }
            }
        }
        int length = glcm.length;
        for (int j = 0; j < length; j++) {
            for (int i = 0; i < length; i++) {
                glcm[i][j] /= count;
            }
        }
    }

    void setStats() {
        ImageProcessor ip = imp.getProcessor().duplicate();
        ip.setMask(mask);
        ImageStatistics stats = ImageStatistics.getStatistics(ip,
                Measurements.MEAN + Measurements.STD_DEV + Measurements.KURTOSIS + Measurements.SKEWNESS,
                null);
        mean = stats.mean;
        std = stats.stdDev;
        skew = stats.skewness;
        kurt = stats.kurtosis;
    }

    static double[] calcGlcmStats(double[][] glcm) {
        int length = glcm.length;
        double contrast = 0.0, energy = 0.0, homogeneity = 0.0;
        for (int j = 0; j < length; j++) {
            for (int i = 0; i < length; i++) {
                contrast += Math.pow(Math.abs(i - j), 2.0) * glcm[i][j];
                energy += Math.pow(glcm[i][j], 2.0);
                homogeneity += glcm[i][j] / (1.0 + Math.abs(i - j));
            }
        }
        return new double[]{contrast, energy, homogeneity};
    }

    double calcGlcmStats() {
        double[] stats = calcGlcmStats(glcm);
        contrast = stats[0];
        energy = stats[1];
        homogeneity = stats[2];
        return contrast;
    }

    public double getContrast() {
        return contrast;
    }

    public double getHomogeneity() {
        return homogeneity;
    }

    public double getEnergy() {
        return energy;
    }

    public double getMean() {
        return mean;
    }

    public double getStd() {
        return std;
    }

    public double getSkew() {
        return skew;
    }

    public double getKurt() {
        return kurt;
    }

}
