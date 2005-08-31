package com.xpn.xwiki.plugin.charts.params;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class FontChartParam extends AbstractChartParam {
	private Map styleChoices = new HashMap();

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
		styleChoices.put("plain", new Integer(Font.PLAIN));
		styleChoices.put("bold", new Integer(Font.BOLD));
		styleChoices.put("italic", new Integer(Font.ITALIC));
		styleChoices.put("bold+italic", new Integer(Font.BOLD+Font.ITALIC));
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value, 3);
		return new Font(getStringArg(map, "name"),
				getStyleParam(map, "style"), getIntArg(map, "size"));
	}
	
	private int getStyleParam(Map map, String name) throws ParamException {
		return ((Integer)getChoiceArg(map, name, styleChoices)).intValue();
	}	
}
