package com.xpn.xwiki.plugin.charts.params;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.axis.DateTickUnit;

import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class DateTickUnitChartParam extends DateFormatChartParam {
	private Map unitChoice;
	
	public DateTickUnitChartParam(String name) {
		super(name);
		init();
	}

	public DateTickUnitChartParam(String name, boolean optional) {
		super(name, optional);
		init();
	}
	
	public void init() {
		unitChoice = new HashMap();
		unitChoice.put("day", new Integer(DateTickUnit.DAY));
		unitChoice.put("hour", new Integer(DateTickUnit.HOUR));
		unitChoice.put("millisecond", new Integer(DateTickUnit.MILLISECOND));
		unitChoice.put("minute", new Integer(DateTickUnit.MINUTE));
		unitChoice.put("month", new Integer(DateTickUnit.MONTH));
		unitChoice.put("second", new Integer(DateTickUnit.SECOND));
		unitChoice.put("year", new Integer(DateTickUnit.YEAR));
	}

	public Class getType() {
		return DateTickUnit.class;
	}

	public Object convert(String value) throws ParamException {
		Map map = parseMap(value);
		DateFormat format;
		int unit = ((Integer)getChoiceArg(map, "unit", unitChoice)).intValue();
		int count = getIntArg(map, "count");
		try {
			return new DateTickUnit(unit, count, (DateFormat)super.convert(value));
		} catch (MissingArgumentException e) {
			return new DateTickUnit(unit, count);
		}
	}
}
