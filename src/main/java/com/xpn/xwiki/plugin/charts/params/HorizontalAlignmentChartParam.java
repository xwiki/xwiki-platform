package com.xpn.xwiki.plugin.charts.params;

import org.jfree.ui.HorizontalAlignment;

public class HorizontalAlignmentChartParam extends ChoiceChartParam {

	public HorizontalAlignmentChartParam(String name) {
		super(name);
	}

	public HorizontalAlignmentChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}
	

	public Class getType() {
		return HorizontalAlignment.class;
	}

	protected void init() {
		addChoice("left", HorizontalAlignment.LEFT);
		addChoice("right", HorizontalAlignment.RIGHT);
		addChoice("center", HorizontalAlignment.CENTER);
	}
}
