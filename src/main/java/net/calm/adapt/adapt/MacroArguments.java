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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the {@code key=value} argument string passed to a macro-callable
 * plugin's {@code run(String arg)}. Tokens are whitespace-separated; paths with
 * embedded spaces are not supported in v1.
 */
public class MacroArguments {

    private final Map<String, String> values;

    private MacroArguments(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Parses a whitespace-separated {@code key=value} string into a key/value
     * map. Tokens without a {@code =} (or with an empty key) are ignored.
     *
     * @param arg the macro argument string (may be {@code null} or blank)
     * @return a parsed argument set
     */
    public static MacroArguments parse(String arg) {
        Map<String, String> values = new LinkedHashMap<>();
        if (arg != null) {
            for (String token : arg.trim().split("\\s+")) {
                int eq = token.indexOf('=');
                if (eq > 0) {
                    values.put(token.substring(0, eq), token.substring(eq + 1));
                }
            }
        }
        return new MacroArguments(values);
    }

    /**
     * @return {@code true} if the argument string contained no {@code key=value}
     * tokens
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * @param key the argument name
     * @return {@code true} if the argument was supplied
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /**
     * @param key the argument name
     * @return the raw string value, or {@code null} if absent
     */
    public String get(String key) {
        return values.get(key);
    }

    /**
     * @param key          the argument name
     * @param defaultValue returned when the argument is absent
     * @return the argument parsed as an integer
     */
    public int getInt(String key, int defaultValue) {
        String v = values.get(key);
        return v == null ? defaultValue : Integer.parseInt(v);
    }
}
