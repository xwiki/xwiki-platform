package com.xpn.xwiki.plugin.charts.params;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class StrokeChartParam extends AbstractChartParam {
	private Map capChoices = new HashMap();
	private Map joinChoices = new HashMap();
	
	public StrokeChartParam(String name) {
		super(name);
		init();
	}

	public StrokeChartParam(String name, boolean optional) {
		super(name, optional);
		init();
	}

	public Class getType() {
		return Stroke.class;
	}
	
	public void init () {
		capChoices.put("butt", new Integer(BasicStroke.CAP_BUTT));
		capChoices.put("round", new Integer(BasicStroke.CAP_ROUND));
		capChoices.put("square", new Integer(BasicStroke.CAP_SQUARE));
		
		joinChoices.put("miter", new Integer(BasicStroke.JOIN_MITER));
		joinChoices.put("round", new Integer(BasicStroke.JOIN_ROUND));
		joinChoices.put("bevel", new Integer(BasicStroke.JOIN_BEVEL));		
	}
	
	public Object convert(String value) throws ParamException {
		Map map = parseMap(value);
		switch (map.size()) {
			case 0: return new BasicStroke();
			case 1: return new BasicStroke(getFloatParam(map, "width"));
			case 3: return new BasicStroke(
							getFloatParam(map, "width"),
							getCapParam(map, "cap"),
							getJoinParam(map, "join")
					);
			case 4: return new BasicStroke(
							getFloatParam(map, "width"),
							getCapParam(map, "cap"),
							getJoinParam(map, "join"),
							getFloatParam(map, "miterlimit")
					);
			case 6: return new BasicStroke(
					getFloatParam(map, "width"),
					getCapParam(map, "cap"),
					getJoinParam(map, "join"),
					getFloatParam(map, "miterlimit"),
					toFloatArray(getListParam(map, "dash")),
					getFloatParam(map, "dash_phase")
			);
			default: throw new ParamException(
					"Invalid number of paramaters in stroke value: "+map.size());
		}
	}

	private int getCapParam(Map map, String name) throws ParamException {
		return ((Integer)getChoiceParam(map, name, capChoices)).intValue();
	}
	
	private int getJoinParam(Map map, String name) throws ParamException {
		return ((Integer)getChoiceParam(map, name, joinChoices)).intValue();
	}
}
