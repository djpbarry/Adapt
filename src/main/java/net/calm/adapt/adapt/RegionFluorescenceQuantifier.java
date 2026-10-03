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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import net.calm.iaclasslibrary.IAClasses.Region;

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
            IJ.showStatus(String.format("Quantifying fluorescence distribution %d%%",(int)Math.round(i * 100.0 / length)));
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
