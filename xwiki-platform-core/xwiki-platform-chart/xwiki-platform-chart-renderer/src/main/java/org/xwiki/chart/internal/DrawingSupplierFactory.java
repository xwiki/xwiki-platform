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
package org.xwiki.chart.internal;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.xwiki.chart.ChartGenerator;

/**
 * Allows user to provide custom colors.
 *
 * @version $Id$
 * @since 4.1M2
 */
public class DrawingSupplierFactory
{
    /**
     * @param parameters the user-defined parameters, containing {@link ChartGenerator#COLORS_PARAM} if the user
     *        wants custom colors
     * @return the Drawing Supplier to use
     */
    public DrawingSupplier createDrawingSupplier(Map<String, String> parameters)
    {
        DrawingSupplier supplier;

        String colorParam = parameters.get(ChartGenerator.COLORS_PARAM);
        if (colorParam != null) {
            List<Color> colors = new ArrayList<Color>();
            for (String colorAsString : colorParam.split(",")) {
                if (colorAsString.length() == 6) {
                    int red = Integer.parseInt(colorAsString.substring(0, 2), 16);
                    int green = Integer.parseInt(colorAsString.substring(2, 4), 16);
                    int blue = Integer.parseInt(colorAsString.substring(4, 6), 16);
                    colors.add(new Color(red, green, blue));
                }
            }
            Paint[] paint = new Paint[colors.size()];
            int i = 0;
            for (Color color : colors) {
                paint[i++] = color;
            }
            supplier = new DefaultDrawingSupplier(paint,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
        } else {
            supplier = new DefaultDrawingSupplier();
        }

        return supplier;
    }
}
