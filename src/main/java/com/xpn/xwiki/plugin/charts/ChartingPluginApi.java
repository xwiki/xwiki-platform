package com.xpn.xwiki.plugin.charts;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

public class ChartingPluginApi extends Api {
	private ChartingPlugin plugin;

	public ChartingPluginApi(ChartingPlugin plugin, XWikiContext context) {
        super(context);
        setPlugin(plugin);
	}

	public void setPlugin(ChartingPlugin plugin) {
		this.plugin = plugin;
	}
	
	public ChartingPlugin getPlugin() {
		return plugin;
	}
	
	public Chart generateChart(ChartParams params, XWikiContext context) throws GenerateException {
		return plugin.generateChart(params, context);
	}
	
    public void outputFile(String filename, XWikiContext context) throws IOException {
    	plugin.outputFile(filename, context);
    }
}
