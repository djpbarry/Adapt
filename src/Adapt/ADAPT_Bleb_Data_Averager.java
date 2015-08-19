/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Adapt;

import DataProcessing.DataFileAverager;
import ij.plugin.PlugIn;

/**
 *
 * @author David Barry <david.barry at crick.ac.uk>
 */
public class ADAPT_Bleb_Data_Averager implements PlugIn {

    public void run(String arg) {
        String normHeadings[] = new String[]{StaticVariables.TOTAL_SIGNAL, StaticVariables.MEAN_SIGNAL};
        new DataFileAverager(null, normHeadings, true, StaticVariables.VELOCITY, StaticVariables.TIME, StaticVariables.ZEROED_TIME).run(null);
    }
}
