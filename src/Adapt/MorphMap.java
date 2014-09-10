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

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Two dimensional map for representing cell velocities, fluorescence signals at
 * cell boundaries and curvature of cell boundaries
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class MorphMap {

    private int width, height;
    private double[][] xCoords;
    private double[][] yCoords;
    private double[][] zVals;
    //private double[][] distanceMap;
    //private Pixel[][] anchorPoints;
    //private ArrayList<Region> regions;
//    private int tmin, tmax;
//    private double magTmin, magTmax, filtRad = 5.0;
//    private final String dT = '\u03B4' + "t";

    /**
     * Default constructor
     */
    public MorphMap() {
    }

    /**
     * Returns the height of the map
     *
     * @return the height of the map
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the width of the map
     *
     * @return the width of the map
     */
    public int getWidth() {
        return width;
    }

    /**
     * Create a new MorphMap with the specified width and height
     *
     * @param width should correspond to the number of frames in the original
     * movie analysed
     * @param height should correspond to the maximum length of the cell
     * perimeter in the original movie sequence
     */
    public MorphMap(int width, int height) {
        this.width = width;
        this.height = height;
        xCoords = new double[width][height];
        yCoords = new double[width][height];
        zVals = new double[width][height];
    }

//    public double[][] calcDeltaMap() {
//        double[][] deltaMap = new double[width][height];
//        int k, l, m;
//        for (int j = 0; j < height; j++) {
//            k = j - 1;
//            l = j;
//            m = j + 1;
//            if (k < 0) {
//                k += height;
//            }
//            if (m >= height) {
//                m -= height;
//            }
//            for (int i = 1; i < width - 1; i++) {
//                deltaMap[i][j] = -zVals[i - 1][k]
//                        - 2.0 * zVals[i - 1][l]
//                        - zVals[i - 1][m]
//                        + zVals[i + 1][k]
//                        + 2.0 * zVals[i + 1][l]
//                        + zVals[i + 1][m];
//            }
//        }
//        return deltaMap;
//    }
    /**
     * Apply a Gaussian smoothing filter to the MorphMap, with the specified
     * radii in the temporal and spatial dimensions
     *
     * @param tempFiltRad standard deviation of Gaussian to be applied in
     * temporal (y) dimension
     * @param spatFiltRad standard deviation of Gaussian to be applied in
     * temporal (x) dimension
     * @return the smoothed map as a two-dimensional array
     */
    public double[][] smoothMap(double tempFiltRad, double spatFiltRad) {
        return smoothMap(zVals, tempFiltRad, spatFiltRad);
    }

    private double[][] smoothMap(double[][] map, double tempFiltRad, double spatFiltRad) {
        if (!(tempFiltRad > 0.0 && spatFiltRad > 0.0)) {
            return map;
        }
        FloatProcessor fp = new FloatProcessor(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                fp.putPixelValue(x, y, map[x][y]);
            }
        }
        (new GaussianBlur()).blurFloat(fp, tempFiltRad, spatFiltRad, 0.01);
        double[][] smoothedMap = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                smoothedMap[x][y] = fp.getPixelValue(x, y);
            }
        }
        return smoothedMap;
    }

    /**
     * Add a column to this MorphMap
     *
     * @param x x-coordinates
     * @param y y-coordinates
     * @param z z-coordinates, corresponding to the property of interest that
     * the map represents (e.g. cell boundary velocity).
     * @param t the time index that this column represents
     */
    public void addColumn(double x[], double y[], double z[], int t) {
        for (int i = 0; i < z.length; i++) {
            xCoords[t][i] = x[i];
            yCoords[t][i] = y[i];
            zVals[t][i] = z[i];
        }
        for (int i = z.length; i < height; i++) {
            zVals[t][i] = 0.0;
        }
    }

