/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import IAClasses.Region;
import UtilClasses.GenUtils;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.csv.CSVPrinter;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class RegionFluorescenceQuantifier {

    Region[] regions;
    ImageStack stack;
    CSVPrinter printer;
    static TextWindow tw = new TextWindow("Fluorescence Distribution", FluorescenceAnalyser.PARAM_HEADINGS, new ArrayList(), 640, 480);

    public RegionFluorescenceQuantifier(Region[] regions, ImageStack stack, CSVPrinter printer) {
        this.regions = regions;
        this.stack = GenUtils.convertStackTo8Bit(stack);
        this.printer = printer;
    }

    public void doQuantification() throws IOException {
        int length = stack.size();
        String headings = FluorescenceAnalyser.PARAM_HEADINGS;
        printer.printRecord(headings.replace('\t', ','));
        for (int i = 1; i <= length; i++) {
            if (regions[i - 1] != null) {
                ImageProcessor mask = regions[i - 1].getMask();
                mask.invert();
                FluorescenceAnalyser fa = new FluorescenceAnalyser(new ImagePlus("", stack), mask, 1);
                fa.doAnalysis();
                printer.printRecord(fa.getContrast(), fa.getHomogeneity(), fa.getEnergy(), fa.getMean(), fa.getStd(), fa.getSkew(), fa.getKurt());
                tw.append(fa.getContrast() + "\t" + fa.getHomogeneity() + "\t" + fa.getEnergy() + "\t" + fa.getMean() + "\t" + fa.getStd() + "\t" + fa.getSkew() + "\t" + fa.getKurt());
            }
        }
    }
}
