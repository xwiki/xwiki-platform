package com.xpn.xwiki.plugin.charts;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public class CreateChartAction extends XWikiAction {

	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        ChartParams params;
        Map map = map(request);
        try {
        	params = new ChartParams(map, DefaultChartParams.getInstance());
        } catch (ParamException e) {
        	throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
        			XWikiException.ERROR_XWIKI_UNKNOWN, e.getMessage(), e);
        }
        String chartRadeoxMacro = RadeoxHelper.buildMacro("chart", map);
        XWikiDocument doc = context.getDoc();
        doc.setContent(doc.getContent()+"\n\n"+chartRadeoxMacro);
        context.getWiki().saveDocument(doc, context);
		try {
			response.sendRedirect(doc.getURL("view", true, context));
		} catch(IOException ex) {
        	throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
        			XWikiException.ERROR_XWIKI_UNKNOWN, ex.getMessage(), ex);
        }
		return null;
	}
	
	private Map map(XWikiRequest request) {
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
