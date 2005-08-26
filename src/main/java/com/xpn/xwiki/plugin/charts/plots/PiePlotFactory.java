package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.DefaultPieDataset;

import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class PiePlotFactory implements PlotFactory {
	private static PiePlotFactory uniqueInstance = new PiePlotFactory();
	
	private PiePlotFactory() {
		// empty
	}

	public static PiePlotFactory getInstance() {
		return uniqueInstance;
	}

	public Plot create(DataSource dataSource, ChartParams params)
			throws GenerateException, DataSourceException {
		DefaultPieDataset dataset = new DefaultPieDataset();
		String dataSeries = params.getString(ChartParams.SERIES);
		if (dataSeries.equals("columns")) {
			for (int row = 0; row<dataSource.getRowCount(); row++) {
				if (dataSource.hasHeaderColumn()) {
					String category = dataSource.getHeaderColumnValue(row);
					dataset.setValue(category, dataSource.getCell(row, 0));
				} else {
					dataset.setValue("", dataSource.getCell(row, 0));						
				}
			}
		} else if (dataSeries.equals("rows")) {
			for (int column = 0; column<dataSource.getColumnCount(); column++) {
				if (dataSource.hasHeaderRow()) {
					String category = dataSource.getHeaderRowValue(column);
					dataset.setValue(category, dataSource.getCell(0, column));
				} else {
					dataset.setValue("", dataSource.getCell(0, column));						
				}
			}
		} else {
			throw new GenerateException("Invalid series parameter:"+dataSeries);
		}
		
		return new PiePlot(dataset);
	}
}
