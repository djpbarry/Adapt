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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects per-cell tabular rows from the parallel output generators and, once
 * every cell has reported, writes them as single tidy long-format tables (one
 * observation per row with a {@code cell_id} column).
 */
public final class CellTableAccumulator {

    private static final String VELOCITY_HEADER = "cell_id,frame,%_protruding,%_retracting,mean_protrusion_velocity_um_min,mean_retraction_velocity_um_min";
    private static final String BOUNDARY_HEADER = "cell_id,frame,x,y";
    private static final String BLEBS_HEADER = "cell_id,bleb_id,time_s,v_um_s,total_signal_au,mean_signal,length_um,normalised_length";

    private final List<List<String>> velocityRows;
    private final List<List<String>> boundaryRows;
    private final List<List<String>> blebRows;

    public CellTableAccumulator(int cellCount) {
        velocityRows = new ArrayList<>(cellCount);
        boundaryRows = new ArrayList<>(cellCount);
        blebRows = new ArrayList<>(cellCount);
        for (int i = 0; i < cellCount; i++) {
            velocityRows.add(new ArrayList<>());
            boundaryRows.add(new ArrayList<>());
            blebRows.add(new ArrayList<>());
        }
    }

    public void addVelocityRow(int cell, String row) {
        velocityRows.get(cell).add(row);
    }

    public void addBoundaryRow(int cell, String row) {
        boundaryRows.get(cell).add(row);
    }

    public void addBlebRow(int cell, String row) {
        blebRows.get(cell).add(row);
    }

    public void save(File tablesDir) throws IOException {
        writeCsv(new File(tablesDir, "velocity.csv"), VELOCITY_HEADER, velocityRows);
        writeCsv(new File(tablesDir, "boundary.csv"), BOUNDARY_HEADER, boundaryRows);
        writeCsv(new File(tablesDir, "blebs.csv"), BLEBS_HEADER, blebRows);
    }

    private void writeCsv(File file, String header, List<List<String>> rows) throws IOException {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (List<String> cellRows : rows) {
            for (String row : cellRows) {
                sb.append(row).append('\n');
            }
        }
        Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }
}