//    public Plot periodicity(double[][] map, String directory) {
//        PrintWriter outputStream = null;
//        File results = null;
//        if (directory != null) {
//            try {
//                results = new File(directory + "AutoCorrelation.csv");
//            } catch (Exception e) {
//                IJ.error(e.toString());
//            }
//            try {
//                outputStream = new PrintWriter(new FileOutputStream(results));
//            } catch (FileNotFoundException e) {
//                IJ.error("Could not write to results file.");
//            }
//        }
//        int T = map.length / 2;
//        double mean = getMean(map);
//        double autoCorrelation = correlation(map, 0, mean);
//        double[] crossCorrelation = new double[T];
//        double[] deltaT = new double[T];
//        crossCorrelation[0] = 1.0;
//        deltaT[0] = 0.0;
//        for (int i = 1; i < T; i++) {
//            deltaT[i] = i;
//            crossCorrelation[i] = correlation(map, i, mean) / autoCorrelation;
//            if (outputStream != null) {
//                outputStream.print(crossCorrelation[i] + ",");
//            }
//        }
//        if (outputStream != null) {
//            outputStream.close();
//        }
//        return new Plot("Correlation Function", dT, "c(" + dT + ")", deltaT, crossCorrelation);
//    }
//    public double correlation(double[][] map, int t, double mean) {
//        double sum = 0.0;
//        for (int i = 0; i + t < map.length; i++) {
//            for (int j = 0; j < map[i].length; j++) {
//                sum += (map[i + t][j] - mean) * (map[i][j] - mean);
//            }
//        }
//        return sum;
//    }
    private double correlation2D(ImageProcessor map1, ImageProcessor map2, int t, int s, double mean1, double mean2) {
        double sum = 0.0;
        int count = 0;
        int w = map1.getWidth(), h = map1.getHeight();
        for (int i = ((t < 0) ? -t : 0); i + t < w; i++) {
            for (int j = ((s < 0) ? -s : 0); j + s < h; j++) {
                sum += (map1.getPixelValue(i + t, j + s) - mean1) * (map2.getPixelValue(i, j) - mean2);
                count++;
            }
        }
        return sum / count;
    }

    /**
     * Computes the cross-correlation of two maps
     *
     * @param mapa MorphMap in the form of an
     * {@link ij.process.ImageProcessor ImageProcessor}
     * @param mapb MorphMap in the form of an
     * {@link ij.process.ImageProcessor ImageProcessor}
     * @param newsize maps are resized to this square dimension to speed
     * calculation
     * @return an {@link ij.ImagePlus ImagePlus} representing the
     * cross-correlation of the two maps
     */
    public ImagePlus periodicity2D(ImageProcessor mapa, ImageProcessor mapb, int newsize) {
        ImageProcessor map1 = mapa.resize(newsize, newsize, true);
        ImageProcessor map2 = mapb.resize(newsize, newsize, true);
        double sd2 = map2.getStatistics().stdDev * map1.getStatistics().stdDev;
        int w = map1.getWidth();
        int h = map1.getHeight();
        int T = w;
        int S = h;
        int midX = T / 2;
        int midY = S / 2;
        double mean1 = map1.getStatistics().mean;
        double mean2 = map2.getStatistics().mean;
        FloatProcessor crossCorrelation = new FloatProcessor(T, S);
        crossCorrelation.putPixelValue(midX, midY, 1.0);
        for (int x = 0; x < T; x++) {
            for (int y = 0; y < S; y++) {
                double c = correlation2D(map1, map2, x - midX, y - midY, mean1, mean2) / sd2;
                crossCorrelation.putPixelValue(x, y, c);
            }
        }
        return new ImagePlus("2D AutoCorrelation", crossCorrelation);
    }

//    private int correlateColumns(int c1, int c2) {
//        double minDist = Double.MAX_VALUE;
//        int offset = 0;
//        for (int i = height - 1; i >= 0; i--) {
//            double dist = Utils.calcDistance(xCoords[c1][0], yCoords[c1][0], xCoords[c2][i], yCoords[c2][i]);
//            if (dist < minDist) {
//                minDist = dist;
//                offset = i;
//            }
//        }
//        return offset;
//    }
//    public double getMean(double[][] map) {
//        double sum = 0.0;
//        for (int i = 0; i < map.length; i++) {
//            for (int j = 0; j < map[i].length; j++) {
//                sum += map[i][j];
//            }
//        }
//        return sum / (width * height);
//    }
    /**
     * Get the x-coordinates of this map
     *
     * @return x-coordinates as a two-dimensional array
     */
    public double[][] getxCoords() {
        return xCoords;
    }

    /**
     * Get the y-coordinates of this map
     *
     * @return y-coordinates as a two-dimensional array
     */
    public double[][] getyCoords() {
        return yCoords;
    }

