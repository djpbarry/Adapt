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

import net.calm.iaclasslibrary.UserVariables.UserVariables;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParameterPropertiesTest {

    private static UserVariables populated() throws Exception {
        UserVariables uv = new UserVariables();
        Analyse_Batch.applyParams(uv, new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                "{\n"
                + "  \"version\": 1,\n"
                + "  \"autoThreshold\": true,\n"
                + "  \"threshMethod\": \"Otsu\",\n"
                + "  \"greyThresh\": 0.95,\n"
                + "  \"spatialRes\": 0.2,\n"
                + "  \"timeRes\": 60.0,\n"
                + "  \"erosion\": 4,\n"
                + "  \"spatFiltRad\": 5.0,\n"
                + "  \"tempFiltRad\": 5.0,\n"
                + "  \"gaussRad\": 1.0,\n"
                + "  \"genVis\": true,\n"
                + "  \"getMorph\": true,\n"
                + "  \"analyseProtrusions\": true,\n"
                + "  \"blebDetect\": true,\n"
                + "  \"curveRange\": 10,\n"
                + "  \"minCurveThresh\": 0.0,\n"
                + "  \"blebLenThresh\": 1.0,\n"
                + "  \"blebDurThresh\": 10.0,\n"
                + "  \"cutOffTime\": 120.0,\n"
                + "  \"cortexDepth\": 1.0,\n"
                + "  \"useSigThresh\": true,\n"
                + "  \"sigThreshFact\": 0.0,\n"
                + "  \"sigRecoveryThresh\": 0.25,\n"
                + "  \"minLength\": 10,\n"
                + "  \"filoSizeMax\": 4.0,\n"
                + "  \"getFluorDist\": true,\n"
                + "  \"morphSizeMin\": 10.0,\n"
                + "  \"visLineWidth\": 2,\n"
                + "  \"displayPlots\": true,\n"
                + "  \"filoSizeMin\": 1.0\n"
                + "}"));
        return uv;
    }

    @Test
    void emitsAllLabelKeyedEntries() throws Exception {
        Properties props = ParameterProperties.fromUserVariables(populated());
        assertEquals(27, props.size());
    }

    @Test
    void formatsNumericAndBooleanValuesLikeTheGui() throws Exception {
        Properties props = ParameterProperties.fromUserVariables(populated());
        assertEquals("0.95", props.getProperty(StaticVariables.GREY_SENS));
        assertEquals("4", props.getProperty(StaticVariables.EROSION));
        assertEquals("0.0", props.getProperty(StaticVariables.MIN_CURVE_THRESH));
        assertEquals("true", props.getProperty(StaticVariables.AUTO_THRESH));
        assertEquals("0.2", props.getProperty(StaticVariables.SPAT_RES));
    }

    @Test
    void detectFilopodiaIsTheInverseOfDetectBlebs() throws Exception {
        Properties props = ParameterProperties.fromUserVariables(populated());
        assertEquals("true", props.getProperty(StaticVariables.DETECT_BLEB));
        assertEquals("false", props.getProperty(StaticVariables.DETECT_FILO));
    }

    @Test
    void omitsParametersWithoutGuiControls() throws Exception {
        Properties props = ParameterProperties.fromUserVariables(populated());
        assertNull(props.getProperty(StaticVariables.THRESH_METHOD));
    }
}
