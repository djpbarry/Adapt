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

import DataProcessing.DataFileAverager;
import ij.plugin.PlugIn;

public class ADAPT_Bleb_Data_Averager implements PlugIn {

    public void run(String arg) {
        String normHeadings[] = new String[]{StaticVariables.TOTAL_SIGNAL, StaticVariables.MEAN_SIGNAL};
        new DataFileAverager(null, normHeadings, true, StaticVariables.VELOCITY, StaticVariables.TIME, StaticVariables.ZEROED_TIME).run(null);
    }
}
