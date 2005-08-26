package com.xpn.xwiki.plugin.charts.params;

import org.jfree.ui.RectangleEdge;

public class RectangleEdgeChartParam extends ChoiceChartParam {

	public RectangleEdgeChartParam(String name) {
		super(name);
	}
	
	public RectangleEdgeChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}
	
	public Class getType() {
		return RectangleEdge.class;
	}
	
	protected void init() {
		addChoice("top", RectangleEdge.TOP);
		addChoice("bottom", RectangleEdge.BOTTOM);
		addChoice("left", RectangleEdge.LEFT);
		addChoice("right", RectangleEdge.RIGHT);
	}
}
