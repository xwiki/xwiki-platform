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
 * User: ludovic
 * Date: 8 mars 2004
 * Time: 08:55:40
 */

package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.filter.XWikiFilter;
import com.xpn.xwiki.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.api.engine.ImageRenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.filter.Filter;
import org.radeox.filter.FilterPipe;
import org.radeox.util.Service;

import java.net.URL;
import java.util.Iterator;

public class XWikiRadeoxRenderEngine extends BaseRenderEngine implements WikiRenderEngine, ImageRenderEngine {
    private static Log log = LogFactory.getLog(XWikiRadeoxRenderEngine.class);
    private XWikiContext context;

    public XWikiRadeoxRenderEngine(XWikiContext context) {
        // super();
        this.setContext(context);
    }

    public XWikiRadeoxRenderEngine(InitialRenderContext ircontext, XWikiContext context) {
        super(ircontext);
        this.setContext(context);
    }

    public XWikiContext getContext() {
        return context;
    }

    public void setContext(XWikiContext context) {
        this.context = context;
    }

    // Overidding to load our own Filter list.
    protected void init() {
        if (null == fp) {
            fp = new FilterPipe(initialContext);

            Iterator iterator = Service.providers(XWikiFilter.class);
            while (iterator.hasNext()) {
                try {
                    Filter filter = (Filter) iterator.next();
                    fp.addFilter(filter);
                    log.debug("Loaded filter: " + filter.getClass().getName());
                } catch (Exception e) {
                    log.warn("BaseRenderEngine: unable to load filter", e);
                }
            }

            fp.init();
            //Logger.debug("FilterPipe = "+fp.toString());
        }
    }

    public String noaccents(String name) {
        Util util = context.getUtil();
        return StringUtils.replace( util.noaccents(name), " ", "");
    }

    public boolean exists(String name) {
        String database = context.getDatabase();
        try {
            int colonIndex = name.indexOf(":");
            if (colonIndex!=-1) {
                String db = name.substring(0,colonIndex);
                name = name.substring(colonIndex + 1);
                context.setDatabase(db);
            }

            XWikiDocument currentdoc = ((XWikiDocument) context.get("doc"));

            int qsIndex = name.indexOf("?");
            if (qsIndex!=-1) {
                name = name.substring(0, qsIndex);
            }

            String newname = noaccents(name);
            XWikiDocument doc = new XWikiDocument(
                    (currentdoc!=null) ? currentdoc.getWeb() : "Main",
                    newname);
            return context.getWiki().exists(doc.getFullName(), context);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            context.setDatabase(database);
        }

    }

    public boolean showCreate() {
        return true;
    }

    public void appendLink(StringBuffer buffer, String name, String view, String anchor) {
        String database = context.getDatabase();
        XWikiContext context = getContext();

        try {
            String db = null;
            int colonIndex = name.indexOf(":");
            if (colonIndex!=-1) {
                db = name.substring(0,colonIndex);
                name = name.substring(colonIndex + 1);
                context.setDatabase(db);
            }

            String querystring = null;
            int qsIndex = name.indexOf("?");
            if (qsIndex!=-1) {
                querystring = name.substring(qsIndex+1);
                name = name.substring(0, qsIndex);
            }

            buffer.append("<span class=\"wikilink\"><a href=\"");
            String newname = noaccents(name);
            XWikiDocument newdoc = new XWikiDocument();
            if (newname.indexOf(".")!=-1) {
                try {
                    newdoc.setFullName(newname, context);
                } catch (XWikiException e) {
                }
            } else {
                newdoc.setWeb(context.getDoc().getWeb());
                newdoc.setName(newname);
            }

            URL url = context.getURLFactory().createURL(newdoc.getWeb(), newdoc.getName(),
                    "view", querystring, anchor, context);
            buffer.append(context.getURLFactory().getURL(url, context));
            buffer.append("\">");
            buffer.append(view);
            buffer.append("</a></span>");
        } finally {
            context.setDatabase(database);
        }
    }


    public void appendLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer, name, view, null);
    }

    public void appendCreateLink(StringBuffer buffer, String name, String view) {
        String database = context.getDatabase();
        XWikiContext context = getContext();

        try {
            String db = null;
            int colonIndex = name.indexOf(":");
            if (colonIndex!=-1) {
                db = name.substring(0,colonIndex);
                name = name.substring(colonIndex + 1);
                context.setDatabase(db);
            }

            int qsIndex = name.indexOf("?");
            if (qsIndex!=-1) {
                name = name.substring(0, qsIndex);
            }


            String newname = noaccents(name);
            XWikiDocument newdoc = new XWikiDocument();
            if (newname.indexOf(".")!=-1) {
                try {
                    newdoc.setFullName(newname, context);
                } catch (XWikiException e) {
                }
            } else {
                newdoc.setWeb(context.getDoc().getWeb());
                newdoc.setName(newname);
            }

            String querystring = null;
            XWikiDocument currentdoc = context.getDoc();
            if (currentdoc!=null) {
                querystring = "parent=" + currentdoc.getFullName();
            }

            URL url = context.getURLFactory().createURL(newdoc.getWeb(), newdoc.getName(),
                    "edit", querystring, null, context);
            String surl = context.getURLFactory().getURL(url, context);
            buffer.append("<a class=\"wikicreatelink\" href=\"");
            buffer.append(surl);
            buffer.append("\">");
            buffer.append("<span class=\"wikicreatelinktext\">");
            buffer.append(view);
            buffer.append("</span>");
            buffer.append("<span class=\"wikicreatelinkqm\">?</span>");
            buffer.append("</a>");
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Get a link to an image. This can be used by filters or
     * macros to get images for e.g. external links or icons
     * Should be refactored to get other images as well
     *
     * @return result String with an HTML link to an image
     */
    public String getExternalImageLink() {
        return "";
    }
}
