package com.xpn.xwiki.plugin.charts.params;

import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class LocaleChartParam extends AbstractChartParam {
	public static final String LANGUAGE = "language";
	public static final String COUNTRY = "country";
	public static final String VARIANT = "variant";

	public LocaleChartParam(String name) {
		super(name);
	}

	public LocaleChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Locale.class;
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value);
		String language = getStringParam(map, LANGUAGE);
		try {
			String country = getStringParam(map, COUNTRY);
			try {
				String variant = getStringParam(map, VARIANT);
				return new Locale(language, country, variant);
			} catch (ParamException e) {
				return new Locale(language, country);
			}
		} catch (ParamException e) {
			return new Locale(language);
		}		
	}
}
