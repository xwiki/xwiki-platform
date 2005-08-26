package com.xpn.xwiki.plugin.charts.params;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;


public class DefaultChartParameters extends ChartParams {
	public static DefaultChartParameters uniqueInstance;
	
	private DefaultChartParameters() {
		try {
			set(ChartParams.WIDTH, "400");
			set(ChartParams.HEIGHT, "300");
			set(ChartParams.SERIES, "columns");
			set(ChartParams.RENDERER, "default");
			set(ChartParams.DOMAIN_AXIS_LABEL, "");
			set(ChartParams.RANGE_AXIS_LABEL, "");
			set(ChartParams.BORDER_VISIBLE, "false");
			set(ChartParams.ANTI_ALIAS, "true");
		} catch (ParamException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static synchronized DefaultChartParameters getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new DefaultChartParameters();
		}
		return uniqueInstance; 
	}
}
