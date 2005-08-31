package com.xpn.xwiki.plugin.charts.params;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class FloatChartParam extends AbstractChartParam {

	public FloatChartParam(String name) {
		super(name);
	}

	public FloatChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Float.class;
	}

	public Object convert(String value) throws ParamException {
        try {
	        return new Float(value);
        } catch (NumberFormatException nfe) {
			throw new InvalidParamException("Non-float value for the "+getName()+" parameter", nfe);
        }
	}
}
