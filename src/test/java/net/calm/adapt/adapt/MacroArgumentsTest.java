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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroArgumentsTest {

    @Test
    void parsesKeyValuePairs() {
        MacroArguments args = MacroArguments.parse("params=/a/b.json cytosol=1 signal=2 output=/out");
        assertEquals("/a/b.json", args.get("params"));
        assertEquals("1", args.get("cytosol"));
        assertEquals("2", args.get("signal"));
        assertEquals("/out", args.get("output"));
    }

    @Test
    void ignoresTokensWithoutEquals() {
        MacroArguments args = MacroArguments.parse("params=/a.json garbage cytosol=1");
        assertEquals("/a.json", args.get("params"));
        assertEquals("1", args.get("cytosol"));
        assertFalse(args.has("garbage"));
    }

    @Test
    void handlesNullAndBlankArgs() {
        assertTrue(MacroArguments.parse(null).isEmpty());
        assertTrue(MacroArguments.parse("   ").isEmpty());
    }

    @Test
    void readsIntegersWithDefault() {
        MacroArguments args = MacroArguments.parse("cytosol=2");
        assertEquals(2, args.getInt("cytosol", 1));
        assertEquals(1, args.getInt("signal", 1));
    }
}
