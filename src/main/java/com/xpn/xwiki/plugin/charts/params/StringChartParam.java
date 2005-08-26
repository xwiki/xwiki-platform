package com.xpn.xwiki.plugin.charts.params;

public class StringChartParam extends AbstractChartParam implements ChartParam {

	public StringChartParam(String name) {
		super(name);
	}

	public StringChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}
	
	public Class getType() {
		return String.class;
	}
	
	public Object convert(String value) {
		return value;
	}
}
