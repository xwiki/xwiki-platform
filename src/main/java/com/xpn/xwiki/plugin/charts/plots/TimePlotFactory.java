package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.axis.DateAxis;
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

public class TimePlotFactory implements PlotFactory {
	private static TimePlotFactory uniqueInstance = new TimePlotFactory();
	
	private TimePlotFactory() {
		// empty
	}

	public static TimePlotFactory getInstance() {
		return uniqueInstance;
	}
	
	public Plot create(DataSource dataSource, ChartParams params)
			throws GenerateException, DataSourceException {
        DateAxis domainAxis = new DateAxis();
		ChartCustomizer.customizeDateAxis(domainAxis, params, ChartParams.AXIS_DOMAIN_PREFIX);

        NumberAxis rangeAxis = new NumberAxis();
		ChartCustomizer.customizeNumberAxis(rangeAxis, params, ChartParams.AXIS_RANGE_PREFIX);   
        rangeAxis.setAutoRangeIncludesZero(false);  // override default

		XYItemRenderer renderer = XYItemRendererFactory.getInstance().create(params);

		XYDataset dataset = TimeSeriesCollectionFactory.getInstance().create(dataSource, params);

		XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

		ChartCustomizer.customizeXYPlot(plot, params);
        
        return plot;
	}	
}
