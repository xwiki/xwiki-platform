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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.RangeType;
import org.jfree.data.time.Day;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

/**
 * This is used by the charting wizard only Q: Why is this not used in the ChartingMacro ?
 */
public class DefaultChartParams2 extends ChartParams
{
    public static DefaultChartParams2 uniqueInstance;

    protected DefaultChartParams2() throws ParamException
    {
        set(SERIES, "columns");

        set(HEIGHT, new Integer(600));
        set(WIDTH, new Integer(500));
        set(LINK_ATTRIBUTES, new HashMap());
        set(IMAGE_ATTRIBUTES, new HashMap());

        set(RENDERER, (Class) null); // default renderer is chart type dependent
        set(RENDERER_COLOR, AbstractRenderer.DEFAULT_PAINT);
        set(RENDERER_STROKE, AbstractRenderer.DEFAULT_STROKE);
        set(RENDERER_SHAPE, AbstractRenderer.DEFAULT_SHAPE);
        set(RENDERER_FILL_COLOR, Color.white);
        set(RENDERER_OUTLINE_COLOR, AbstractRenderer.DEFAULT_OUTLINE_PAINT);
        set(RENDERER_OUTLINE_STROKE, AbstractRenderer.DEFAULT_OUTLINE_STROKE);
        set(RENDERER_ITEM_LABEL_VISIBLE, Boolean.FALSE);
        set(RENDERER_ITEM_LABEL_COLOR, AbstractRenderer.DEFAULT_VALUE_LABEL_PAINT);
        set(RENDERER_ITEM_LABEL_FONT, AbstractRenderer.DEFAULT_VALUE_LABEL_FONT);
        set(RENDERER_SERIES_VISIBLE, Boolean.TRUE);
        set(RENDERER_SERIES_VISIBLE_IN_LEGEND, Boolean.TRUE);

        set(BORDER_VISIBLE, Boolean.FALSE);
        set(BORDER_COLOR, Color.black);
        set(BORDER_STROKE, new BasicStroke(1.0f));

        setTitle(TITLE_PREFIX);
        set(TITLE_PREFIX + TITLE_FONT_SUFFIX, JFreeChart.DEFAULT_TITLE_FONT);
        setTitle(SUBTITLE_PREFIX);

        set(ANTI_ALIAS, Boolean.TRUE);
        set(BACKGROUND_COLOR, JFreeChart.DEFAULT_BACKGROUND_PAINT);

        set(PLOT_BACKGROUND_COLOR, Plot.DEFAULT_BACKGROUND_PAINT);
        set(PLOT_BACKGROUND_ALPHA, new Float(Plot.DEFAULT_BACKGROUND_ALPHA));
        set(PLOT_FOREGROUND_ALPHA, new Float(Plot.DEFAULT_FOREGROUND_ALPHA));
        set(PLOT_INSERTS, Plot.DEFAULT_INSETS);
        set(PLOT_OUTLINE_COLOR, Plot.DEFAULT_OUTLINE_PAINT);
        set(PLOT_OUTLINE_STROKE, Plot.DEFAULT_OUTLINE_STROKE);

        set(XYPLOT_ORIENTATION, PlotOrientation.VERTICAL);
        set(XYPLOT_QUADRANT_ORIGIN, new Point2D.Double(0.0, 0.0));
        List colors = new LinkedList();
        colors.add(null);
        colors.add(null);
        colors.add(null);
        colors.add(null);
        set(XYPLOT_QUADRANT_COLORS, colors);

        set(LEGEND_BACKGROUND_COLOR, (Color) null);
        set(LEGEND_ITEM_FONT, LegendTitle.DEFAULT_ITEM_FONT);
        set(LEGEND_ITEM_LABEL_PADDING, new RectangleInsets(2.0, 2.0, 2.0, 2.0));
        set(LEGEND_ITEM_GRAPHIC_ANCHOR, RectangleAnchor.CENTER);
        set(LEGEND_ITEM_GRAPHIC_EDGE, RectangleEdge.LEFT);
        set(LEGEND_ITEM_GRAPHIC_LOCATION, RectangleAnchor.CENTER);
        set(LEGEND_ITEM_GRAPHIC_PADDING, new RectangleInsets(2.0, 2.0, 2.0, 2.0));

        setAxis(AXIS_DOMAIN_PREFIX);
        setAxis(AXIS_RANGE_PREFIX);

        set(TIME_PERIOD_CLASS, Day.class);
        set(DATE_FORMAT, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
    }

    void setTitle(String prefix) throws ParamException
    {
        set(prefix + TITLE_SUFFIX, ""); // ?
        set(prefix + TITLE_BACKGROUND_COLOR_SUFFIX, (Color) null);
        set(prefix + TITLE_POSITION_SUFFIX, Title.DEFAULT_POSITION);
        set(prefix + TITLE_HORIZONTAL_ALIGNMENT_SUFFIX, Title.DEFAULT_HORIZONTAL_ALIGNMENT);
        set(prefix + TITLE_VERTICAL_ALIGNMENT_SUFFIX, Title.DEFAULT_VERTICAL_ALIGNMENT);
        set(prefix + TITLE_COLOR_SUFFIX, TextTitle.DEFAULT_TEXT_PAINT);
        set(prefix + TITLE_FONT_SUFFIX, TextTitle.DEFAULT_FONT);
        set(prefix + TITLE_PADDING_SUFFIX, Title.DEFAULT_PADDING);
    }

    void setAxis(String prefix) throws ParamException
    {
        set(prefix + AXIS_VISIBLE_SUFIX, Axis.DEFAULT_AXIS_VISIBLE);

        set(prefix + AXIS_LINE_VISIBLE_SUFFIX, Boolean.TRUE);
        set(prefix + AXIS_LINE_COLOR_SUFFIX, Axis.DEFAULT_AXIS_LINE_PAINT);
        set(prefix + AXIS_LINE_STROKE_SUFFIX, Axis.DEFAULT_AXIS_LINE_STROKE);

        set(prefix + AXIS_LABEL_SUFFIX, ""); // ?
        set(prefix + AXIS_LABEL_FONT_SUFFIX, Axis.DEFAULT_AXIS_LABEL_FONT);
        set(prefix + AXIS_LABEL_COLOR_SUFFIX, Axis.DEFAULT_AXIS_LABEL_PAINT);
        set(prefix + AXIS_LABEL_INSERTS_SUFFIX, Axis.DEFAULT_AXIS_LABEL_INSETS);

        set(prefix + AXIS_TICK_LABEL_VISIBLE_SUFFIX, new Boolean(Axis.DEFAULT_TICK_LABELS_VISIBLE));
        set(prefix + AXIS_TICK_LABEL_FONT_SUFFIX, Axis.DEFAULT_TICK_LABEL_FONT);
        set(prefix + AXIS_TICK_LABEL_COLOR_SUFFIX, Axis.DEFAULT_TICK_LABEL_PAINT);
        set(prefix + AXIS_TICK_LABEL_INSERTS_SUFFIX, Axis.DEFAULT_TICK_LABEL_INSETS);

        set(prefix + AXIS_TICK_MARK_VISIBLE_SUFFIX, new Boolean(Axis.DEFAULT_TICK_MARKS_VISIBLE));
        set(prefix + AXIS_TICK_MARK_INSIDE_LENGTH_SUFFIX, new Float(Axis.DEFAULT_TICK_MARK_INSIDE_LENGTH));
        set(prefix + AXIS_TICK_MARK_OUTSIDE_LENGTH_SUFFIX, new Float(Axis.DEFAULT_TICK_MARK_OUTSIDE_LENGTH));
        set(prefix + AXIS_TICK_MARK_COLOR_SUFFIX, Axis.DEFAULT_TICK_MARK_PAINT);
        set(prefix + AXIS_TICK_MARK_STROKE_SUFFIX, Axis.DEFAULT_TICK_MARK_STROKE);

        set(prefix + PLOTXY_AXIS_GRIDLINE_VISIBLE_SUFFIX, Boolean.TRUE);
        set(prefix + PLOTXY_AXIS_GRIDLINE_COLOR_SUFFIX, XYPlot.DEFAULT_GRIDLINE_PAINT);
        set(prefix + PLOTXY_AXIS_GRIDLINE_STROKE_SUFFIX, XYPlot.DEFAULT_GRIDLINE_STROKE);

        set(prefix + VALUE_AXIS_AUTO_RANGE_SUFFIX, new Boolean(ValueAxis.DEFAULT_AUTO_RANGE));
        set(prefix + VALUE_AXIS_AUTO_RANGE_MIN_SIZE_SUFFIX, new Double(ValueAxis.DEFAULT_AUTO_RANGE_MINIMUM_SIZE));
        set(prefix + VALUE_AXIS_AUTO_TICK_UNIT_SUFFIX, new Boolean(ValueAxis.DEFAULT_AUTO_TICK_UNIT_SELECTION));
        set(prefix + VALUE_AXIS_LOWER_BOUND_SUFFIX, new Double(ValueAxis.DEFAULT_LOWER_BOUND));
        set(prefix + VALUE_AXIS_UPPER_BOUND_SUFFIX, new Double(ValueAxis.DEFAULT_UPPER_BOUND));
        set(prefix + AXIS_LOWER_MARGIN_SUFFIX, new Double(ValueAxis.DEFAULT_LOWER_MARGIN));
        set(prefix + AXIS_UPPER_MARGIN_SUFFIX, new Double(ValueAxis.DEFAULT_UPPER_MARGIN));
        set(prefix + VALUE_AXIS_VERTICAL_TICK_LABELS_SUFFIX, Boolean.FALSE);

        set(prefix + NUMBER_AXIS_AUTO_RANGE_INCLUDES_ZERO_SUFFIX, new Boolean(
            NumberAxis.DEFAULT_AUTO_RANGE_INCLUDES_ZERO));
        set(prefix + NUMBER_AXIS_AUTO_RANGE_STICKY_ZERO_SUFFIX, new Boolean(NumberAxis.DEFAULT_AUTO_RANGE_STICKY_ZERO));
        set(prefix + NUMBER_AXIS_RANGE_TYPE_SUFFIX, RangeType.FULL);
        set(prefix + NUMBER_AXIS_NUMBER_TICK_UNIT_SUFFIX, NumberAxis.DEFAULT_TICK_UNIT);
        set(prefix + NUMBER_AXIS_NUMBER_FORMAT_OVERRIDE_SUFFIX, (NumberFormat) null);

        set(prefix + DATE_AXIS_DATE_FORMAT_OVERRIDE_SUFFIX, (DateFormat) null);
        set(prefix + DATE_AXIS_LOWER_DATE_SUFFIX, DateAxis.DEFAULT_DATE_RANGE.getLowerDate());
        set(prefix + DATE_AXIS_UPPER_DATE_SUFFIX, DateAxis.DEFAULT_DATE_RANGE.getUpperDate());
        set(prefix + DATE_AXIS_DATE_TICK_MARK_POSITION_SUFFIX, DateTickMarkPosition.START);
        set(prefix + DATE_AXIS_DATE_TICK_UNIT_SUFFIX, DateAxis.DEFAULT_DATE_TICK_UNIT);

        set(prefix + CATEGORY_AXIS_CATEGORY_MARGIN_SUFFIX, new Double(CategoryAxis.DEFAULT_CATEGORY_MARGIN));
        set(prefix + CATEGORY_AXIS_LABEL_POSITIONS_SUFFIX, CategoryLabelPositions.STANDARD);
        set(prefix + CATEGORY_AXIS_LABEL_POSITION_OFFSET_SUFFIX, new Integer(4));
        set(prefix + CATEGORY_AXIS_MAXIMUM_LABEL_LINES_SUFFIX, new Integer(1));
        set(prefix + CATEGORY_AXIS_MAXIMUM_LABEL_WIDTH_RATIO_SUFFIX, new Float(0.0f));
    }

    /*
     * // DefaultDrawingSupplier addParam(new ListChartParam(new ColorChartParam(RENDERER_SERIES_COLORS))); addParam(new
     * ListChartParam(new StrokeChartParam(RENDERER_SERIES_STROKES))); addParam(new ListChartParam(new
     * ShapeChartParam(RENDERER_SERIES_SHAPES))); addParam(new ListChartParam(new
     * ColorChartParam(RENDERER_SERIES_FILL_COLORS))); addParam(new ListChartParam(new
     * ColorChartParam(RENDERER_SERIES_OUTLINE_COLORS))); addParam(new ListChartParam(new
     * StrokeChartParam(RENDERER_SERIES_OUTLINE_STROKES))); addParam(new ListChartParam(new
     * BooleanChartParam(RENDERER_SERIES_ITEM_LABEL_VISIBLES))); addParam(new ListChartParam(new
     * ColorChartParam(RENDERER_SERIES_ITEM_LABEL_COLORS))); addParam(new ListChartParam(new
     * FontChartParam(RENDERER_SERIES_ITEM_LABEL_FONTS)));
     */

    public static synchronized DefaultChartParams2 getInstance() throws ParamException
    {
        if (uniqueInstance == null) {
            uniqueInstance = new DefaultChartParams2();
        }
        return uniqueInstance;
    }
}
