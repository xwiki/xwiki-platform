package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class CategoryPlotFactory implements PlotFactory {
	private static CategoryPlotFactory uniqueInstance = new CategoryPlotFactory();
	
	private CategoryPlotFactory() {
		// empty
	}

	public static CategoryPlotFactory getInstance() {
		return uniqueInstance;
	}

	public Plot create(DataSource dataSource, ChartParams params)
			throws GenerateException, DataSourceException {
		String domainAxisLabel = params.getString(ChartParams.DOMAIN_AXIS_LABEL);
		String rangeAxisLabel = params.getString(ChartParams.RANGE_AXIS_LABEL);
		String dataSeries = params.getString(ChartParams.SERIES);
		
		CategoryAxis domainAxis = new CategoryAxis(domainAxisLabel);
		ValueAxis rangeAxis = new NumberAxis(rangeAxisLabel);
		
		CategoryItemRenderer renderer = CategoryItemRendererFactory.getInstance().create(params);

		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		
		if (dataSeries.equals("columns")) {
			for (int row = 0; row<dataSource.getRowCount(); row++) {
				for (int column = 0; column<dataSource.getColumnCount(); column++) {
					dataset.addValue(dataSource.getCell(row, column),
							dataSource.hasHeaderRow()?dataSource.getHeaderRowValue(column):"",
							dataSource.hasHeaderColumn()?dataSource.getHeaderColumnValue(row):""
					);
				}
			}
		} else if (dataSeries.equals("rows")) {
			for (int row = 0; row<dataSource.getRowCount(); row++) {
				for (int column = 0; column<dataSource.getColumnCount(); column++) {
					dataset.addValue(dataSource.getCell(row, column),
							dataSource.hasHeaderColumn()?dataSource.getHeaderColumnValue(row):"",
							dataSource.hasHeaderRow()?dataSource.getHeaderRowValue(column):""
					);
				}
			}
		} else {
			throw new GenerateException("Invalid series parameter");					
		}
		return new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
	}
}
