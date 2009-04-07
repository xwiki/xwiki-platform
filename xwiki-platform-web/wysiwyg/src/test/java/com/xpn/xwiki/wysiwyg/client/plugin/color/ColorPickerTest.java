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
package com.xpn.xwiki.wysiwyg.client.plugin.color;

import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;

/**
 * Unit tests for {@link ColorPicker}.
 * 
 * @version $Id$
 */
public class ColorPickerTest extends AbstractWysiwygClientTest
{
    /**
     * The array of hex color codes that are tested.
     */
    private static final String[] HEX_CODES =
        new String[] {"#000000", "#FF0000", "#00FF00", "#0000FF", "#FFFFFF", "#C27BA0"};

    /**
     * Tests if a decimal integer (IE specific) is converted to hex.
     */
    public void testConvertDecimalIntegerToHex()
    {
        String[] decimals = new String[] {"0", "255", "65280", "16711680", "16777215", "10517442"};
        for (int i = 0; i < HEX_CODES.length; i++) {
            assertEquals(HEX_CODES[i], ColorPicker.convertToHex(decimals[i]).toUpperCase());
        }
    }

    /**
     * Tests if a RGB CSS expression is converted to hex.
     */
    public void testConvertRGBToHex()
    {
        String[] rgbCodes =
            new String[] {"rgb(0, 0, 0)", "rgb(255, 0, 0)", "rgb(0, 255, 0)", "rgb(0, 0, 255)", "rgb(255, 255, 255)",
                "rgb(194, 123, 160)"};
        for (int i = 0; i < HEX_CODES.length; i++) {
            assertEquals(HEX_CODES[i], ColorPicker.convertToHex(rgbCodes[i]).toUpperCase());
        }
    }

    /**
     * Tests if a hex color code is left unchanged.
     */
    public void testConvertHexToHex()
    {
        String hex = "#F0F0EE";
        assertEquals(hex, ColorPicker.convertToHex(hex));
    }

    /**
     * Tests if a color name is left unchanged. Update this test when support for color names will be added.
     */
    public void testConvertColorNameToHex()
    {
        String colorName = "red";
        assertEquals(colorName, ColorPicker.convertToHex(colorName));
    }
}
