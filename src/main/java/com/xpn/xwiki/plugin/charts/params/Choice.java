package com.xpn.xwiki.plugin.charts.params;

public class Choice {
	private String selector;
	private Object value;

	public Choice(String selector, Object value) {
		this.selector = selector;
		this.value = value;
	}

	public String getSelector() {
		return selector;
	}

	public Object getValue() {
		return value;
	}
}
