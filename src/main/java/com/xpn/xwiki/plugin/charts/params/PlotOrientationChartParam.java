package com.xpn.xwiki.plugin.charts.params;

import org.jfree.chart.plot.PlotOrientation;

public class PlotOrientationChartParam extends ChoiceChartParam {

	public PlotOrientationChartParam(String name) {
		super(name);
	}

	public PlotOrientationChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}

	protected void init() {
		addChoice("horizontal", PlotOrientation.HORIZONTAL);
		addChoice("vertical", PlotOrientation.VERTICAL);
	}

	public Class getType() {
		return PlotOrientation.class;
	}
}
