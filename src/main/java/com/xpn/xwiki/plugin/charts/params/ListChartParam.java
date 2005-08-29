package com.xpn.xwiki.plugin.charts.params;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ListChartParam extends AbstractChartParam {
	private ChartParam param;

	public ListChartParam(ChartParam param) {
		this(param, true);
	}

	public ListChartParam(ChartParam param, boolean optional) {
		super(param.getName(), optional);
		this.param = param;
	}
	
	public Class getType() {
		return List.class;
	}

	public Object convert(String value) throws ParamException {
		List list = parseList(value);
		List result = new ArrayList(list.size());
		Iterator it = list.iterator();
		while (it.hasNext()) {
			String s = (String)it.next();
			result.add(param.convert(s));
		}
		return result;
	}
}
