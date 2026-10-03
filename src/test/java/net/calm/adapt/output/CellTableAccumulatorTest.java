/*
 * Copyright (C) 2026 David Barry <david.barry at cancer.org.uk>
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
package net.calm.adapt.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CellTableAccumulatorTest {

    @TempDir
    Path tempDir;

    @Test
    void writesMergedTablesInCellOrder() throws Exception {
        CellTableAccumulator acc = new CellTableAccumulator(2);
        acc.addVelocityRow(0, "0,0,50.0,50.0,1.0,-1.0");
        acc.addVelocityRow(1, "1,0,10.0,90.0,0.5,-0.5");
        acc.addBoundaryRow(1, "1,3,10.0,20.0");
        acc.addBlebRow(0, "0,0,0.0,1.0,2.0,3.0,4.0,5.0");
        acc.save(tempDir.toFile());

        assertEquals("cell_id,frame,%_protruding,%_retracting,mean_protrusion_velocity_um_min,mean_retraction_velocity_um_min\n"
                + "0,0,50.0,50.0,1.0,-1.0\n"
                + "1,0,10.0,90.0,0.5,-0.5\n", read("velocity.csv"));
        assertEquals("cell_id,frame,x,y\n"
                + "1,3,10.0,20.0\n", read("boundary.csv"));
        assertEquals("cell_id,bleb_id,time_s,v_um_s,total_signal_au,mean_signal,length_um,normalised_length\n"
                + "0,0,0.0,1.0,2.0,3.0,4.0,5.0\n", read("blebs.csv"));
    }

    private String read(String name) throws Exception {
        return Files.readString(tempDir.resolve(name), StandardCharsets.UTF_8);
    }
}
