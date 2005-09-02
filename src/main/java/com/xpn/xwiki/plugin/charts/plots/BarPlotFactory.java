package com.xpn.xwiki.plugin.charts.plots;

import java.lang.reflect.Constructor;

import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
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
		Class rendererClass = params.getClass(ChartParams.RENDERER);
		if (rendererClass == null || 
				CategoryItemRenderer.class.isAssignableFrom(rendererClass)) {
			CategoryItemRenderer renderer;
			if (rendererClass != null) {
				try {
					Constructor ctor = rendererClass.getConstructor(new Class[] {});
					renderer = (CategoryItemRenderer)ctor.newInstance(new Object[] { });
				} catch (Throwable e) {
					throw new GenerateException(e);
				}
			} else {
				renderer = new BarRenderer();
			}
			return CategoryPlotFactory.getInstance().create(dataSource, renderer, params);
		} else if (XYItemRenderer.class.isAssignableFrom(rendererClass)) {
			XYItemRenderer renderer;
			if (rendererClass != null) {
				try {
					Constructor ctor = rendererClass.getConstructor(new Class[] {});
					renderer = (XYItemRenderer)ctor.newInstance(new Object[] { });
				} catch (Throwable e) {
					throw new GenerateException(e);
				}
			} else {
				renderer = new XYBarRenderer(); // will never run
			}
			ChartCustomizer.customizeXYItemRenderer(renderer, params);

			return XYPlotFactory.getInstance().create(dataSource, renderer, params);
		} else {
			throw new GenerateException("Incompatible renderer class: " + rendererClass);
		}
	}
}
