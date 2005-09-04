package com.xpn.xwiki.plugin.charts.params;

import org.jfree.ui.VerticalAlignment;

public class VerticalAlignmentChartParam extends ChoiceChartParam {

	public VerticalAlignmentChartParam(String name) {
		super(name);
	}

	public VerticalAlignmentChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}

	public Class getType() {
		return VerticalAlignment.class;
	}

	public void init() {
		addChoice("top", VerticalAlignment.TOP);
		addChoice("bottom", VerticalAlignment.BOTTOM);
		addChoice("center", VerticalAlignment.CENTER);
	}
}
