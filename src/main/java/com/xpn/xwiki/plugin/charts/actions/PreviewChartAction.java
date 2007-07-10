/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package com.xpn.xwiki.plugin.charts.actions;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.charts.Chart;
import com.xpn.xwiki.plugin.charts.ChartingPluginApi;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

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
