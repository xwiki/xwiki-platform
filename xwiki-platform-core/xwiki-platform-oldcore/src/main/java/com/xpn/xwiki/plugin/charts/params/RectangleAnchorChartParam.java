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
package com.xpn.xwiki.plugin.charts.params;

import org.jfree.ui.RectangleAnchor;

public class RectangleAnchorChartParam extends ChoiceChartParam
{
    public RectangleAnchorChartParam(String name)
    {
        super(name);
    }

    public RectangleAnchorChartParam(String name, boolean isOptional)
    {
        super(name, isOptional);
    }

    @Override
    protected void init()
    {
        addChoice("bottom", RectangleAnchor.BOTTOM);
        addChoice("bottom-left", RectangleAnchor.BOTTOM_LEFT);
        addChoice("bottom-right", RectangleAnchor.BOTTOM_RIGHT);
        addChoice("center", RectangleAnchor.CENTER);
        addChoice("left", RectangleAnchor.LEFT);
        addChoice("right", RectangleAnchor.RIGHT);
        addChoice("top", RectangleAnchor.TOP);
        addChoice("top-left", RectangleAnchor.TOP_LEFT);
        addChoice("top-right", RectangleAnchor.TOP_RIGHT);
    }

    @Override
    public Class getType()
    {
        return RectangleAnchor.class;
    }
}
