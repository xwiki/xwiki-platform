package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

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
		String dataSeries = params.getString(ChartParams.SERIES);
		
		boolean hasNumericHeader = false;
		try { 
			if (dataSeries.equals("rows") && dataSource.hasHeaderRow()) {
				for (int column = 0; column<dataSource.getColumnCount(); column++) {
					Double.parseDouble(dataSource.getHeaderRowValue(column));
				}
				hasNumericHeader = true;
			} else if (dataSeries.equals("columns") && dataSource.hasHeaderColumn()) {
				for (int row = 0; row<dataSource.getRowCount(); row++) {
					Double.parseDouble(dataSource.getHeaderColumnValue(row));
				}
				hasNumericHeader = true;			
			}
		} catch (NumberFormatException e) { /* ignore */ }
		
		if (hasNumericHeader) {
			String domainAxisLabel = params.getString(ChartParams.DOMAIN_AXIS_LABEL);
			String rangeAxisLabel = params.getString(ChartParams.RANGE_AXIS_LABEL);
			
			NumberAxis domainAxis = new NumberAxis(domainAxisLabel);
			ValueAxis rangeAxis = new NumberAxis(rangeAxisLabel);
			
			XYItemRenderer renderer = XYItemRendererFactory.getInstance().create(params);

			XYDataset dataset = XYDatasetFactory.getInstance().create(dataSource, params);
			
			return new XYPlot(dataset, domainAxis, rangeAxis, renderer);
		} else {
			return CategoryPlotFactory.getInstance().create(dataSource, params);			
		}
	}
}
