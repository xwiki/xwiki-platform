package com.xpn.xwiki.plugin.charts.params;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class IntegerChartParam extends AbstractChartParam implements ChartParam {

	public IntegerChartParam(String name) {
		super(name);
	}

	public IntegerChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}
	
	public Class getType() {
		return Integer.class;
	}
	
	public Object convert(String value) throws ParamException {
        try {
	        return new Integer(value);
        } catch (NumberFormatException nfe) {
			throw new ParamException("Noninteger value for the "+getName()+" parameter", nfe);
        }
	}
}
