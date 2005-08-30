package com.xpn.xwiki.plugin.charts.params;

import org.jfree.data.RangeType;

public class RangeTypeChartParam extends ChoiceChartParam {

	public RangeTypeChartParam(String name) {
		super(name);
	}

	public RangeTypeChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}

	protected void init() {
		addChoice("full", RangeType.FULL);
		addChoice("negative", RangeType.NEGATIVE);
		addChoice("positive", RangeType.POSITIVE);
	}

	public Class getType() {
		return RangeType.class;
	}
}
