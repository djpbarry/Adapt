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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.calm.iaclasslibrary.UserVariables.UserVariables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Analyse_BatchReadParamsTest {

    private static JsonNode parse(String json) throws Exception {
        return new ObjectMapper().readTree(json);
    }

    private static String validJson() {
        return "{\n"
                + "  \"version\": 1,\n"
                + "  \"autoThreshold\": true,\n"
                + "  \"threshMethod\": \"Otsu\",\n"
                + "  \"greyThresh\": 0.5,\n"
                + "  \"spatialRes\": 0.25,\n"
                + "  \"timeRes\": 30.0,\n"
                + "  \"erosion\": 2,\n"
                + "  \"spatFiltRad\": 4.0,\n"
                + "  \"tempFiltRad\": 4.0,\n"
                + "  \"gaussRad\": 1.5,\n"
                + "  \"genVis\": true,\n"
                + "  \"getMorph\": true,\n"
                + "  \"analyseProtrusions\": true,\n"
                + "  \"blebDetect\": false,\n"
                + "  \"curveRange\": 5,\n"
                + "  \"minCurveThresh\": 2.0,\n"
                + "  \"blebLenThresh\": 1.0,\n"
                + "  \"blebDurThresh\": 10.0,\n"
                + "  \"cutOffTime\": 20.0,\n"
                + "  \"cortexDepth\": 0.6,\n"
                + "  \"useSigThresh\": true,\n"
                + "  \"sigThreshFact\": 1.5,\n"
                + "  \"sigRecoveryThresh\": 0.2,\n"
                + "  \"minLength\": 7,\n"
                + "  \"filoSizeMax\": 2.5,\n"
                + "  \"getFluorDist\": false,\n"
                + "  \"morphSizeMin\": 150.0\n"
                + "}";
    }

    @Test
    void appliesAllFields() throws Exception {
        UserVariables uv = new UserVariables();
        Analyse_Batch.applyParams(uv, parse(validJson()));

        assertTrue(uv.isAutoThreshold());
        assertEquals("Otsu", uv.getThreshMethod());
        assertEquals(0.5, uv.getGreyThresh(), 1e-9);
        assertEquals(0.25, uv.getSpatialRes(), 1e-9);
        assertEquals(30.0, uv.getTimeRes(), 1e-9);
        assertEquals(2, uv.getErosion());
        assertEquals(4.0, uv.getSpatFiltRad(), 1e-9);
        assertEquals(4.0, uv.getTempFiltRad(), 1e-9);
        assertEquals(1.5, uv.getGaussRad(), 1e-9);
        assertTrue(uv.isGenVis());
        assertTrue(uv.isGetMorph());
        assertTrue(uv.isAnalyseProtrusions());
        assertFalse(uv.isBlebDetect());
        assertEquals(5, uv.getCurveRange());
        assertEquals(2.0, uv.getMinCurveThresh(), 1e-9);
        assertEquals(1.0, uv.getBlebLenThresh(), 1e-9);
        assertEquals(10.0, uv.getBlebDurThresh(), 1e-9);
        assertEquals(20.0, uv.getCutOffTime(), 1e-9);
        assertEquals(0.6, uv.getCortexDepth(), 1e-9);
        assertTrue(uv.isUseSigThresh());
        assertEquals(1.5, uv.getSigThreshFact(), 1e-9);
        assertEquals(0.2, uv.getSigRecoveryThresh(), 1e-9);
        assertEquals(7, uv.getMinLength());
        assertEquals(2.5, uv.getFiloSizeMax(), 1e-9);
        assertFalse(uv.isGetFluorDist());
        assertEquals(150.0, uv.getMorphSizeMin(), 1e-9);
    }

    @Test
    void rejectsUnsupportedVersion() throws Exception {
        UserVariables uv = new UserVariables();
        JsonNode root = parse("{\"version\": 99, \"autoThreshold\": true}");
        assertThrows(IllegalArgumentException.class, () -> Analyse_Batch.applyParams(uv, root));
    }

    @Test
    void rejectsMissingVersion() throws Exception {
        UserVariables uv = new UserVariables();
        JsonNode root = parse("{\"autoThreshold\": true}");
        assertThrows(IllegalArgumentException.class, () -> Analyse_Batch.applyParams(uv, root));
    }

    @Test
    void rejectsMissingField() throws Exception {
        UserVariables uv = new UserVariables();
        JsonNode root = parse("{\"version\": 1, \"autoThreshold\": true}");
        assertThrows(IllegalArgumentException.class, () -> Analyse_Batch.applyParams(uv, root));
    }

    @Test
    void rejectsNonObject() throws Exception {
        UserVariables uv = new UserVariables();
        JsonNode root = parse("[]");
        assertThrows(IllegalArgumentException.class, () -> Analyse_Batch.applyParams(uv, root));
    }
}
