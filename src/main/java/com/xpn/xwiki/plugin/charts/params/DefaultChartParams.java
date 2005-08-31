package com.xpn.xwiki.plugin.charts.params;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;


public class DefaultChartParams extends ChartParams {
	public static DefaultChartParams uniqueInstance;
	
	private DefaultChartParams() {
		try {
			set(ChartParams.WIDTH, "400");
			set(ChartParams.HEIGHT, "300");
			set(ChartParams.SERIES, "columns");
			set(ChartParams.RENDERER, "default");
			set(ChartParams.BORDER_VISIBLE, "false");
			set(ChartParams.ANTI_ALIAS, "true");
		} catch (ParamException e) {
			throw new RuntimeException(e);
		}
	}

	public static synchronized DefaultChartParams getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new DefaultChartParams();
		}
		return uniqueInstance; 
	}
}
