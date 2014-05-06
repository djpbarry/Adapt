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

import java.awt.Polygon;
import java.util.ArrayList;

/**
 * Container class for data pertaining to cellular protrusions
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class Protrusion {

    String mapDir;
    int maxExtent;
    ArrayList<Double> meanVel;
    ArrayList<Polygon> polys; // Points around bleb perimeter
    ArrayList<Double> sumSig;

    /**
     * Default constructor
     */
    public Protrusion() {
    }

    /**
     * Get the maximum perimeter length attained by this protrusion
     * 
     * @return maximum protrusion length in pixels
     */
    public int getMaxExtent() {
        return maxExtent;
    }

    public void setMaxExtent(int maxExtent) {
        this.maxExtent = maxExtent;
    }

    /**
     * Get a list of polygons, each representing this protrusion at a different
     * point in time
     * 
     * @return list of polygon-representations of this protrusion, where the ArrayList
     * index represents movie frames
     */
    public ArrayList<Polygon> getPolys() {
        return polys;
    }

    public void setPolys(ArrayList<Polygon> polys) {
        this.polys = polys;
    }

    /**
     * Get a list of values corresponding to the sum of signal pixel values along
     * the protrusion periphery (to a depth of {@link UserVariables#cortexDepth cortexDepth}}
     * 
     * @return list of time-varying signal values for this protrusion
     */
    public ArrayList<Double> getSumSig() {
        return sumSig;
    }

    public void setSumSig(ArrayList<Double> sumSig) {
        this.sumSig = sumSig;
    }

    /**
     * Get a list of values corresponding to the mean membrane velocity along
     * the protrusion periphery
     * 
     * @return list of time-varying velocity values for this protrusion
     */
    public ArrayList<Double> getMeanVel() {
        return meanVel;
    }

    public void setMeanVel(ArrayList<Double> meanVel) {
        this.meanVel = meanVel;
    }
}
