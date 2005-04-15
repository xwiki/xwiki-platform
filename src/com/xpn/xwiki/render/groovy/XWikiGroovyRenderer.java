/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 6 juil. 2004
 * Time: 12:22:18
 */
package com.xpn.xwiki.render.groovy;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import groovy.text.Template;

import java.util.Map;
import java.util.HashMap;

public class XWikiGroovyRenderer implements XWikiRenderer {
    private GroovyTemplateEngine engine = new GroovyTemplateEngine();
    private XWikiCache cache;

    public XWikiGroovyRenderer() {
    }


    public void flushCache() {
        if (cache!=null)
         cache.flushAll();
    }

    public Map prepareContext(XWikiContext context) {
            if (cache==null) {
                 cache = context.getWiki().getCacheService().newCache(100);
            }

            Map gcontext = (Map) context.get("gcontext");
            if (gcontext==null)
                gcontext = new HashMap();
            gcontext.put("xwiki", new XWiki(context.getWiki(), context));
            gcontext.put("request", context.getRequest());
            gcontext.put("response", context.getResponse());
            gcontext.put("context", new Context(context));

            // Put the Grrovy Context in the context
            // so that includes can use it..
            context.put("gcontext", gcontext);
            return gcontext;
        }

    public String evaluate(String content, String name, Map gcontext) {
        Template template = null;
        try {
            try {
             template = (Template) cache.getFromCache(content);
            } catch (XWikiCacheNeedsRefreshException e) {
                template = engine.createTemplate(content);
                cache.putInCache(content, template);
            }
            return template.make(gcontext).toString();
        } catch (Exception e) {
            e.printStackTrace();
            Object[] args =  { name };

            String title;
            String text;

            XWikiException xe = new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_RENDERING_GROOVY_EXCEPTION,
                                                        "Error while parsing groovy page {0}", e, args);
            title = xe.getMessage();
            text = com.xpn.xwiki.XWiki.getFormEncoded(xe.getFullMessage());

            return "<a href=\"\" onclick=\"document.getElementById('xwikierror').style.display='block'; return false;\">"
                    + title + "</a><div id=\"xwikierror\" style=\"display: none;\"><pre>\n"
                    + text + "</pre></div>";
        }
    }

    public String render(String content, XWikiDocument contentdoc, XWikiDocument contextdoc, XWikiContext context) {
        if (content.indexOf("<%")==-1)
            return content;

        if (!context.getWiki().getRightService().hasProgrammingRights(contentdoc, context))
             return content;

        Map gcontext = null;
        try {
            String name = contextdoc.getFullName();
            gcontext = prepareContext(context);
            Document previousdoc = (Document) gcontext.get("doc");

            try {
                gcontext.put("doc", new Document(contextdoc, context));
                return evaluate(content, name, gcontext);
            } finally {
                if (previousdoc!=null)
                    gcontext.put("doc", previousdoc);
            }

        } finally {
        }
    }
}
