package com.xpn.xwiki.plugin.charts.params;

import java.util.Map;

import org.jfree.ui.RectangleInsets;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class RectangleInsetsChartParam extends AbstractChartParam {

	public RectangleInsetsChartParam(String name) {
		super(name);
	}

	public RectangleInsetsChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return RectangleInsets.class;
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value, 4);
		return new RectangleInsets(
				getDoubleArg(map, "top"),
				getDoubleArg(map, "left"),
				getDoubleArg(map, "bottom"),
				getDoubleArg(map, "right")
		);
	}
}
