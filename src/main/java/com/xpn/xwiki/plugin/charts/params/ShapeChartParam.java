package com.xpn.xwiki.plugin.charts.params;

import java.awt.Shape;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ShapeChartParam extends AbstractChartParam {
	private ChartParam choice;

	public ShapeChartParam(String name) {
		super(name);
		init();
	}

	public ShapeChartParam(String name, boolean optional) {
		super(name, optional);
		init();
	}
	
	public void init() {
		choice = new ChoiceChartParam(getName()) {
				protected void init() {
					Shape[] shapes = DefaultDrawingSupplier.createStandardSeriesShapes();
					addChoice("square", shapes[0]);
					addChoice("circle", shapes[1]);
					addChoice("triangle-up", shapes[2]);
					addChoice("diamond", shapes[3]);
					addChoice("rectangle-horizontal", shapes[4]);
					addChoice("triangle-down", shapes[5]);
					addChoice("ellipse", shapes[6]);
					addChoice("triangle-right", shapes[7]);
					addChoice("rectangle-vertical", shapes[8]);
					addChoice("triangle-left", shapes[9]);			
				}
				public Class getType() {
					return Shape.class;
				}
			};
	}

	public Class getType() {
		return Shape.class;
	}
	

	public Object convert(String value) throws ParamException {
		try {
			return choice.convert(value);
		} catch (ParamException e) {
			// TODO: custom shapes
			throw e;
		}
	}
}
