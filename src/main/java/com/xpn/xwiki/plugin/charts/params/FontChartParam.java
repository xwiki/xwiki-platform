package com.xpn.xwiki.plugin.charts.params;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class FontChartParam extends AbstractChartParam {
	private Map choices = new HashMap();

	public FontChartParam(String name) {
		super(name);
		init();
	}

	public FontChartParam(String name, boolean optional) {
		super(name, optional);
		init();
	}

	public Class getType() {
		return Font.class;
	}
	
	public void init() {
		choices.put("plain", new Integer(Font.PLAIN));
		choices.put("bold", new Integer(Font.BOLD));
		choices.put("italic", new Integer(Font.ITALIC));
		choices.put("bold+italic", new Integer(Font.BOLD+Font.ITALIC));
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value, 3);
		return new Font(getStringParam(map, "name"),
				getStyleParam(map, "style"), getIntParam(map, "size"));
	}
	
	private int getStyleParam(Map map, String name) throws ParamException {
		return ((Integer)getChoiceParam(map, name, choices)).intValue();
	}	
}
