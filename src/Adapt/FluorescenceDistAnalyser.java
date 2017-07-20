/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import java.util.Arrays;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class FluorescenceDistAnalyser {

    ImagePlus imp;
    ImageProcessor mask;
    final int FOREGROUND = 255, BACKGROUND = 0;
    double[][] glcm;
    int offset;
    double contrast, homogeneity, energy, mean, std, skew, kurt;
    public static final String PARAM_HEADINGS = "Contrast\tHomogeneity\tEnergy\tMean\tStandard Deviation\tSkewness\tKurtosis";

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

    double calcGlcmStats() {
        int length = glcm.length;
        contrast = 0.0;
        energy = 0.0;
        homogeneity = 0.0;
        for (int j = 0; j < length; j++) {
            for (int i = 0; i < length; i++) {
                contrast += Math.pow(Math.abs(i - j), 2.0) * glcm[i][j];
                energy += Math.pow(glcm[i][j], 2.0);
                homogeneity += glcm[i][j] / (1.0 + Math.abs(i - j));
            }
        }
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
