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
		String renderer = params.getString(ChartParams.RENDERER);
		if (renderer.equals("default")) {
			return new DefaultXYItemRenderer();
		} else if (renderer.equals("candlestick")) {
			return new CandlestickRenderer();
		} else if (renderer.equals("clustered_bar")) {
			return new ClusteredXYBarRenderer();
		} else if (renderer.equals("cyclic")) {
			return new CyclicXYItemRenderer();			
		} else if (renderer.equals("high_low")) {
			return new HighLowRenderer();
		} else if (renderer.equals("signal")) {
			return new SignalRenderer();					
		} else if (renderer.equals("stacked_area")) {
			return new StackedXYAreaRenderer();
		} else if (renderer.equals("stacked_area2")) {
			return new StackedXYAreaRenderer2();
		} else if (renderer.equals("stacked_bar")) {
			return new StackedXYBarRenderer();
		} else if (renderer.equals("standard")) {
			return new StandardXYItemRenderer();					
		} else if (renderer.equals("wind")) {
			return new WindItemRenderer();					
		} else if (renderer.equals("area")) {
			return new XYAreaRenderer();					
		} else if (renderer.equals("area2")) {
			return new XYAreaRenderer2();					
		} else if (renderer.equals("bar")) {
			return new XYBarRenderer();
		} else if (renderer.equals("box_and_whisker")) {
			return new XYBoxAndWhiskerRenderer();
		} else if (renderer.equals("bubble")) {
			return new XYBubbleRenderer();					
		} else if (renderer.equals("difference")) {
			return new XYDifferenceRenderer();					
		} else if (renderer.equals("dot")) {
			return new XYDotRenderer();
		} else if (renderer.equals("line_3d")) {
			return new XYLine3DRenderer();
		} else if (renderer.equals("line_and_shape")) {
			return new XYLineAndShapeRenderer();
		} else if (renderer.equals("step_area")) {
			return new XYStepAreaRenderer();
		} else if (renderer.equals("step")) {
			return new XYStepRenderer();
		} else if (renderer.equals("y_interval")) {
			return new YIntervalRenderer();
		} else {
			throw new GenerateException("Unknown renderer:"+renderer);
		}
	}
}
