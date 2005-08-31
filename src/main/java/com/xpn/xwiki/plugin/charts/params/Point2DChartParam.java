package com.xpn.xwiki.plugin.charts.params;

import java.awt.geom.Point2D;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class Point2DChartParam extends AbstractChartParam {

	public Point2DChartParam(String name) {
		super(name);
	}

	public Point2DChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Point2D.class;
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value, 2);
		return new Point2D.Double(getDoubleArg(map, "x"), getDoubleArg(map, "y"));
	}
}
