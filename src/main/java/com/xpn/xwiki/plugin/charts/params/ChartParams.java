package com.xpn.xwiki.plugin.charts.params;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.VerticalAlignment;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ChartParams {
	private Map paramMap = new HashMap(); // Map<String,ChartParam>
	private Map valueMap = new HashMap(); // Map<String,Object>
	private ChartParams parent;
	
	public static final String TYPE = "type";
	public static final String SOURCE = "source";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String SERIES = "series";
	public static final String RENDERER = "renderer";
	
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
	public static final String PLOT_ZOOM = "plot_zoom";
	
	public static final String LEGEND_BACKGROUND_COLOR = "legend_background_color";
	public static final String LEGEND_ITEM_FONT = "legend_item_font";
	public static final String LEGEND_ITEM_LABEL_PADDING = "legend_item_label_padding";
	public static final String LEGEND_ITEM_GRAPHIC_ANCHOR = "legend_item_graphic_anchor";
	public static final String LEGEND_ITEM_GRAPHIC_EDGE = "legend_item_graphic_edge";
	public static final String LEGEND_ITEM_GRAPHIC_LOCATION = "legend_item_graphic_location";
	public static final String LEGEND_ITEM_GRAPHIC_PADDING = "legend_item_graphic_padding";

	public static final String AXIS_DOMAIN_PREFIX = "axis_domain_";
	public static final String AXIS_RANGE_PREFIX = "axis_range_";
	
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
	
	public ChartParams() {
		this((ChartParams)null);
	}

	public ChartParams(Map map) throws ParamException {
		this(map, null, false);
	}
	
	public ChartParams(Map map, ChartParams parent) throws ParamException {
		this(map, parent, false);
	}
	
	public ChartParams(Map map, ChartParams parent,
			boolean discardNumbers) throws ParamException {
		this(parent);
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String name = (String)it.next();
			String value = (String)map.get(name);
			if (discardNumbers) {
	            try {
	    	        Integer.parseInt(name);
	            } catch (NumberFormatException nfe) {
	    	        set(name, value);	            	
	            }
			} else {
				set(name, value);				
			}
		}		
	}
	
	public ChartParams(ChartParams parent) {
		this.parent = parent;
		addParam(new StringChartParam(TYPE, false));
		addParam(new StringChartParam(SOURCE, false));
		addParam(new IntegerChartParam(WIDTH));
		addParam(new IntegerChartParam(HEIGHT));
		addParam(new StringChartParam(RENDERER));
		
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
		addParam(new DoubleChartParam(PLOT_ZOOM));

		addParam(new ColorChartParam(LEGEND_BACKGROUND_COLOR));
		addParam(new FontChartParam(LEGEND_ITEM_FONT));
		addParam(new RectangleInsetsChartParam(LEGEND_ITEM_LABEL_PADDING));
		addParam(new RectangleAnchorChartParam(LEGEND_ITEM_GRAPHIC_ANCHOR));
		addParam(new RectangleEdgeChartParam(LEGEND_ITEM_GRAPHIC_EDGE));
		addParam(new RectangleAnchorChartParam(LEGEND_ITEM_GRAPHIC_LOCATION));
		addParam(new RectangleInsetsChartParam(LEGEND_ITEM_GRAPHIC_PADDING));

		addAxisParams(AXIS_DOMAIN_PREFIX);
		addAxisParams(AXIS_RANGE_PREFIX);
	}
	
	// 16x2
	private void addAxisParams(String prefix) {
		addParam(new BooleanChartParam(prefix+AXIS_VISIBLE_SUFIX));
		
		addParam(new BooleanChartParam(prefix+AXIS_LINE_VISIBLE_SUFFIX));
		addParam(new ColorChartParam(prefix+AXIS_LINE_COLOR_SUFFIX));
		addParam(new StrokeChartParam(prefix+AXIS_LINE_STROKE_SUFFIX));
		
		addParam(new StringChartParam(prefix+AXIS_LABEL_SUFFIX));
		addParam(new FontChartParam(prefix+AXIS_LABEL_FONT_SUFFIX));
		addParam(new ColorChartParam(prefix+AXIS_LABEL_COLOR_SUFFIX));
		addParam(new RectangleInsetsChartParam(prefix+AXIS_LABEL_INSERTS_SUFFIX));		

		addParam(new BooleanChartParam(prefix+AXIS_TICK_LABEL_VISIBLE_SUFFIX));
		addParam(new FontChartParam(prefix+AXIS_TICK_LABEL_FONT_SUFFIX));
		addParam(new ColorChartParam(prefix+AXIS_TICK_LABEL_COLOR_SUFFIX));
		addParam(new RectangleInsetsChartParam(prefix+AXIS_TICK_LABEL_INSERTS_SUFFIX));
		
		addParam(new BooleanChartParam(prefix+AXIS_TICK_MARK_VISIBLE_SUFFIX));
		addParam(new FloatChartParam(prefix+AXIS_TICK_MARK_INSIDE_LENGTH_SUFFIX));
		addParam(new FloatChartParam(prefix+AXIS_TICK_MARK_OUTSIDE_LENGTH_SUFFIX));
		addParam(new ColorChartParam(prefix+AXIS_TICK_MARK_COLOR_SUFFIX));
		addParam(new StrokeChartParam(prefix+AXIS_TICK_MARK_STROKE_SUFFIX));
	}
	
	//8x2
	private void addTitleParams(String prefix) {
		addParam(new StringChartParam(prefix+TITLE_SUFFIX));
		addParam(new ColorChartParam(prefix+TITLE_BACKGROUND_COLOR_SUFFIX));
		addParam(new RectangleEdgeChartParam(prefix+TITLE_POSITION_SUFFIX));
		addParam(new HorizontalAlignmentChartParam(prefix+TITLE_HORIZONTAL_ALIGNMENT_SUFFIX));
		addParam(new VerticalAlignmentChartParam(prefix+TITLE_VERTICAL_ALIGNMENT_SUFFIX));	
		addParam(new ColorChartParam(prefix+TITLE_COLOR_SUFFIX));
		addParam(new FontChartParam(prefix+TITLE_FONT_SUFFIX));
		addParam(new RectangleInsetsChartParam(prefix+TITLE_PADDING_SUFFIX));
	}

	public void addParam(ChartParam param) {
		paramMap.put(param.getName(), param);
	}
	
	public void set(String name, String value) throws ParamException {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null) {
			valueMap.put(name, param.convert(value));
		} else {
			valueMap.put(name, value);
		}
	}
	
	public void check() throws ParamException {
		// TODO:
	}
	
	public Object get(String name) {
		Object result = valueMap.get(name);
		if (result != null) {
			return result;
		} else if (parent != null) {
			return parent.get(name);			
		} else {
			return null;
		}	
	}
	
	public String getString(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param == null || param.getType() == String.class) {
			return (String)get(name);
		} else {
			return null;
		}
	}
	
	public Integer getInteger(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Integer.class) {
			return (Integer)get(name);
		} else {
			return null;
		}
	}
	
	public Float getFloat(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Float.class) {
			return (Float)get(name);
		} else {
			return null;
		}
	}
	
	public Double getDouble(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Double.class) {
			return (Double)get(name);
		} else {
			return null;
		}
	}
	
	public Boolean getBoolean(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Boolean.class) {
			return (Boolean)get(name);
		} else {
			return null;
		}
	}
	
	public Color getColor(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Color.class) {
			return (Color)get(name);
		} else {
			return null;
		}
	}
	
	public Stroke getStroke(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Stroke.class) {
			return (Stroke)get(name);
		} else {
			return null;
		}
	}
	
	public RectangleEdge getRectangleEdge(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == RectangleEdge.class) {
			return (RectangleEdge)get(name);
		} else {
			return null;
		}
	}
	
	public HorizontalAlignment getHorizontalAlignment(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == HorizontalAlignment.class) {
			return (HorizontalAlignment)get(name);
		} else {
			return null;
		}
	}
	
	public VerticalAlignment getVerticalAlignment(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == VerticalAlignment.class) {
			return (VerticalAlignment)get(name);
		} else {
			return null;
		}
	}
	
	public Font getFont(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == Font.class) {
			return (Font)get(name);
		} else {
			return null;
		}		
	}
	
	public RectangleInsets getRectangleInsets(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == RectangleInsets.class) {
			return (RectangleInsets)get(name);
		} else {
			return null;
		}
	}
	
	public RectangleAnchor getRectangleAnchor(String name) {
		ChartParam param = (ChartParam)paramMap.get(name);
		if (param != null && param.getType() == RectangleAnchor.class) {
			return (RectangleAnchor)get(name);
		} else {
			return null;
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		Iterator it = valueMap.keySet().iterator();
		while (it.hasNext()) {
			String name = (String)it.next();
			sb.append(name+"=");
			sb.append(valueMap.get(name).toString());;
			sb.append("\n");
		}
		return sb.toString();
	}
}
