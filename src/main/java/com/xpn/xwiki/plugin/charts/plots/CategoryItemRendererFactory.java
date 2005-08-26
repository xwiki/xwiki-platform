package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.CategoryStepRenderer;
import org.jfree.chart.renderer.category.DefaultCategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.chart.renderer.category.IntervalBarRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.renderer.category.LevelRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.LineRenderer3D;
import org.jfree.chart.renderer.category.MinMaxCategoryRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer3D;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.chart.renderer.category.WaterfallBarRenderer;

import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

public class CategoryItemRendererFactory {
	private static CategoryItemRendererFactory uniqueInstance = new CategoryItemRendererFactory();
	
	private CategoryItemRendererFactory() {
		// empty
	}

	public static CategoryItemRendererFactory getInstance() {
		return uniqueInstance;
	}
	
	public CategoryItemRenderer create(ChartParams params) throws GenerateException {
		String renderer = params.getString(ChartParams.RENDERER);
		String type = params.getString(ChartParams.TYPE);
		if (renderer.equals("default")) {
			if (type.equals("bar")) {
				return new BarRenderer();
			} else if (type.equals("line")) {
				return new LineAndShapeRenderer();				
			} else {
				return new DefaultCategoryItemRenderer();
			}
		} else if (renderer.equals("bar")) {
			return new BarRenderer();
		} else if (renderer.equals("bar_3d")) {
			return new BarRenderer3D();
		} else if (renderer.equals("area")) {
			return new AreaRenderer();					
		} else if (renderer.equals("stacked_area")) {
			return new StackedAreaRenderer();
		} else if (renderer.equals("box_and_whisker")) {
			return new BoxAndWhiskerRenderer();					
		} else if (renderer.equals("category_step")) {
			return new CategoryStepRenderer();					
		} else if (renderer.equals("default_category")) {
			return new DefaultCategoryItemRenderer();
		} else if (renderer.equals("gantt")) {
			return new GanttRenderer();					
		} else if (renderer.equals("grouped_stacked_bar")) {
			return new GroupedStackedBarRenderer();					
		} else if (renderer.equals("interval_bar")) {
			return new IntervalBarRenderer();					
		} else if (renderer.equals("layered_bar")) {
			return new LayeredBarRenderer();					
		} else if (renderer.equals("level")) {
			return new LevelRenderer();					
		} else if (renderer.equals("line_and_shape")) {
			return new LineAndShapeRenderer();
		} else if (renderer.equals("line_3D")) {
			return new LineRenderer3D();
		} else if (renderer.equals("min_max")) {
			return new MinMaxCategoryRenderer();					
		} else if (renderer.equals("stacked_bar")) {
			return new StackedBarRenderer();					
		} else if (renderer.equals("stacked_bar_3D")) {
			return new StackedBarRenderer3D();
		} else if (renderer.equals("statistical_bar")) {
			return new StatisticalBarRenderer();
		} else if (renderer.equals("statistical_line_and_shape")) {
			return new StatisticalLineAndShapeRenderer();
		} else if (renderer.equals("waterfall_bar")) {
			return new WaterfallBarRenderer();
		} else {
			throw new GenerateException("Unknown renderer:"+renderer);
		}
	}
}
