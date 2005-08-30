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

public class LinePlotFactory  implements PlotFactory {
	private static LinePlotFactory uniqueInstance = new LinePlotFactory();
	
	private LinePlotFactory() {
		// empty
	}

	public static LinePlotFactory getInstance() {
		return uniqueInstance;
	}
	
	public Plot create(DataSource dataSource, ChartParams params)
			throws GenerateException, DataSourceException {
		
		if (hasNumericHeader(dataSource, params)) {
			NumberAxis domainAxis = new NumberAxis();
			NumberAxis rangeAxis = new NumberAxis();
			ChartCustomizer.customizeNumberAxis(domainAxis, params, ChartParams.AXIS_DOMAIN_PREFIX);
			ChartCustomizer.customizeNumberAxis(rangeAxis, params, ChartParams.AXIS_RANGE_PREFIX);   
			
			XYItemRenderer renderer = XYItemRendererFactory.getInstance().create(params);

			XYDataset dataset = XYDatasetFactory.getInstance().create(dataSource, params);
			
			XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);
			
			ChartCustomizer.customizeXYPlot(plot, params);
			
			return plot;
		} else {
			return CategoryPlotFactory.getInstance().create(dataSource, params);			
		}
	}
	
	private boolean hasNumericHeader(DataSource dataSource,
			ChartParams params) throws DataSourceException {
		String dataSeries = params.getString(ChartParams.SERIES);
		try { 
			if (dataSeries.equals("rows") && dataSource.hasHeaderRow()) {
				for (int column = 0; column<dataSource.getColumnCount(); column++) {
					Double.parseDouble(dataSource.getHeaderRowValue(column));
				}
				return true;
			} else if (dataSeries.equals("columns") && dataSource.hasHeaderColumn()) {
				for (int row = 0; row<dataSource.getRowCount(); row++) {
					Double.parseDouble(dataSource.getHeaderColumnValue(row));
				}
				return true;			
			}
		} catch (NumberFormatException e) { /* ignore */ }
		return false;
	}
}
