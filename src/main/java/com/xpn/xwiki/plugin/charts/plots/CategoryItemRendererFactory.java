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

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
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
		String rendererSelector = params.getString(ChartParams.RENDERER);
		String type = params.getString(ChartParams.TYPE);
		CategoryItemRenderer renderer;
		if (rendererSelector.equals("default")) {
			if (type.equals("bar")) {
				renderer = new BarRenderer();
			} else if (type.equals("line")) {
				renderer = new LineAndShapeRenderer();				
			} else {
				renderer = new DefaultCategoryItemRenderer();
			}
		} else if (rendererSelector.equals("bar")) {
			renderer = new BarRenderer();
		} else if (rendererSelector.equals("bar_3d")) {
			renderer = new BarRenderer3D();
		} else if (rendererSelector.equals("area")) {
			renderer = new AreaRenderer();					
		} else if (rendererSelector.equals("stacked_area")) {
			renderer = new StackedAreaRenderer();
		} else if (rendererSelector.equals("box_and_whisker")) {
			renderer = new BoxAndWhiskerRenderer();					
		} else if (rendererSelector.equals("category_step")) {
			renderer = new CategoryStepRenderer();					
		} else if (rendererSelector.equals("default_category")) {
			renderer = new DefaultCategoryItemRenderer();
		} else if (rendererSelector.equals("gantt")) {
			renderer = new GanttRenderer();					
		} else if (rendererSelector.equals("grouped_stacked_bar")) {
			renderer = new GroupedStackedBarRenderer();					
		} else if (rendererSelector.equals("interval_bar")) {
			renderer = new IntervalBarRenderer();					
		} else if (rendererSelector.equals("layered_bar")) {
			renderer = new LayeredBarRenderer();					
		} else if (rendererSelector.equals("level")) {
			renderer = new LevelRenderer();					
		} else if (rendererSelector.equals("line_and_shape")) {
			renderer = new LineAndShapeRenderer();
		} else if (rendererSelector.equals("line_3D")) {
			renderer = new LineRenderer3D();
		} else if (rendererSelector.equals("min_max")) {
			renderer = new MinMaxCategoryRenderer();					
		} else if (rendererSelector.equals("stacked_bar")) {
			renderer = new StackedBarRenderer();					
		} else if (rendererSelector.equals("stacked_bar_3D")) {
			renderer = new StackedBarRenderer3D();
		} else if (rendererSelector.equals("statistical_bar")) {
			renderer = new StatisticalBarRenderer();
		} else if (rendererSelector.equals("statistical_line_and_shape")) {
			renderer = new StatisticalLineAndShapeRenderer();
		} else if (rendererSelector.equals("waterfall_bar")) {
			renderer = new WaterfallBarRenderer();
		} else {
			throw new GenerateException("Unknown renderer:"+rendererSelector);
		}
		ChartCustomizer.customizeCategoryItemRenderer(renderer, params);
		return renderer;
	}
}
