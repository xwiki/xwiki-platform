package com.xpn.xwiki.plugin.charts.params;

import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class MapChartParam extends AbstractChartParam {

	public MapChartParam(String name) {
		super(name);
	}

	public MapChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Map.class;
	}

	public Object convert(String value) throws ParamException {
		return parseMap(value);
	}
}
