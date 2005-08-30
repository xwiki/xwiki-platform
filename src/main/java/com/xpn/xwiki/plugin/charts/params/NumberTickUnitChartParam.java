package com.xpn.xwiki.plugin.charts.params;

import java.text.NumberFormat;
import java.util.Map;

import org.jfree.chart.axis.NumberTickUnit;

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
		NumberFormat format;
		try {
			format = (NumberFormat)super.convert(value);
			return new NumberTickUnit(getDoubleParam(map, "size"), format);
		} catch (ParamException e) {
			return new NumberTickUnit(getDoubleParam(map, "size"));
		}
	}
}
