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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Writes the human-readable output manifest ({@code parameters.json} and
 * {@code README.md}) in place of the external {@code PropertyWriter}.
 */
public final class ParameterWriter {

    private ParameterWriter() {
    }

    /**
     * Serialises the given properties to a sorted, pretty-printed JSON object
     * at {@code parDir/parameters.json}.
     */
    public static void saveParameters(Properties props, File parDir) throws IOException {
        TreeMap<String, String> sorted = new TreeMap<>();
        for (String name : props.stringPropertyNames()) {
            sorted.put(name, props.getProperty(name));
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(parDir, "parameters.json"), sorted);
    }

    /**
     * Writes a short {@code README.md} describing the output layout.
     */
    public static void saveReadme(File parDir, String title) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        sb.append("Output from ADAPT (Automated Detection and Analysis of ProTrusions).\n\n");
        sb.append("## Contents\n\n");
        sb.append("- `parameters.json` - analysis parameters used for this run.\n");
        sb.append("- `tables/` - per-cell and per-protrusion metrics as UTF-8 CSV tables.\n");
        sb.append("- `images/` - per-cell map images, one `cell_NNN/` subdirectory per cell.\n");
        sb.append("- `labels.zip` - ImageJ ROI labels.\n\n");
        sb.append("CSV column names use snake_case ASCII.\n");
        Files.writeString(new File(parDir, "README.md").toPath(), sb.toString(), StandardCharsets.UTF_8);
    }
}
