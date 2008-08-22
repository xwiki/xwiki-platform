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

public class FontFamily
{
    private String name;

    private String sValue;

    private String[] aValue;

    public FontFamily(String name, String sValue)
    {
        this.name = name;
        this.sValue = sValue;
        this.aValue = sValue.split(",");
    }

    public String getName()
    {
        return name;
    }

    public String getStringValue()
    {
        return sValue;
    }

    public String[] getArrayValue()
    {
        return aValue;
    }

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
