package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.plot.Plot;

import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class BarPlotFactory implements PlotFactory {
	private static BarPlotFactory uniqueInstance = new BarPlotFactory();
	
	private BarPlotFactory() {
		// empty
	}

	public static BarPlotFactory getInstance() {
		return uniqueInstance;
	}
	
	public Plot create(DataSource dataSource, ChartParams params)
			throws GenerateException, DataSourceException {
		return CategoryPlotFactory.getInstance().create(dataSource, params);
	}
}
