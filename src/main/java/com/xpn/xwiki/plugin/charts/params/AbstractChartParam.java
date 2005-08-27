package com.xpn.xwiki.plugin.charts.params;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public abstract class AbstractChartParam implements ChartParam {
	protected String name;
	protected boolean optional;
	
	public static final String MAP_SEPARATOR = ";";
	public static final String MAP_ASSIGNMENT = ":";
	public static final String LIST_SEPARATOR = ",";

	public AbstractChartParam(String name) {
		this(name, false);
	}

	public AbstractChartParam(String name, boolean optional) {
		this.name = name;
		this.optional = optional; 
	}
	
	public String getName() {
		return name;
	}

	public boolean isOptional() {
		return optional;
	}

	public abstract Class getType();
	public abstract Object convert(String value) throws ParamException;

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ChartParam) {
			return getName().equals(((ChartParam)obj).getName());			
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		return getName().hashCode();
	}
	
	public String toString() {
		return getName();
	}
	
	protected String getStringParam(Map map, String name) throws ParamException {
		String value = (String)map.get(name);
		if (value != null) {
			return value;
		} else {
			throw new ParamException("Invalid value for parameter "+getName()+":when " + 
					map.size() + " parameters are present one has to be " + name);
		}
	}

	protected int getIntParam(Map map, String name) throws ParamException {
		String value = (String)map.get(name);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw new ParamException("Invalid parameter value:" +
						"non-int value for the "+name+" parameter");
			}
		} else {
			throw new ParamException("Invalid parameter value:when " + 
					map.size() + " parameters are present one has to be " + name);
		}
	}
	
	protected float getFloatParam(Map map, String name) throws ParamException {
		String value = (String)map.get(name);
		if (value != null) {
			try {
				return Float.parseFloat(value);
			} catch (NumberFormatException e) {
				throw new ParamException("Invalid parameter value:" +
						"non-float value for the "+name+" parameter");
			}
		} else {
			throw new ParamException("Invalid parameter value:when " + 
					map.size() + " parameters are present one has to be " + name);
		}
	}
	
	protected double getDoubleParam(Map map, String name) throws ParamException {
		String value = (String)map.get(name);
		if (value != null) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				throw new ParamException("Invalid parameter value:" +
						"non-double value for the "+name+" parameter");
			}
		} else {
			throw new ParamException("Invalid parameter value:when " + 
					map.size() + " parameters are present one has to be " + name);
		}
	}

	protected Object getChoiceParam(Map map, String name,
			Map choices) throws ParamException {
		String value = getStringParam(map, name);
		Object obj = choices.get(value);
		if (obj != null) {
			return obj;
		} else {
			throw new ParamException("Invalid parameter value: accepted values for the "
					+ name + " parameter are " + choices.keySet() + "; encountered:" + value);
		}
	}
	
	protected List getListParam(Map map, String name) throws ParamException {
		String value = (String)map.get(name);
		if (value != null) {
			return parseList(value);
		} else {
			throw new ParamException("Invalid parameter value:when " + 
					map.size() + " parameters are present one has to be " + name);
		}
	}
	
	protected Map parseMap(String value) throws ParamException {
		String[] args = value.split(MAP_SEPARATOR);
		if (args.length == 0 || (args.length == 1 && args[0].length() == 0)) {
			return new HashMap(0);
		}
		Map result = new HashMap(args.length);
		for (int i = 0; i<args.length; i++) {
			String[] split = args[i].split(MAP_ASSIGNMENT);
			if (split.length != 2) {
				throw new ParamException("Invalid parameter value:"
						+ "name" + MAP_ASSIGNMENT + "value \"" + 
						MAP_SEPARATOR + "\"-separated list expected");
			}
			result.put(split[0].trim(), split[1].trim());
		}
		return result;
	}

	protected Map parseMap(String value, int expectedTokenCount) throws ParamException {
		Map result = parseMap(value);
		if (result.size() != expectedTokenCount) {
			throw new ParamException("Invalid number of values given to the "
					+ getName() + " parameter; expected:"+expectedTokenCount);
		}
		return result;
	}
	
	protected List parseList(String value) throws ParamException {
		String[] args = value.split(LIST_SEPARATOR);
		if (args.length == 0 || (args.length == 1 && args[0].length() == 0)) {
			return new ArrayList(0);
		}
		List result = new ArrayList(args.length);
		for (int i = 0; i<args.length; i++) {
			result.add(args[i]);
		}
		return result;
	}
	
	protected List toFloatList(List list) throws ParamException {
		List result = new ArrayList(list.size());
		Iterator it = list.iterator();
		while (it.hasNext()) {
			String value = (String)it.next();
			try {
				result.add(new Float(value));
			} catch (NumberFormatException e) {
				throw new ParamException("Invalid parameter value:" +
						"non-float value for the " + name + " parameter");
			}			
		}
		return result;
	}
	
	protected float[] toFloatArray(List list) throws ParamException {
		float[] result = new float[list.size()];
		Iterator it = list.iterator(); int i = 0;
		while (it.hasNext()) {
			String value = (String)it.next();
			try {
				result[i] = Float.parseFloat(value);
			} catch (NumberFormatException e) {
				throw new ParamException("Invalid parameter value:" +
						"non-float value for the " + name + " parameter");
			}
			i++;
		}
		return result;
	}
}
