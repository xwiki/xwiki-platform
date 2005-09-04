package com.xpn.xwiki.plugin.charts;

import java.util.Iterator;
import java.util.LinkedHashMap;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.XWikiRenderingEngine;

public class CustomXWikiRenderingEngine extends XWikiRenderingEngine {
	public CustomXWikiRenderingEngine(XWiki xwiki, XWikiContext context) throws XWikiException {
		super(xwiki, context);
    	Iterator it = getRendererNames().iterator();
    	LinkedHashMap map = new LinkedHashMap();
    	boolean found = false;
    	while (it.hasNext()) {
    		String name = (String)it.next();    		
    		XWikiRenderer renderer = getRenderer(name);
    		if (renderer instanceof XWikiRadeoxRenderer || found) {
    			found = true;
    			map.put(name, renderer);
    		}
    	}
    	
    	it = map.keySet().iterator();
    	while (it.hasNext()) {
    		removeRenderer((String)it.next());
    	}
	}
}
