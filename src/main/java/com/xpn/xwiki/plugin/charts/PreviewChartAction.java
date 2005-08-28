package com.xpn.xwiki.plugin.charts;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public class PreviewChartAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
                
        ChartingPluginApi chartingPlugin = (ChartingPluginApi)
		context.getWiki().getPluginApi("charting", context);
        if (chartingPlugin == null) {
        	throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
        			XWikiException.ERROR_XWIKI_UNKNOWN, "ChartingPlugin not loaded");
        }
                
        try {
            Map map = map(request);
            ChartParams params = new ChartParams(map, DefaultChartParams.getInstance());
        	Chart chart = chartingPlugin.generateChart(params, context);
        	response.getWriter().print(chart.getImageURL());        
        } catch (ParamException e) {
        	throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
        			XWikiException.ERROR_XWIKI_UNKNOWN, e.getMessage(), e);
        } catch (GenerateException e) {
        	throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
        			XWikiException.ERROR_XWIKI_UNKNOWN, e.getMessage(), e);
        } catch (IOException e) {
        	throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
        			XWikiException.ERROR_XWIKI_UNKNOWN, e.getMessage(), e);
        }
        
		return null;
	}
	
	private static Map map(XWikiRequest request) {
		Map map = new LinkedHashMap();
    	Enumeration e = request.getParameterNames();
    	while (e.hasMoreElements()) {
    		String name = (String)e.nextElement();
    		String value = request.getParameter(name);
    		map.put(name, value);
    	}		
		return map;
	}
}
