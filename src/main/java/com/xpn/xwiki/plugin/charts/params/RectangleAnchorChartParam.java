package com.xpn.xwiki.plugin.charts.params;

import org.jfree.ui.RectangleAnchor;

public class RectangleAnchorChartParam extends ChoiceChartParam {

	public RectangleAnchorChartParam(String name) {
		super(name);
	}

	public RectangleAnchorChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}

	protected void init() {
		addChoice("bottom", RectangleAnchor.BOTTOM);
		addChoice("bottom-left", RectangleAnchor.BOTTOM_LEFT);
		addChoice("bottom-right", RectangleAnchor.BOTTOM_RIGHT);
		addChoice("center", RectangleAnchor.CENTER);
		addChoice("left", RectangleAnchor.LEFT);
		addChoice("right", RectangleAnchor.RIGHT);
		addChoice("top", RectangleAnchor.TOP);
		addChoice("top-left", RectangleAnchor.TOP_LEFT);
		addChoice("top-right", RectangleAnchor.TOP_RIGHT);
	}

	public Class getType() {
		return RectangleAnchor.class;
	}
}
