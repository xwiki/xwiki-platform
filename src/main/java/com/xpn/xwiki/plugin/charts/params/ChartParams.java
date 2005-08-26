package com.xpn.xwiki.plugin.charts.params;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.VerticalAlignment;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ChartParams {
	private Map paramMap = new HashMap(); // Map<String,ChartParam>
	private Map valueMap = new HashMap(); // Map<String,Object>
	private ChartParams parent;
	
	public static final String TITLE = "title";
	public static final String TYPE = "type";
	public static final String SOURCE = "source";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	public static final String SERIES = "series";
	public static final String RENDERER = "renderer";
	
	public static final String DOMAIN_AXIS_LABEL = "domain_axis_label";
	public static final String RANGE_AXIS_LABEL = "range_axis_label";
	
	public static final String BORDER_VISIBLE = "border_visible";
	public static final String BORDER_COLOR = "border_color";
	public static final String BORDER_STROKE = "border_stroke";
	
	public static final String TITLE_POSITION = "title_position";
	public static final String TITLE_HORIZONTAL_ALIGNMENT = "title_horizontal_alignment";
	public static final String TITLE_VERTICAL_ALIGNMENT = "title_vertical_alignment";
	public static final String TITLE_COLOR = "title_color";
	public static final String TITLE_BACKGROUND_COLOR = "title_background_color";
	public static final String TITLE_FONT = "title_font";
	public static final String TITLE_PADDING = "title_padding";
	public static final String TITLE_URL = "title_url";
	
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
	
	public ChartParams() {
		this(null);
	}
	
	public ChartParams(ChartParams parent) {
		this.parent = parent;
		addParam(new StringChartParam(TYPE, false));
		addParam(new StringChartParam(SOURCE, false));
		addParam(new StringChartParam(TITLE));
		addParam(new IntegerChartParam(WIDTH));
		addParam(new IntegerChartParam(HEIGHT));
		addParam(new StringChartParam(RENDERER));
		addParam(new BooleanChartParam(BORDER_VISIBLE));
		addParam(new ColorChartParam(BORDER_COLOR));
		addParam(new ColorChartParam(TITLE_BACKGROUND_COLOR));
		addParam(new StrokeChartParam(BORDER_STROKE));
		addParam(new RectangleEdgeChartParam(TITLE_POSITION));
		addParam(new HorizontalAlignmentChartParam(TITLE_HORIZONTAL_ALIGNMENT));
		addParam(new VerticalAlignmentChartParam(TITLE_VERTICAL_ALIGNMENT));	
		addParam(new ColorChartParam(TITLE_COLOR));
		addParam(new FontChartParam(TITLE_FONT));
		addParam(new RectangleInsetsChartParam(TITLE_PADDING));
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
}
