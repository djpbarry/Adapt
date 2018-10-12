/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import IAClasses.Region;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import java.io.IOException;
import java.util.ArrayList;

public class RegionFluorescenceQuantifier {

    private final Region[] regions;
    private final ImageStack stack;
    private final ArrayList<ArrayList<Double>> data;
    private final int index;

    public RegionFluorescenceQuantifier(Region[] regions, ImageStack stack, ArrayList<ArrayList<Double>> data, int index) {
        this.regions = regions;
        this.stack = stack;
        this.data = data;
        this.index = index;
    }

    public void doQuantification() throws IOException {
        int length = stack.size();
        for (int i = 1; i <= length; i++) {
            IJ.showProgress(i,length);
            data.add(new ArrayList());
            if (regions[i - 1] != null) {
                ImageProcessor mask = regions[i - 1].getMask();
                mask.invert();
                FluorescenceDistAnalyser fa = new FluorescenceDistAnalyser(new ImagePlus("", stack.getProcessor(i).convertToByteProcessor(true)), mask, 1);
                fa.doAnalysis();
                ArrayList<Double> thisData = data.get(i - 1);
                thisData.add((double) index);
                thisData.add((double) (i - 1));
                thisData.add(fa.getContrast());
                thisData.add(fa.getHomogeneity());
                thisData.add(fa.getEnergy());
                thisData.add(fa.getMean());
                thisData.add(fa.getStd());
                thisData.add(fa.getSkew());
                thisData.add(fa.getKurt());
            }
        }
    }
}
