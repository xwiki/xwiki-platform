package com.xpn.xwiki.plugin.charts.params;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class NumberFormatChartParam extends LocaleChartParam {
	public static final String TYPE = "type";
	public static final String GENERAL = "general";
	public static final String NUMBER = "number";
	public static final String INTEGER = "integer";
	public static final String CURRENCY = "currency";
	public static final String PERCENT = "percent";

	public NumberFormatChartParam(String name) {
		super(name);
	}

	public NumberFormatChartParam(String name, boolean optional) {
		super(name, optional);
	}
	
	public Class getType() {
		return NumberFormat.class;
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value);
		String type = getStringParam(map, TYPE);
		Locale locale;
		try {
			locale = (Locale)super.convert(value);
		} catch (ParamException e) {
			locale = null;
		}
		
		if (type.equals(GENERAL)) {
			if (locale != null) {
				return NumberFormat.getInstance(locale);
			} else {
				return NumberFormat.getInstance();
			}
		} else if (type.equals(NUMBER)) {
			if (locale != null) {
				return NumberFormat.getNumberInstance(locale);
			} else {
				return NumberFormat.getNumberInstance();
			}			
		} else if (type.equals(INTEGER)) {
			if (locale != null) {
				return NumberFormat.getIntegerInstance(locale);
			} else {
				return NumberFormat.getIntegerInstance();
			}			
		} else if (type.equals(CURRENCY)) {
			if (locale != null) {
				return NumberFormat.getCurrencyInstance(locale);
			} else {
				return NumberFormat.getCurrencyInstance();
			}			
		} else if (type.equals(PERCENT)) {
			if (locale != null) {
				return NumberFormat.getPercentInstance(locale);
			} else {
				return NumberFormat.getPercentInstance();
			}			
		} else {
			throw new ParamException("Invalid value for parameter "+getName()+
			"Unknown type argument: "+type);
		}
	}
}
