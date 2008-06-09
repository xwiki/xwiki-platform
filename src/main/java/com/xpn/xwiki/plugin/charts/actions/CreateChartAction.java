/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.charts.RadeoxHelper;
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

public class CreateChartAction extends XWikiAction {

	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        ChartParams params;
        Map map = map(request);
        try {
        	params = new ChartParams(map, DefaultChartParams.getInstance());
        	params.check();
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
