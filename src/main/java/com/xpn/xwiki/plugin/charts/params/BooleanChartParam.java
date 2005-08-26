package com.xpn.xwiki.plugin.charts.params;

public class BooleanChartParam extends AbstractChartParam {

	public BooleanChartParam(String name) {
		super(name);
	}

	public Class getType() {
		return Boolean.class;
	}

	public Object convert(String value) {
		return new Boolean(value);
	}
}
