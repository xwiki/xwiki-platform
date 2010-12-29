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
package org.xwiki.properties.internal.converter;

import java.awt.Color;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.StringTokenizer;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

/**
 * Bean Utils converter that converts a value into an {@link Color} object.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component("java.awt.Color")
public class ColorConverter extends AbstractConverter
{
    /**
     * The String input supported by this {@link org.apache.commons.beanutils.Converter}.
     */
    private static final String USAGE = "Color value should be in the form of '#xxxxxx' or 'r,g,b'";

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.converter.AbstractConverter#convertToType(org.xwiki.properties.PropertyType,
     *      java.lang.Object)
     */
    @Override
    protected Color convertToType(Type type, Object value)
    {
        Color color = null;
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
    protected String convertToString(Object value)
    {
        Color colorValue = (Color) value;

        return MessageFormat.format("{0},{1},{2}", colorValue.getRed(), colorValue.getGreen(), colorValue.getBlue());
    }

    /**
     * Parsers a String in the form "x, y, z" into an SWT RGB class.
     * 
     * @param value the color as String
     * @return RGB
     */
    protected Color parseRGB(String value)
    {
        StringTokenizer items = new StringTokenizer(value, ",");

        try {
            int red = 0;
            if (items.hasMoreTokens()) {
                red = Integer.parseInt(items.nextToken().trim());
            }

            int green = 0;
            if (items.hasMoreTokens()) {
                green = Integer.parseInt(items.nextToken().trim());
            }

            int blue = 0;
            if (items.hasMoreTokens()) {
                blue = Integer.parseInt(items.nextToken().trim());
            }

            return new Color(red, green, blue);
        } catch (NumberFormatException ex) {
            throw new ConversionException(value + "is not a valid RGB colo", ex);
        }
    }

    /**
     * Parsers a String in the form "#xxxxxx" into an SWT RGB class.
     * 
     * @param value the color as String
     * @return RGB
     */
    protected Color parseHtml(String value)
    {
        if (value.length() != 7) {
            throw new ConversionException(USAGE);
        }

        int colorValue = 0;
        try {
            colorValue = Integer.parseInt(value.substring(1), 16);
            return new Color(colorValue);
        } catch (NumberFormatException ex) {
            throw new ConversionException(value + "is not a valid Html color", ex);
        }
    }

    /**
     * Convert a String in {@link Color}.
     * 
     * @param value the String to parse
     * @return the {@link Color}
     */
    public Color parse(String value)
    {
        if (value.length() <= 1) {
            throw new ConversionException(USAGE);
        }

        if (value.charAt(0) == '#') {
            return parseHtml(value);
        } else if (value.indexOf(',') != -1) {
            return parseRGB(value);
        } else {
            throw new ConversionException(USAGE);
        }
    }
}
