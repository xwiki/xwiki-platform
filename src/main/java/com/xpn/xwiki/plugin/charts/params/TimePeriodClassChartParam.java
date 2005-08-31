package com.xpn.xwiki.plugin.charts.params;

import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Month;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.Second;
import org.jfree.data.time.Week;
import org.jfree.data.time.Year;

public class TimePeriodClassChartParam extends ChoiceChartParam {

	public TimePeriodClassChartParam(String name) {
		super(name);
	}

	public TimePeriodClassChartParam(String name, boolean isOptional) {
		super(name, isOptional);
	}

	protected void init() {
		addChoice("year", Year.class);
		addChoice("quarter", Quarter.class);
		addChoice("month", Month.class);
		addChoice("week", Week.class);
		addChoice("day", Day.class);
		addChoice("hour", Hour.class);
		addChoice("minute", Minute.class);
		addChoice("second", Second.class);
		addChoice("millisecond", Millisecond.class);
	}

	public Class getType() {
		return Class.class;
	}
}
