package com.xpn.xwiki.plugin.charts.params;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ColorsChartParam extends AbstractChartParam {

	public ColorsChartParam(String name) {
		super(name);
	}

	public ColorsChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Color[].class;
	}

	public Object convert(String value) throws ParamException {
		List list = parseList(value);
		Color[] colors = new Color[list.size()];
		ColorChartParam colorParam = new ColorChartParam("internal_use");
		Iterator it = list.iterator(); int i = 0;
		while (it.hasNext()) {
			String s = (String)it.next();
			colors[i] = (Color)colorParam.convert(s);
			i++;
		}
		return colors;
	}
}
