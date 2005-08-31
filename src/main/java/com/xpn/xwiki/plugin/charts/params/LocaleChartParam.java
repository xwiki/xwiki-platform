package com.xpn.xwiki.plugin.charts.params;

import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
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
		String language = getStringArg(map, LANGUAGE);
		try {
			String country = getStringArg(map, COUNTRY);
			try {
				String variant = getStringArg(map, VARIANT);
				return new Locale(language, country, variant);
			} catch (MissingArgumentException e) {
				return new Locale(language, country);
			}
		} catch (MissingArgumentException e) {
			return new Locale(language);
		}		
	}
}
