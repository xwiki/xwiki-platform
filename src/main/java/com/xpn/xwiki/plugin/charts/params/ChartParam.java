package com.xpn.xwiki.plugin.charts.params;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public interface ChartParam {
	
	public String getName();
	
	public Class getType();
	
	public boolean isOptional(); 
	
	public Object convert(String value) throws ParamException;
}
