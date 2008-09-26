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
 *
 */
package com.xpn.xwiki.plugin.charts.params;

import java.awt.Color;
import java.awt.Shape;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ColorChartParam extends AbstractChartParam
{
    private ChartParam colorChoice;

    public ColorChartParam(String name)
    {
        super(name);
        init();
    }

    public ColorChartParam(String name, boolean optional)
    {
        super(name, optional);
        init();
    }

    @Override
    public Class getType()
    {
        return Color.class;
    }

    public void init()
    {
        colorChoice = new ChoiceChartParam(getName())
        {
            @Override
            protected void init()
            {
                addChoice("black", new Color(0x000000));
                addChoice("silver", new Color(0xC0C0C0));
                addChoice("gray", new Color(0x808080));
                addChoice("white", new Color(0xFFFFFF));
                addChoice("maroon", new Color(0x800000));
                addChoice("red", new Color(0xFF0000));
                addChoice("purple", new Color(0x800080));
                addChoice("fuchsia", new Color(0xFF00FF));
                addChoice("green", new Color(0x008000));
                addChoice("lime", new Color(0x00FF00));
                addChoice("olive", new Color(0x808000));
                addChoice("yellow", new Color(0xFFFF00));
                addChoice("navy", new Color(0x000080));
                addChoice("blue", new Color(0x0000FF));
                addChoice("teal", new Color(0x008080));
                addChoice("aqua", new Color(0x00FFFF));
                addChoice("orange", new Color(0xFFA500));
                addChoice("transparent", new Color(0, 0, 0, 0));
            }

            @Override
            public Class getType()
            {
                return Shape.class;
            }
        };
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        try {
            return colorChoice.convert(value);
        } catch (ParamException e) {
            if (value.length() == 0) {
                throw new InvalidParamException("Empty color parameter " + getName());
            }
            if (value.charAt(0) == '#') {
                value = value.substring(1);
                int intValue;
                try {
                    intValue = Integer.parseInt(value, 16);
                } catch (NumberFormatException nfe) {
                    throw new InvalidParamException("Color parameter " + getName()
                        + " is not a valid hexadecimal number");
                }
                return new Color(intValue);
            } else {
                Map map = parseMap(value, 4);
                try {
                    return new Color(getIntArg(map, "red"), getIntArg(map, "green"), getIntArg(map, "blue"), getIntArg(
                        map, "alpha"));
                } catch (IllegalArgumentException iae) {
                    throw new InvalidParamException("Color component out of range (0-255)");
                }
            }
        }
    }
}
