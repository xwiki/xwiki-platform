package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.CyclicXYItemRenderer;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.chart.renderer.xy.SignalRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.WindItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLine3DRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.renderer.xy.YIntervalRenderer;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

public class XYItemRendererFactory {

	private static XYItemRendererFactory uniqueInstance = new XYItemRendererFactory();
	
	private XYItemRendererFactory() {
		// empty
	}

	public static XYItemRendererFactory getInstance() {
		return uniqueInstance;
	}
	
	public XYItemRenderer create(ChartParams params) throws GenerateException {
		String rendererSelector = params.getString(ChartParams.RENDERER);
		XYItemRenderer renderer;
		if (rendererSelector.equals("default")) {
			renderer = new DefaultXYItemRenderer();
		} else if (rendererSelector.equals("candlestick")) {
			renderer = new CandlestickRenderer();
		} else if (rendererSelector.equals("clustered_bar")) {
			renderer = new ClusteredXYBarRenderer();
		} else if (rendererSelector.equals("cyclic")) {
			renderer = new CyclicXYItemRenderer();			
		} else if (rendererSelector.equals("high_low")) {
			renderer = new HighLowRenderer();
		} else if (rendererSelector.equals("signal")) {
			renderer = new SignalRenderer();					
		} else if (rendererSelector.equals("stacked_area")) {
			renderer = new StackedXYAreaRenderer();
		} else if (rendererSelector.equals("stacked_area2")) {
			renderer = new StackedXYAreaRenderer2();
		} else if (rendererSelector.equals("stacked_bar")) {
			renderer = new StackedXYBarRenderer();
		} else if (rendererSelector.equals("standard")) {
			renderer = new StandardXYItemRenderer();					
		} else if (rendererSelector.equals("wind")) {
			renderer = new WindItemRenderer();					
		} else if (rendererSelector.equals("area")) {
			renderer = new XYAreaRenderer();					
		} else if (rendererSelector.equals("area2")) {
			renderer = new XYAreaRenderer2();					
		} else if (rendererSelector.equals("bar")) {
			renderer = new XYBarRenderer();
		} else if (rendererSelector.equals("box_and_whisker")) {
			renderer = new XYBoxAndWhiskerRenderer();
		} else if (rendererSelector.equals("bubble")) {
			renderer = new XYBubbleRenderer();					
		} else if (rendererSelector.equals("difference")) {
			renderer = new XYDifferenceRenderer();					
		} else if (rendererSelector.equals("dot")) {
			renderer = new XYDotRenderer();
		} else if (rendererSelector.equals("line_3d")) {
			renderer = new XYLine3DRenderer();
		} else if (rendererSelector.equals("line_and_shape")) {
			renderer = new XYLineAndShapeRenderer();
		} else if (rendererSelector.equals("step_area")) {
			renderer = new XYStepAreaRenderer();
		} else if (rendererSelector.equals("step")) {
			renderer = new XYStepRenderer();
		} else if (rendererSelector.equals("y_interval")) {
			renderer = new YIntervalRenderer();
		} else {
			throw new GenerateException("Unknown renderer:"+rendererSelector);
		}
		ChartCustomizer.customizeXYItemRenderer(renderer, params);
		return renderer;
	}
}
