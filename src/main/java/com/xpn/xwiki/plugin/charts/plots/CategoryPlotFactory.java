package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class CategoryPlotFactory {
	private static CategoryPlotFactory uniqueInstance = new CategoryPlotFactory();
	
	private CategoryPlotFactory() {
		// empty
	}

	public static CategoryPlotFactory getInstance() {
		return uniqueInstance;
	}

	public Plot create(DataSource dataSource, CategoryItemRenderer renderer, ChartParams params)
			throws GenerateException, DataSourceException {
		String dataSeries = params.getString(ChartParams.SERIES);
		
		CategoryAxis domainAxis = new CategoryAxis();
		ValueAxis rangeAxis = new NumberAxis();
		ChartCustomizer.customizeCategoryAxis(domainAxis, params, ChartParams.AXIS_DOMAIN_PREFIX);
		ChartCustomizer.customizeValueAxis(rangeAxis, params, ChartParams.AXIS_RANGE_PREFIX);    
		
		Class rendererClass = params.getClass(ChartParams.RENDERER);
		
		ChartCustomizer.customizeCategoryItemRenderer(renderer, params);
		
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		
		if ("columns".equals(dataSeries)) {
			for (int row = 0; row<dataSource.getRowCount(); row++) {
				for (int column = 0; column<dataSource.getColumnCount(); column++) {
					dataset.addValue(dataSource.getCell(row, column),
							dataSource.hasHeaderRow()?dataSource.getHeaderRowValue(column):"",
							dataSource.hasHeaderColumn()?dataSource.getHeaderColumnValue(row):""
					);
				}
			}
		} else if ("rows".equals(dataSeries)) {
			for (int row = 0; row<dataSource.getRowCount(); row++) {
				for (int column = 0; column<dataSource.getColumnCount(); column++) {
					dataset.addValue(dataSource.getCell(row, column),
							dataSource.hasHeaderColumn()?dataSource.getHeaderColumnValue(row):"",
							dataSource.hasHeaderRow()?dataSource.getHeaderRowValue(column):""
					);
				}
			}
		} else {
			throw new GenerateException("Invalid series parameter: "+dataSeries);					
		}
		return new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
	}
}
