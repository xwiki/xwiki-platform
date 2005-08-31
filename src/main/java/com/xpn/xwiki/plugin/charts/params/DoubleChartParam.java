package com.xpn.xwiki.plugin.charts.params;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class DoubleChartParam extends AbstractChartParam {

	public DoubleChartParam(String name) {
		super(name);
	}

	public DoubleChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Double.class;
	}

	public Object convert(String value) throws ParamException {
        try {
	        return new Double(value);
        } catch (NumberFormatException nfe) {
			throw new InvalidParamException("Non-double value for the "+getName()+" parameter", nfe);
        }
	}
}
