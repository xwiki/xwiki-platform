package com.xpn.xwiki.plugin.charts.params;

import java.text.NumberFormat;
import java.util.Map;

import org.jfree.chart.axis.NumberTickUnit;

import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class NumberTickUnitChartParam extends NumberFormatChartParam {
	
	public NumberTickUnitChartParam(String name) {
		super(name);
	}

	public NumberTickUnitChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return NumberTickUnit.class;
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value);
		double size = getDoubleArg(map, "size");
		try {
			NumberFormat format = (NumberFormat)super.convert(value);
			return new NumberTickUnit(size, format);
		} catch (MissingArgumentException e) {
			return new NumberTickUnit(size);
		}
	}
}
