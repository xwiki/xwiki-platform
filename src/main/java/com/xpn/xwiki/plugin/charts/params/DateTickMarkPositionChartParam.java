package com.xpn.xwiki.plugin.charts.params;

import org.jfree.chart.axis.DateTickMarkPosition;

public class DateTickMarkPositionChartParam extends ChoiceChartParam {

	public DateTickMarkPositionChartParam(String name) {
		super(name);
	}

	public DateTickMarkPositionChartParam(String name, boolean optional) {
		super(name, optional);
	}

	protected void init() {
		addChoice("start", DateTickMarkPosition.START);
		addChoice("middle", DateTickMarkPosition.MIDDLE);
		addChoice("end", DateTickMarkPosition.END);
	}
	
	public Class getType() {
		return DateTickMarkPosition.class;
	}
}
