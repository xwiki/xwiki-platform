package com.xpn.xwiki.plugin.charts.params;

import org.jfree.chart.axis.CategoryLabelPositions;

// TODO: maybe extend this class sometime
public class CategoryLabelPositionsChartParam extends ChoiceChartParam {

	public CategoryLabelPositionsChartParam(String name) {
		super(name);
	}

	public CategoryLabelPositionsChartParam(String name, boolean optional) {
		super(name, optional);
	}

	protected void init() {
		addChoice("down_45", CategoryLabelPositions.DOWN_45);
		addChoice("down_90", CategoryLabelPositions.DOWN_90);
		addChoice("standard", CategoryLabelPositions.STANDARD);
		addChoice("up_45", CategoryLabelPositions.UP_45);
		addChoice("up_90", CategoryLabelPositions.UP_90);		
	}

	public Class getType() {
		return CategoryLabelPositions.class;
	}
}
