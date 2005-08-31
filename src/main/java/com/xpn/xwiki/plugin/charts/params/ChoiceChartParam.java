package com.xpn.xwiki.plugin.charts.params;

import java.util.HashMap;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public abstract class ChoiceChartParam extends AbstractChartParam {
	protected Map choices = new HashMap();

	public ChoiceChartParam(String name) {
		super(name);
		init();
	}

	public ChoiceChartParam(String name, boolean isOptional) {
		super(name, isOptional);
		init();
	}
	
	public void addChoice(String selector, Object value) {
		choices.put(selector, value);
	}

	public Object convert(String selector) throws ParamException {
		Object value = choices.get(selector);
		if (value != null) {
			return value;
		} else {
			throw new InvalidParamException("Invalid parameter value: " +
					"Accepted values for the " + getName() + " parameter are "
					+ choices.toString() + "; encountered: " + selector);
		}
	}

	protected abstract void init();
	
	public abstract Class getType();
}
