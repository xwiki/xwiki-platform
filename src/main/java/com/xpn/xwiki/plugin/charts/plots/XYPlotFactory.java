package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class XYPlotFactory {
	private static XYPlotFactory uniqueInstance = new XYPlotFactory();
	
	private XYPlotFactory() {
		// empty
	}

	public static XYPlotFactory getInstance() {
		return uniqueInstance;
	}

	public Plot create(DataSource dataSource, XYItemRenderer renderer, ChartParams params)
			throws GenerateException, DataSourceException {
		NumberAxis domainAxis = new NumberAxis();
		NumberAxis rangeAxis = new NumberAxis();
		ChartCustomizer.customizeNumberAxis(domainAxis, params, ChartParams.AXIS_DOMAIN_PREFIX);
		ChartCustomizer.customizeNumberAxis(rangeAxis, params, ChartParams.AXIS_RANGE_PREFIX);   

		XYDataset dataset = TableXYDatasetFactory.getInstance().create(dataSource, params);
		
		XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);
		
		ChartCustomizer.customizeXYPlot(plot, params);
		return plot;
	}
}
