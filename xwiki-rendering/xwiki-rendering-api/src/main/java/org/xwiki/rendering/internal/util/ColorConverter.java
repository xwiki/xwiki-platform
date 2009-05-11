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
package org.xwiki.rendering.internal.util;

import java.awt.Color;
import java.util.StringTokenizer;

import org.apache.commons.beanutils.converters.AbstractConverter;

/**
 * Bean Utils converter that converts a value into an {@link Color} object.
 * 
 * @version $Id$
 * @since 1.6M2
 */
public final class ColorConverter extends AbstractConverter
{
    private static final ColorConverter instance = new ColorConverter();

    private static final String USAGE = "Color value should be in the form of '#xxxxxx' or 'r,g,b'";

    public static ColorConverter getInstance()
    {
        return instance;
    }

    private ColorConverter()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.commons.beanutils.converters.AbstractConverter#convertToType(java.lang.Class, java.lang.Object)
     */
    @Override
    protected Object convertToType(Class type, Object value) throws Throwable
    {
        Object color = null;
        if (value != null) {
            color = parse(value.toString());
        }

        return color;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.commons.beanutils.converters.AbstractConverter#convertToString(java.lang.Object)
     */
    @Override
    protected String convertToString(Object value) throws Throwable
    {
        Color colorValue = (Color) value;

        return colorValue.getRed() + "," + colorValue.getGreen() + "," + colorValue.getBlue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.commons.beanutils.converters.AbstractConverter#getDefaultType()
     */
    @Override
    protected Class getDefaultType()
    {
        return Color.class;
    }

    /**
     * Parsers a String in the form "x, y, z" into an SWT RGB class.
     * 
     * @param value
     * @return RGB
     */
    protected Color parseRGB(String value)
    {
        StringTokenizer items = new StringTokenizer(value, ",");
        int red = 0;
        int green = 0;
        int blue = 0;
        if (items.hasMoreTokens()) {
            red = parseNumber(items.nextToken());
        }
        if (items.hasMoreTokens()) {
            green = parseNumber(items.nextToken());
        }
        if (items.hasMoreTokens()) {
            blue = parseNumber(items.nextToken());
        }
        return new Color(red, green, blue);
    }

    /**
     * Parsers a String in the form "#xxxxxx" into an SWT RGB class
     * 
     * @param value
     * @return RGB
     */
    protected Color parseHtml(String value)
    {
        if (value.length() != 7) {
            throw new IllegalArgumentException(USAGE);
        }
        int colorValue = 0;
        try {
            colorValue = Integer.parseInt(value.substring(1), 16);
            return new Color(colorValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(value + "is not a valid Html color\n " + ex);
        }
    }

    /**
     * Parse a String
     */
    public Color parse(String value)
    {
        if (value.length() <= 1) {
            throw new IllegalArgumentException(USAGE);
        }

        if (value.charAt(0) == '#') {
            return parseHtml(value);
        } else if (value.indexOf(',') != -1) {
            return parseRGB(value);
        } else {
            throw new IllegalArgumentException(USAGE);
        }
    }

    protected int parseNumber(String text)
    {
        return Integer.parseInt(text.trim());
    }
}
