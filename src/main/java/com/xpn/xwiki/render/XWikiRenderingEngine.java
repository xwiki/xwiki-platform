/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 20:48:17
 */
package com.xpn.xwiki.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.render.groovy.XWikiGroovyRenderer;
import com.xpn.xwiki.util.Util;

public class XWikiRenderingEngine {

    private List renderers = new ArrayList();
    private HashMap renderermap = new LinkedHashMap();

    public XWikiRenderingEngine(XWiki xwiki, XWikiContext context) throws XWikiException {
        if (xwiki.Param("xwiki.render.macromapping", "0").equals("1"))
            addRenderer("mapping", new XWikiMacrosMappingRenderer(xwiki, context));
        // addRenderer(new XWikiJSPRenderer());
        if (xwiki.Param("xwiki.render.velocity", "1").equals("1"))
            addRenderer("velocity", new XWikiVelocityRenderer());
        if (xwiki.Param("xwiki.render.groovy", "1").equals("1"))
            addRenderer("groovy", new XWikiGroovyRenderer());
        if (xwiki.Param("xwiki.render.plugin", "1").equals("1"))
            addRenderer("plugin", new XWikiPluginRenderer());

        // The first should not removePre
        // The last one should removePre
        if (xwiki.Param("xwiki.render.wiki", "1").equals("1"))
            addRenderer("wiki", new XWikiRadeoxRenderer(false));

        // if (xwiki.Param("xwiki.render.wiki2", "1").equals("1"))
        // addRenderer("wiki2", new XWikiWikiParser2Renderer(false));

        if (xwiki.Param("xwiki.render.wikiwiki", "0").equals("1")) {
            addRenderer("xwiki", new XWikiWikiBaseRenderer(true, true));
        } else {
            addRenderer("xwiki", new XWikiWikiBaseRenderer(false, true));
        }
    }

    public void addRenderer(String name, XWikiRenderer renderer) {
        renderers.add(renderer);
        renderermap.put(name, renderer);
    }
    
    public XWikiRenderer getRenderer(String name) {
		return (XWikiRenderer) renderermap.get(name);
	}

	public List getRendererList() {
		return (List) ((ArrayList) renderers).clone();
	}

	public List getRendererNames() {
		return new LinkedList(renderermap.keySet());
	}
	
	public XWikiRenderer removeRenderer(String name) {
		XWikiRenderer result = (XWikiRenderer) renderermap.remove(name);
		if (result != null) {
			renderers.remove(result);
		}
		return result;
	}

    public String renderDocument(XWikiDocument doc, XWikiContext context) throws XWikiException {
           return renderText(doc.getTranslatedContent(context), doc, context);
    }

    public String renderDocument(XWikiDocument doc, XWikiDocument includingdoc, XWikiContext context) throws XWikiException {
        return renderText(doc.getTranslatedContent(context), includingdoc, context);
    }

    public String renderText(String text, XWikiDocument includingdoc, XWikiContext context) {
        return renderText(text, includingdoc, includingdoc, context);
    }

    public String renderText(String text, XWikiDocument contentdoc, XWikiDocument includingdoc, XWikiContext context) {
        MonitorPlugin monitor  = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor!=null)
             monitor.startTimer("rendering");

        XWikiDocument doc = context.getDoc();
        XWikiDocument cdoc = context.getDoc();

        // Let's call the beginRendering loop
        context.getWiki().getPluginManager().beginRendering(context);

        String content = text;

        // Which is the current idoc and sdoc
        XWikiDocument idoc = (XWikiDocument) context.get("idoc");
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");
        // We put the including and security doc in the context
        // It will be needed to verify programming rights
        context.put("idoc", includingdoc);
        context.put("sdoc", contentdoc);

        try {

            for (int i=0;i<renderers.size();i++)
                content = ((XWikiRenderer)renderers.get(i)).render(content, contentdoc, includingdoc, context);
        } finally {
            // Remove including doc or set the previous one
            if (idoc==null)
             context.remove("idoc");
            else
             context.put("idoc", idoc);

            // Remove security doc or set the previous one
            if (sdoc==null)
             context.remove("sdoc");
            else
             context.put("sdoc", sdoc);

            // Let's call the endRendering loop
            context.getWiki().getPluginManager().endRendering(context);
        }

        return content;
        }
        finally {
               if (monitor!=null)
                   monitor.endTimer("rendering");
        }
    }

    public void flushCache() {
        for (int i=0;i<renderers.size();i++)
           ((XWikiRenderer)renderers.get(i)).flushCache();
    }

    public String convertMultiLine(String macroname, String params, String data, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        String language = macro.getLanguage();
        XWikiRenderer renderer = (XWikiRenderer) renderermap.get(language);
        if (renderer==null)
            return allcontent;
        else
            return renderer.convertMultiLine(macroname, params, data, allcontent, macro, context);
    }

    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro, XWikiContext context) {
        String language = macro.getLanguage();
        XWikiRenderer renderer = (XWikiRenderer) renderermap.get(language);
        if (renderer==null)
            return allcontent;
        else
            return renderer.convertSingleLine(macroname, params, allcontent, macro, context);
    }

}
