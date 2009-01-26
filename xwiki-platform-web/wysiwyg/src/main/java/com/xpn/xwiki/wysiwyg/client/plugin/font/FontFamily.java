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
package com.xpn.xwiki.wysiwyg.client.plugin.font;

/**
 * Defines an entry in the font family list box.
 * 
 * @version $Id$
 */
public class FontFamily
{
    /**
     * The label.
     */
    private String name;

    /**
     * The value, a comma-separated list of font families.
     */
    private String sValue;

    /**
     * The array of font families associated with {@link #name}. This array is obtained by splitting {@link #sValue}.
     */
    private String[] aValue;

    /**
     * Creates a new entry in the font family list box.
     * 
     * @param name {@link #name}
     * @param sValue {@link #sValue}
     */
    public FontFamily(String name, String sValue)
    {
        this.name = name;
        this.sValue = sValue;
        this.aValue = sValue.split(",");
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return {@link #sValue}
     */
    public String getStringValue()
    {
        return sValue;
    }

    /**
     * @return {@link #aValue}
     */
    public String[] getArrayValue()
    {
        return aValue;
    }

    /**
     * Computes the number of font families that are common between {@link #aValue} and the given array.
     * 
     * @param aValue an array of font families
     * @return the number of common font families
     */
    public int match(String[] aValue)
    {
        int match = 0;
        for (int i = 0; i < this.aValue.length; i++) {
            for (int j = 0; j < aValue.length; j++) {
                if (this.aValue[i].compareTo(aValue[j]) == 0) {
                    match++;
                    break;
                }
            }
        }
        return match;
    }
}
