package com.xpn.xwiki.plugin.charts.mocks;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.Chart;
import com.xpn.xwiki.plugin.charts.ChartImpl;
import com.xpn.xwiki.plugin.charts.ChartingPlugin;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

public class MockChartingPlugin extends ChartingPlugin {
	public MockChartingPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
	}

	public Chart generateChart(ChartParams params, XWikiContext context) throws GenerateException {
		return new ChartImpl(params, "http://hahahahahhaha", "http://gsfdgdfg");
	}
}