//    public double[][] allignedMap(double[][] map) {
//        double[][] allignedMap = map.clone();
//        double[][] allignedX = xCoords.clone();
//        double[][] allignedY = yCoords.clone();
//        for (int i = 0; i < map.length - 1; i++) {
//            int xoffset = correlateColumns(i, i + 1);
//            int yoffset = correlateColumns(i, i + 1);
//            int offset = (xoffset + yoffset) / 2;
//            allignedX = shiftColumn(allignedX, i + 1, offset);
//            allignedY = shiftColumn(allignedY, i + 1, offset);
//            allignedMap = shiftColumn(allignedMap, i + 1, offset);
//        }
//        xCoords = allignedX;
//        yCoords = allignedY;
//        return allignedMap;
//    }
//    private double[][] shiftColumn(double[][] map, int colIndex, int offset) {
//        double[] column = new double[map[0].length];
//        System.arraycopy(map[colIndex], 0, column, 0, column.length);
//        int length = column.length;
//        for (int i = 0; i < length; i++) {
//            int j = i + offset;
//            if (j < 0) {
//                j += length;
//            } else if (j >= length) {
//                j -= length;
//            }
//            map[colIndex][i] = column[j];
//        }
//        return map;
//    }
//    private void calcTMin(double[] correlation, int startIndex) {
//        int l = correlation.length;
//        for (int i = startIndex; i < l - 2; i++) {
//            if (correlation[i] > correlation[i + 1] && correlation[i + 1] < correlation[i + 2] && correlation[i + 1] < 0.0) {
//                tmin = i + 1 - startIndex;
//                magTmin = Math.abs(correlation[i + 1]);
//                return;
//            }
//        }
//        return;
//    }
//    private void calcTMax(double[] correlation, int startIndex) {
//        int l = correlation.length;
//        for (int i = startIndex; i < l - 2; i++) {
//            if (correlation[i] < correlation[i + 1] && correlation[i + 1] > correlation[i + 2] && correlation[i + 1] > 0.0) {
//                tmax = i + 1 - startIndex;
//                magTmax = Math.abs(correlation[i + 1]);
//                return;
//            }
//        }
//        return;
//    }
//    public int getTmax() {
//        return tmax;
//    }
//    public int getTmin() {
//        return tmin;
//    }
//    public double getMagTmax() {
//        return magTmax;
//    }
//    public double getMagTmin() {
//        return magTmin;
//    }
    /**
     * Return the z-values of this MorphMap
     *
     * @return two-dimensional array of z-values
     */
    public double[][] getzVals() {
        return zVals;
    }

//    Color getColor(double val) {
//        Color colour = Color.black;
//        int r, g;
//        if (val >= 0.0) {
//            r = 255 - (int) Math.round(255 * val);
//            if (r < 0) {
//                r = 0;
//            }
//            colour = new Color(r, 255, 0);
//        } else if (val < 0.0) {
//            g = 255 + (int) Math.round(255 * val);
//            if (g < 0) {
//                g = 0;
//            }
//            colour = new Color(255, g, 0);
//        }
//        return colour;
//    }
    ImageProcessor calcRateOfChange(ImageProcessor input) {
        int sobel[][] = new int[3][3];
        sobel[0][0] = -1;
        sobel[1][0] = 0;
        sobel[2][0] = 1;
        sobel[0][1] = -2;
        sobel[1][1] = 0;
        sobel[2][1] = 2;
        sobel[0][2] = -1;
        sobel[1][2] = 0;
        sobel[2][2] = 1;
        int w = input.getWidth();
        int h = input.getHeight();
        FloatProcessor output = new FloatProcessor(w, h);
        output.setValue(0.0);
        output.fill();
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                double sum = 0.0;
                for (int j = y - 1; j <= y + 1; j++) {
                    for (int i = x - 1; i <= x + 1; i++) {
                        sum += input.getPixelValue(i, j) * sobel[i - x + 1][j - y + 1];
                    }
                }
                output.putPixelValue(x, y, sum);
            }
        }
        return output;
    }
}
