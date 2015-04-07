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

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.RangeType;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.VerticalAlignment;

import com.xpn.xwiki.plugin.charts.exceptions.MissingMandatoryParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ChartParams
{
    private Map paramMap = new HashMap(); // Map<String,ChartParam>

    private Map valueMap = new HashMap(); // Map<String,Object>

    private ChartParams parent;

    public static final String TYPE = "type";

    public static final String SOURCE = "source";

    public static final String SERIES = "series";

    public static final String HEIGHT = "height";

    public static final String WIDTH = "width";

    public static final String IMAGE_ATTRIBUTES = "image_attributes";

    public static final String LINK_ATTRIBUTES = "link_attributes";

    public static final String RENDERER = "renderer";

    public static final String RENDERER_COLOR = "renderer_color";

    public static final String RENDERER_STROKE = "renderer_stroke";

    public static final String RENDERER_SHAPE = "renderer_shape";

    public static final String RENDERER_FILL_COLOR = "renderer_fill_color";

    public static final String RENDERER_OUTLINE_COLOR = "renderer_outline_color";

    public static final String RENDERER_OUTLINE_STROKE = "renderer_outline_stroke";

    public static final String RENDERER_ITEM_LABEL_VISIBLE = "renderer_item_label_visible";

    public static final String RENDERER_ITEM_LABEL_COLOR = "renderer_item_label_color";

    public static final String RENDERER_ITEM_LABEL_FONT = "renderer_item_label_font";

    public static final String RENDERER_SERIES_VISIBLE = "series_visible";

    public static final String RENDERER_SERIES_VISIBLE_IN_LEGEND = "series_visible_in_legend";

    public static final String RENDERER_SERIES_COLORS = "series_colors";

    public static final String RENDERER_SERIES_STROKES = "series_strokes";

    public static final String RENDERER_SERIES_SHAPES = "series_shapes";

    public static final String RENDERER_SERIES_FILL_COLORS = "series_fill_colors";

    public static final String RENDERER_SERIES_OUTLINE_COLORS = "series_outline_colors";

    public static final String RENDERER_SERIES_OUTLINE_STROKES = "series_outline_strokes";

    public static final String RENDERER_SERIES_ITEM_LABEL_VISIBLES = "series_item_label_visibles";

    public static final String RENDERER_SERIES_ITEM_LABEL_COLORS = "series_item_label_colors";

    public static final String RENDERER_SERIES_ITEM_LABEL_FONTS = "series_item_label_fonts";

    public static final String BORDER_VISIBLE = "border_visible";

    public static final String BORDER_COLOR = "border_color";

    public static final String BORDER_STROKE = "border_stroke";

    public static final String TITLE_PREFIX = "title";

    public static final String SUBTITLE_PREFIX = "subtitle";

    public static final String TITLE_SUFFIX = "";

    public static final String TITLE_POSITION_SUFFIX = "_position";

    public static final String TITLE_HORIZONTAL_ALIGNMENT_SUFFIX = "_horizontal_alignment";

    public static final String TITLE_VERTICAL_ALIGNMENT_SUFFIX = "_vertical_alignment";

    public static final String TITLE_COLOR_SUFFIX = "_color";

    public static final String TITLE_BACKGROUND_COLOR_SUFFIX = "_background_color";

    public static final String TITLE_FONT_SUFFIX = "_font";

    public static final String TITLE_PADDING_SUFFIX = "_padding";

    public static final String TITLE_URL_SUFFIX = "_url";

    public static final String ANTI_ALIAS = "anti_alias";

    public static final String BACKGROUND_COLOR = "background_color";

    public static final String PLOT_BACKGROUND_COLOR = "plot_background_color";

    public static final String PLOT_BACKGROUND_ALPHA = "plot_background_alpha";

    public static final String PLOT_FOREGROUND_ALPHA = "plot_foreground_alpha";

    public static final String PLOT_INSERTS = "plot_inserts";

    public static final String PLOT_OUTLINE_COLOR = "plot_outline_color";

    public static final String PLOT_OUTLINE_STROKE = "plot_outline_stroke";

    public static final String XYPLOT_ORIENTATION = "plot_orientation";

    public static final String XYPLOT_QUADRANT_ORIGIN = "plot_quadrant_origin";

    public static final String XYPLOT_QUADRANT_COLORS = "plot_quadrant_colors";

    public static final String LEGEND_BACKGROUND_COLOR = "legend_background_color";

    public static final String LEGEND_ITEM_FONT = "legend_item_font";

    public static final String LEGEND_ITEM_LABEL_PADDING = "legend_item_label_padding";

    public static final String LEGEND_ITEM_GRAPHIC_ANCHOR = "legend_item_graphic_anchor";

    public static final String LEGEND_ITEM_GRAPHIC_EDGE = "legend_item_graphic_edge";

    public static final String LEGEND_ITEM_GRAPHIC_LOCATION = "legend_item_graphic_location";

    public static final String LEGEND_ITEM_GRAPHIC_PADDING = "legend_item_graphic_padding";

    public static final String AXIS_DOMAIN_PREFIX = "domain_axis_";

    public static final String AXIS_RANGE_PREFIX = "range_axis_";

    public static final String AXIS_VISIBLE_SUFIX = "visible";

    public static final String AXIS_LABEL_SUFFIX = "label";

    public static final String AXIS_LABEL_FONT_SUFFIX = "label_font";

    public static final String AXIS_LABEL_COLOR_SUFFIX = "label_color";

    public static final String AXIS_LABEL_INSERTS_SUFFIX = "label_inserts";

    public static final String AXIS_LINE_VISIBLE_SUFFIX = "line_visible";

    public static final String AXIS_LINE_COLOR_SUFFIX = "line_color";

    public static final String AXIS_LINE_STROKE_SUFFIX = "line_stroke";

    public static final String AXIS_TICK_LABEL_VISIBLE_SUFFIX = "tick_label_visible";

    public static final String AXIS_TICK_LABEL_FONT_SUFFIX = "tick_label_font";

    public static final String AXIS_TICK_LABEL_COLOR_SUFFIX = "tick_label_color";

    public static final String AXIS_TICK_LABEL_INSERTS_SUFFIX = "tick_label_inserts";

    public static final String AXIS_TICK_MARK_VISIBLE_SUFFIX = "tick_mark_visible";

    public static final String AXIS_TICK_MARK_INSIDE_LENGTH_SUFFIX = "tick_mark_inside_length";

    public static final String AXIS_TICK_MARK_OUTSIDE_LENGTH_SUFFIX = "tick_mark_outside_length";

    public static final String AXIS_TICK_MARK_COLOR_SUFFIX = "tick_mark_color";

    public static final String AXIS_TICK_MARK_STROKE_SUFFIX = "tick_mark_stroke";

    public static final String PLOTXY_AXIS_GRIDLINE_VISIBLE_SUFFIX = "gridline_visible";

    public static final String PLOTXY_AXIS_GRIDLINE_COLOR_SUFFIX = "gridline_color";

    public static final String PLOTXY_AXIS_GRIDLINE_STROKE_SUFFIX = "gridline_stroke";

    public static final String VALUE_AXIS_AUTO_RANGE_SUFFIX = "auto_range";

    public static final String VALUE_AXIS_AUTO_RANGE_MIN_SIZE_SUFFIX = "auto_range_min_size";

    public static final String VALUE_AXIS_AUTO_TICK_UNIT_SUFFIX = "auto_tick_unit";

    public static final String VALUE_AXIS_LOWER_BOUND_SUFFIX = "lower_bound";

    public static final String VALUE_AXIS_UPPER_BOUND_SUFFIX = "upper_bound";

    public static final String AXIS_LOWER_MARGIN_SUFFIX = "lower_margin";

    public static final String AXIS_UPPER_MARGIN_SUFFIX = "upper_margin";

    public static final String VALUE_AXIS_VERTICAL_TICK_LABELS_SUFFIX = "vertical_tick_labels";

    public static final String NUMBER_AXIS_AUTO_RANGE_INCLUDES_ZERO_SUFFIX = "auto_range_includes_zero";

    public static final String NUMBER_AXIS_AUTO_RANGE_STICKY_ZERO_SUFFIX = "auto_range_sticky_zero";

    public static final String NUMBER_AXIS_RANGE_TYPE_SUFFIX = "range_type";

    public static final String NUMBER_AXIS_NUMBER_TICK_UNIT_SUFFIX = "number_tick_unit";

    public static final String NUMBER_AXIS_NUMBER_FORMAT_OVERRIDE_SUFFIX = "number_format";

    public static final String DATE_AXIS_DATE_FORMAT_OVERRIDE_SUFFIX = "date_format_override";

    public static final String DATE_AXIS_UPPER_DATE_SUFFIX = "upper_date";

    public static final String DATE_AXIS_LOWER_DATE_SUFFIX = "lower_date";

    public static final String DATE_AXIS_DATE_TICK_MARK_POSITION_SUFFIX = "tick_mark_position";

    public static final String DATE_AXIS_DATE_TICK_UNIT_SUFFIX = "date_tick_unit";

    public static final String CATEGORY_AXIS_CATEGORY_MARGIN_SUFFIX = "category_margin";

    public static final String CATEGORY_AXIS_LABEL_POSITIONS_SUFFIX = "label_positions";

    public static final String CATEGORY_AXIS_LABEL_POSITION_OFFSET_SUFFIX = "label_position_offset";

    public static final String CATEGORY_AXIS_MAXIMUM_LABEL_LINES_SUFFIX = "maximum_label_lines";

    public static final String CATEGORY_AXIS_MAXIMUM_LABEL_WIDTH_RATIO_SUFFIX = "maximul_label_width_ratio";

    public static final String TIME_PERIOD_CLASS = "time_period";

    public static final String DATE_FORMAT = "date_format";

    public ChartParams()
    {
        this((ChartParams) null);
    }

    public ChartParams(Map map) throws ParamException
    {
        this(map, null, false);
    }

    public ChartParams(Map map, ChartParams parent) throws ParamException
    {
        this(map, parent, false);
    }

    public ChartParams(Map map, ChartParams parent, boolean discardNumbers) throws ParamException
    {
        this(parent);
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            String value = (String) map.get(name);
            if (discardNumbers) {
                try {
                    Integer.parseInt(name);
                } catch (NumberFormatException nfe) {
                    set(name.trim(), value.trim());
                }
            } else {
                set(name.trim(), value.trim());
            }
        }
    }

    public ChartParams(ChartParams parent)
    {
        this.parent = parent;
        addParam(new StringChartParam(TYPE, false));
        addParam(new MapChartParam(SOURCE, false));

        addParam(new IntegerChartParam(HEIGHT));
        addParam(new IntegerChartParam(WIDTH));
        addParam(new MapChartParam(LINK_ATTRIBUTES));
        addParam(new MapChartParam(IMAGE_ATTRIBUTES));

        addParam(new RendererClassChartParam(RENDERER));
        addParam(new ColorChartParam(RENDERER_COLOR));
        addParam(new StrokeChartParam(RENDERER_STROKE));
        addParam(new ShapeChartParam(RENDERER_SHAPE));
        addParam(new ColorChartParam(RENDERER_FILL_COLOR));
        addParam(new ColorChartParam(RENDERER_OUTLINE_COLOR));
        addParam(new StrokeChartParam(RENDERER_OUTLINE_STROKE));
        addParam(new BooleanChartParam(RENDERER_ITEM_LABEL_VISIBLE));
        addParam(new ColorChartParam(RENDERER_ITEM_LABEL_COLOR));
        addParam(new FontChartParam(RENDERER_ITEM_LABEL_FONT));
        addParam(new BooleanChartParam(RENDERER_SERIES_VISIBLE));
        addParam(new BooleanChartParam(RENDERER_SERIES_VISIBLE_IN_LEGEND));

        addParam(new ListChartParam(new ColorChartParam(RENDERER_SERIES_COLORS)));
        addParam(new ListChartParam(new StrokeChartParam(RENDERER_SERIES_STROKES)));
        addParam(new ListChartParam(new ShapeChartParam(RENDERER_SERIES_SHAPES)));
        addParam(new ListChartParam(new ColorChartParam(RENDERER_SERIES_FILL_COLORS)));
        addParam(new ListChartParam(new ColorChartParam(RENDERER_SERIES_OUTLINE_COLORS)));
        addParam(new ListChartParam(new StrokeChartParam(RENDERER_SERIES_OUTLINE_STROKES)));
        addParam(new ListChartParam(new BooleanChartParam(RENDERER_SERIES_ITEM_LABEL_VISIBLES)));
        addParam(new ListChartParam(new ColorChartParam(RENDERER_SERIES_ITEM_LABEL_COLORS)));
        addParam(new ListChartParam(new FontChartParam(RENDERER_SERIES_ITEM_LABEL_FONTS)));

        addParam(new BooleanChartParam(BORDER_VISIBLE));
        addParam(new ColorChartParam(BORDER_COLOR));
        addParam(new StrokeChartParam(BORDER_STROKE));

        addTitleParams(TITLE_PREFIX);
        addTitleParams(SUBTITLE_PREFIX);

        addParam(new BooleanChartParam(ANTI_ALIAS));
        addParam(new ColorChartParam(BACKGROUND_COLOR));

        addParam(new ColorChartParam(PLOT_BACKGROUND_COLOR));
        addParam(new FloatChartParam(PLOT_BACKGROUND_ALPHA));
        addParam(new FloatChartParam(PLOT_FOREGROUND_ALPHA));
        addParam(new RectangleInsetsChartParam(PLOT_INSERTS));
        addParam(new ColorChartParam(PLOT_OUTLINE_COLOR));
        addParam(new StrokeChartParam(PLOT_OUTLINE_STROKE));

        addParam(new PlotOrientationChartParam(XYPLOT_ORIENTATION));
        addParam(new Point2DChartParam(XYPLOT_QUADRANT_ORIGIN));
        addParam(new ListChartParam(new ColorChartParam(XYPLOT_QUADRANT_COLORS)));

        addParam(new ColorChartParam(LEGEND_BACKGROUND_COLOR));
        addParam(new FontChartParam(LEGEND_ITEM_FONT));
        addParam(new RectangleInsetsChartParam(LEGEND_ITEM_LABEL_PADDING));
        addParam(new RectangleAnchorChartParam(LEGEND_ITEM_GRAPHIC_ANCHOR));
        addParam(new RectangleEdgeChartParam(LEGEND_ITEM_GRAPHIC_EDGE));
        addParam(new RectangleAnchorChartParam(LEGEND_ITEM_GRAPHIC_LOCATION));
        addParam(new RectangleInsetsChartParam(LEGEND_ITEM_GRAPHIC_PADDING));

        addAxisParams(AXIS_DOMAIN_PREFIX);
        addAxisParams(AXIS_RANGE_PREFIX);

        addParam(new TimePeriodClassChartParam(TIME_PERIOD_CLASS));
        addParam(new DateFormatChartParam(DATE_FORMAT));
    }

    private void addAxisParams(String prefix)
    {
        addParam(new BooleanChartParam(prefix + AXIS_VISIBLE_SUFIX));

        addParam(new BooleanChartParam(prefix + AXIS_LINE_VISIBLE_SUFFIX));
        addParam(new ColorChartParam(prefix + AXIS_LINE_COLOR_SUFFIX));
        addParam(new StrokeChartParam(prefix + AXIS_LINE_STROKE_SUFFIX));

        addParam(new StringChartParam(prefix + AXIS_LABEL_SUFFIX));
        addParam(new FontChartParam(prefix + AXIS_LABEL_FONT_SUFFIX));
        addParam(new ColorChartParam(prefix + AXIS_LABEL_COLOR_SUFFIX));
        addParam(new RectangleInsetsChartParam(prefix + AXIS_LABEL_INSERTS_SUFFIX));

        addParam(new BooleanChartParam(prefix + AXIS_TICK_LABEL_VISIBLE_SUFFIX));
        addParam(new FontChartParam(prefix + AXIS_TICK_LABEL_FONT_SUFFIX));
        addParam(new ColorChartParam(prefix + AXIS_TICK_LABEL_COLOR_SUFFIX));
        addParam(new RectangleInsetsChartParam(prefix + AXIS_TICK_LABEL_INSERTS_SUFFIX));

        addParam(new BooleanChartParam(prefix + AXIS_TICK_MARK_VISIBLE_SUFFIX));
        addParam(new FloatChartParam(prefix + AXIS_TICK_MARK_INSIDE_LENGTH_SUFFIX));
        addParam(new FloatChartParam(prefix + AXIS_TICK_MARK_OUTSIDE_LENGTH_SUFFIX));
        addParam(new ColorChartParam(prefix + AXIS_TICK_MARK_COLOR_SUFFIX));
        addParam(new StrokeChartParam(prefix + AXIS_TICK_MARK_STROKE_SUFFIX));

        addParam(new BooleanChartParam(prefix + PLOTXY_AXIS_GRIDLINE_VISIBLE_SUFFIX));
        addParam(new ColorChartParam(prefix + PLOTXY_AXIS_GRIDLINE_COLOR_SUFFIX));
        addParam(new StrokeChartParam(prefix + PLOTXY_AXIS_GRIDLINE_STROKE_SUFFIX));

        addParam(new BooleanChartParam(prefix + VALUE_AXIS_AUTO_RANGE_SUFFIX));
        addParam(new DoubleChartParam(prefix + VALUE_AXIS_AUTO_RANGE_MIN_SIZE_SUFFIX));
        addParam(new BooleanChartParam(prefix + VALUE_AXIS_AUTO_TICK_UNIT_SUFFIX));
        addParam(new DoubleChartParam(prefix + VALUE_AXIS_LOWER_BOUND_SUFFIX));
        addParam(new DoubleChartParam(prefix + VALUE_AXIS_UPPER_BOUND_SUFFIX));
        addParam(new DoubleChartParam(prefix + AXIS_LOWER_MARGIN_SUFFIX));
        addParam(new DoubleChartParam(prefix + AXIS_UPPER_MARGIN_SUFFIX));
        addParam(new BooleanChartParam(prefix + VALUE_AXIS_VERTICAL_TICK_LABELS_SUFFIX));

        addParam(new BooleanChartParam(prefix + NUMBER_AXIS_AUTO_RANGE_INCLUDES_ZERO_SUFFIX));
        addParam(new BooleanChartParam(prefix + NUMBER_AXIS_AUTO_RANGE_STICKY_ZERO_SUFFIX));
        addParam(new RangeTypeChartParam(prefix + NUMBER_AXIS_RANGE_TYPE_SUFFIX));
        addParam(new NumberTickUnitChartParam(prefix + NUMBER_AXIS_NUMBER_TICK_UNIT_SUFFIX));
        addParam(new NumberFormatChartParam(prefix + NUMBER_AXIS_NUMBER_FORMAT_OVERRIDE_SUFFIX));

        addParam(new DateFormatChartParam(prefix + DATE_AXIS_DATE_FORMAT_OVERRIDE_SUFFIX));
        addParam(new DateChartParam(prefix + DATE_AXIS_LOWER_DATE_SUFFIX));
        addParam(new DateChartParam(prefix + DATE_AXIS_UPPER_DATE_SUFFIX));
        addParam(new DateTickMarkPositionChartParam(prefix + DATE_AXIS_DATE_TICK_MARK_POSITION_SUFFIX));
        addParam(new DateTickUnitChartParam(prefix + DATE_AXIS_DATE_TICK_UNIT_SUFFIX));

        addParam(new DoubleChartParam(prefix + CATEGORY_AXIS_CATEGORY_MARGIN_SUFFIX));
        addParam(new CategoryLabelPositionsChartParam(prefix + CATEGORY_AXIS_LABEL_POSITIONS_SUFFIX));
        addParam(new IntegerChartParam(prefix + CATEGORY_AXIS_LABEL_POSITION_OFFSET_SUFFIX));
        addParam(new IntegerChartParam(prefix + CATEGORY_AXIS_MAXIMUM_LABEL_LINES_SUFFIX));
        addParam(new FloatChartParam(prefix + CATEGORY_AXIS_MAXIMUM_LABEL_WIDTH_RATIO_SUFFIX));
    }

    private void addTitleParams(String prefix)
    {
        addParam(new StringChartParam(prefix + TITLE_SUFFIX));
        addParam(new ColorChartParam(prefix + TITLE_BACKGROUND_COLOR_SUFFIX));
        addParam(new RectangleEdgeChartParam(prefix + TITLE_POSITION_SUFFIX));
        addParam(new HorizontalAlignmentChartParam(prefix + TITLE_HORIZONTAL_ALIGNMENT_SUFFIX));
        addParam(new VerticalAlignmentChartParam(prefix + TITLE_VERTICAL_ALIGNMENT_SUFFIX));
        addParam(new ColorChartParam(prefix + TITLE_COLOR_SUFFIX));
        addParam(new FontChartParam(prefix + TITLE_FONT_SUFFIX));
        addParam(new RectangleInsetsChartParam(prefix + TITLE_PADDING_SUFFIX));
    }

    public void addParam(ChartParam param)
    {
        paramMap.put(param.getName(), param);
    }

    public void set(String name, String value) throws ParamException
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null) {
            valueMap.put(name, param.convert(value));
        } else {
            valueMap.put(name, value);
        }
    }

    protected void set(String name, Object obj) throws ParamException
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (obj == null || param.getType().isInstance(obj)) {
            valueMap.put(name, obj);
        } else {
            throw new InvalidParameterException("Invalid value type for parameter " + param.getName()
                + " ; expected type: " + param.getType());
        }
    }

    public void check() throws ParamException
    {
        Iterator it = paramMap.values().iterator();
        while (it.hasNext()) {
            ChartParam param = (ChartParam) it.next();
            if (!param.isOptional() && valueMap.get(param.getName()) == null) {
                throw new MissingMandatoryParamException("No value given for mandatory parameter " + param.getName());
            }
        }
    }

    public Object get(String name)
    {
        Object result = valueMap.get(name);
        if (result != null) {
            return result;
        } else if (parent != null) {
            return parent.get(name);
        } else {
            return null;
        }
    }

    public String getString(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param == null || param.getType() == String.class) {
            return (String) get(name);
        } else {
            return null;
        }
    }

    public Integer getInteger(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Integer.class) {
            return (Integer) get(name);
        } else {
            return null;
        }
    }

    public Float getFloat(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Float.class) {
            return (Float) get(name);
        } else {
            return null;
        }
    }

    public Double getDouble(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Double.class) {
            return (Double) get(name);
        } else {
            return null;
        }
    }

    public Boolean getBoolean(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Boolean.class) {
            return (Boolean) get(name);
        } else {
            return null;
        }
    }

    public Color getColor(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Color.class) {
            return (Color) get(name);
        } else {
            return null;
        }
    }

    public Stroke getStroke(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Stroke.class) {
            return (Stroke) get(name);
        } else {
            return null;
        }
    }

    public RectangleEdge getRectangleEdge(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == RectangleEdge.class) {
            return (RectangleEdge) get(name);
        } else {
            return null;
        }
    }

    public HorizontalAlignment getHorizontalAlignment(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == HorizontalAlignment.class) {
            return (HorizontalAlignment) get(name);
        } else {
            return null;
        }
    }

    public VerticalAlignment getVerticalAlignment(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == VerticalAlignment.class) {
            return (VerticalAlignment) get(name);
        } else {
            return null;
        }
    }

    public Font getFont(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Font.class) {
            return (Font) get(name);
        } else {
            return null;
        }
    }

    public RectangleInsets getRectangleInsets(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == RectangleInsets.class) {
            return (RectangleInsets) get(name);
        } else {
            return null;
        }
    }

    public RectangleAnchor getRectangleAnchor(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == RectangleAnchor.class) {
            return (RectangleAnchor) get(name);
        } else {
            return null;
        }
    }

    public PlotOrientation getPlotOrientation(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == PlotOrientation.class) {
            return (PlotOrientation) get(name);
        } else {
            return null;
        }
    }

    public Point2D getPoint2D(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Point2D.class) {
            return (Point2D) get(name);
        } else {
            return null;
        }
    }

    public Shape getShape(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Shape.class) {
            return (Shape) get(name);
        } else {
            return null;
        }
    }

    // public Range getRange(String name) {
    // ChartParam param = (ChartParam)paramMap.get(name);
    // if (param != null && param.getType() == Range.class) {
    // return (Range)get(name);
    // } else {
    // return null;
    // }
    // }

    public RangeType getRangeType(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == RangeType.class) {
            return (RangeType) get(name);
        } else {
            return null;
        }
    }

    public NumberTickUnit getNumberTickUnit(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == NumberTickUnit.class) {
            return (NumberTickUnit) get(name);
        } else {
            return null;
        }
    }

    public NumberFormat getNumberFormat(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == NumberFormat.class) {
            return (NumberFormat) get(name);
        } else {
            return null;
        }
    }

    public DateFormat getDateFormat(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == DateFormat.class) {
            return (DateFormat) get(name);
        } else {
            return null;
        }
    }

    public Date getDate(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Date.class) {
            return (Date) get(name);
        } else {
            return null;
        }
    }

    public DateTickMarkPosition getDateTickMarkPosition(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == DateTickMarkPosition.class) {
            return (DateTickMarkPosition) get(name);
        } else {
            return null;
        }
    }

    public DateTickUnit getDateTickUnit(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == DateTickUnit.class) {
            return (DateTickUnit) get(name);
        } else {
            return null;
        }
    }

    public Class getClass(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Class.class) {
            return (Class) get(name);
        } else {
            return null;
        }
    }

    public CategoryLabelPositions getCategoryLabelPositions(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == CategoryLabelPositions.class) {
            return (CategoryLabelPositions) get(name);
        } else {
            return null;
        }
    }

    public List getList(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == List.class) {
            return (List) get(name);
        } else {
            return null;
        }
    }

    public Map getMap(String name)
    {
        ChartParam param = (ChartParam) paramMap.get(name);
        if (param != null && param.getType() == Map.class) {
            return (Map) get(name);
        } else {
            return null;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        Iterator it = valueMap.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            sb.append(name + "=");
            sb.append(valueMap.get(name).toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
