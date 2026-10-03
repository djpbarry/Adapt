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

import ij.measure.ResultsTable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * UTF-8 CSV writer used in place of the external {@code DataWriter}, which
 * hardcodes ISO-8859-1 and therefore breaks non-ASCII headings (e.g. the
 * micro sign in {@code V_(µm/s)}).
 */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static void saveValues(double[][] vals, File dataFile, String[] colHeadings,
            String[] rowLabels, boolean append) throws IOException {
        try (OutputStreamWriter out = new OutputStreamWriter(
                new FileOutputStream(dataFile, append), StandardCharsets.UTF_8)) {
            if (colHeadings != null) {
                out.write(String.join(",", colHeadings));
                out.write("\n");
            }
            for (int l = 0; l < vals.length; l++) {
                if (vals[l] == null) {
                    continue;
                }
                StringBuilder row = new StringBuilder();
                if (rowLabels != null) {
                    row.append(rowLabels.length > l ? rowLabels[l] : " ");
                }
                for (double v : vals[l]) {
                    if (row.length() > 0) {
                        row.append(',');
                    }
                    row.append(Double.isNaN(v) ? " " : String.valueOf(v));
                }
                out.write(row.toString());
                out.write("\n");
            }
        }
    }

    public static void saveValues(ArrayList<ArrayList<Double>> vals, File dataFile,
            String[] colHeadings, String[] rowLabels, boolean append) throws IOException {
        double[][] convertedVals = new double[vals.get(0).size()][vals.size()];
        for (int j = 0; j < convertedVals.length; j++) {
            for (int i = 0; i < convertedVals[j].length; i++) {
                convertedVals[j][i] = vals.get(i).get(j);
            }
        }
        saveValues(convertedVals, dataFile, colHeadings, rowLabels, append);
    }

    public static void saveResultsTable(ResultsTable rt, File file, boolean append,
            boolean useHeadings) throws IOException {
        if (rt.getCounter() < 1) {
            return;
        }
        String[] headings = rt.getHeadings();
        boolean labels = headings[0].contentEquals("Label");
        int nCols = headings.length;
        int nRows = rt.getCounter();
        String[] rowLabels = labels ? new String[nRows] : null;
        double[][] data = new double[nRows][labels ? nCols - 1 : nCols];
        for (int j = 0; j < nRows; j++) {
            if (labels) {
                rowLabels[j] = rt.getLabel(j);
            }
            for (int i = labels ? 1 : 0; i < nCols; i++) {
                data[j][labels ? i - 1 : i] = rt.getValueAsDouble(rt.getColumnIndex(headings[i]), j);
            }
        }
        saveValues(data, file, useHeadings ? headings : null, rowLabels, append);
    }
}
