package com.xpn.xwiki.plugin.charts.params;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class DateChartParam extends DateFormatChartParam {

	public DateChartParam(String name) {
		super(name);
	}

	public DateChartParam(String name, boolean optional) {
		super(name, optional);
	}

	public Class getType() {
		return Date.class;
	}
	
	public Object convert(String value) throws ParamException {
		Map map = parseMap(value);
		DateFormat format;
		try {
			format = (DateFormat)super.convert(value); 
		} catch (ParamException e) {
			format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		}
		try {
			return format.parse(getStringArg(map, "value"));
		} catch (ParseException e) {
			throw new InvalidParamException("Invalid value for parameter :"+getName(), e);
		}
	}
}
