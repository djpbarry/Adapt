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

import ij.ImageStack;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Container class containing data specifying an individual bleb
 *
 * @author David Barry <david.barry at cancer.org.uk>
 */
public class Bleb extends Protrusion {

    private Rectangle bounds;
    private ArrayList<Double> protrusionLength;
    private ImageStack detectionStack;
    private ArrayList<ArrayList<Double>> blebPerimSigs;
    private ArrayList<int[]> startpos, endpos;

    /**
     * Return the list of end-points for this bleb
     *
     * @return a series of time-position coordinates for this bleb's end-points,
     * where time corresponds to frame number and position to position along
     * cell boundary
     */
    public ArrayList<int[]> getEndpos() {
        return endpos;
    }

    /**
     * Return the list of start-points for this bleb
     *
     * @return a series of time-position coordinates for this bleb's
     * start-points, where time corresponds to frame number and position to
     * position along cell boundary
     */
    public ArrayList<int[]> getStartpos() {
        return startpos;
    }

    /**
     * Add a start point to this bleb
     *
     * @param startpos a time-position coordinate, {p, t}, where time (t)
     * corresponds to frame number and position to position (p) along cell
     * boundary
     * @return true (as specified by
     * {@link java.util.ArrayList#add(java.lang.Object) ArrayList.add(E)})
     */
    public boolean addStartPos(int[] startpos) {
        if (this.startpos == null) {
            this.startpos = new ArrayList<>();
        }
        return this.startpos.add(startpos);
    }

    /**
     * Add an end point to this bleb
     *
     * @param endpos a time-position coordinate, {p, t}, where time (t)
     * corresponds to frame number and position to position (p) along cell
     * boundary
     * @return true (as specified by
     * {@link java.util.ArrayList#add(java.lang.Object) ArrayList.add(E)})
     */
    public boolean addEndPos(int[] endpos) {
        if (this.endpos == null) {
            this.endpos = new ArrayList<>();
        }
        return this.endpos.add(endpos);
    }

    /**
     * Get signal data for this bleb
     * 
     * @return a series of {@link java.lang.Double Double} values, organised as
     * an {@link java.util.ArrayList ArrayList} of ArrayLists, where each Double
     * corresponds to a signal value and each ArrayList corresponds to a movie frame.
     */
    public ArrayList<ArrayList<Double>> getBlebPerimSigs() {
        return blebPerimSigs;
    }

    public void setBlebPerimSigs(ArrayList<ArrayList<Double>> blebPerimSigs) {
        this.blebPerimSigs = blebPerimSigs;
    }

    /**
     * Get lengths for this bleb over time
     * 
     * @return a series of values corresponding to the length of this bleb at
     * different points in time
     */
    public ArrayList<Double> getProtrusionLength() {
        return protrusionLength;
    }

    public void setProtrusionLength(ArrayList<Double> protrusionLength) {
        this.protrusionLength = protrusionLength;
    }

    /**
     * The region corresponding to this bleb in the velocity map in which it was
     * originally detected
     * 
     * @return a {@link java.awt.Rectangle Rectangle} describing the extent of this
     * bleb in the velocity map in which it was originally detected
     */
    public Rectangle getBounds() {
        return bounds;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * Get the {@link ij.ImageStack ImageStack} representing the output movie on
     * which this bleb will be drawn
     * 
     * @return an {@link ij.ImageStack ImageStack} on which bleb detections are
     * to be drawn
     */
    public ImageStack getDetectionStack() {
        return detectionStack;
    }

    public void setDetectionStack(ImageStack detectionStack) {
        this.detectionStack = detectionStack;
    }

    /**
     * Default constructor
     */
    public Bleb() {
    }
}
