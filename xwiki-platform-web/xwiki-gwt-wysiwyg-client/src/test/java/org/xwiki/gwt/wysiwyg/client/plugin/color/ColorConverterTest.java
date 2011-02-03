/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.wysiwyg.client.plugin.color;

import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;

/**
 * Unit tests for {@link ColorPicker}.
 * 
 * @version $Id$
 */
public class ColorConverterTest extends WysiwygTestCase
{
    /**
     * The array of hex color codes that are tested.
     */
    private static final String[] HEX_CODES =
        new String[] {"#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFFFF", "#C27BA0"};

    /**
     * The object being tested.
     */
    private ColorConverter converter = new ColorConverter();

    /**
     * Tests if a decimal integer (IE specific) is converted to hex.
     */
    public void testConvertDecimalIntegerToHex()
    {
        String[] decimals = new String[] {"0", "255", "65280", "16711680", "16777215", "10517442"};
        for (int i = 0; i < HEX_CODES.length; i++) {
            assertEquals(HEX_CODES[i], converter.convertToHex(decimals[i]).toUpperCase());
        }
    }

    /**
     * Tests if a RGB CSS expression is converted to hex.
     */
    public void testConvertRGBToHex()
    {
        String[] rgbCodes = new String[] {"rgb(0, 0, 0)", "rgb(255, 0, 0)", "rgb(0, 255, 0)",
            "rgb(0, 0, 255)", "rgb(255, 255, 255)", "rgb(194, 123, 160)"};
        for (int i = 0; i < HEX_CODES.length; i++) {
            assertEquals(HEX_CODES[i], converter.convertToHex(rgbCodes[i]).toUpperCase());
        }
    }

    /**
     * Tests if a hex color code is left unchanged.
     */
    public void testConvertHexToHex()
    {
        String hex = "#F0F0EE";
        assertEquals(hex, converter.convertToHex(hex));
    }

    /**
     * Tests if a color name properly converted to hex.
     */
    public void testConvertColorNameToHex()
    {
        assertEquals(HEX_CODES[1], converter.convertToHex("red"));
    }

    /**
     * Tests if {@link ColorPicker#convertToHex(String)} is null-safe.
     */
    public void testConvertNullToHex()
    {
        assertNull(converter.convertToHex(null));
    }

    /**
     * Converter should return {@code null} for unknown formats.
     */
    public void testConvertUnknownToHex()
    {
        assertNull(converter.convertToHex("transparent"));
    }
}
