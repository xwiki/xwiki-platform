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
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.render.filter.XWikiFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.filter.Filter;
import org.radeox.filter.FilterPipe;
import org.radeox.util.Service;

import java.util.Iterator;

public class XWikiRadeoxRenderEngine extends BaseRenderEngine implements WikiRenderEngine {
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

     public boolean exists(String name) {
        String database = context.getDatabase();
        try {
            int colonIndex = name.indexOf(":");
            if (colonIndex!=-1) {
                String db = name.substring(0,colonIndex);
                name = name.substring(colonIndex + 1);
                context.setDatabase(db);
            }

            XWikiDocInterface currentdoc = ((XWikiDocInterface) context.get("doc"));
            String newname = StringUtils.replace(name, " ", "");
            XWikiDocInterface doc = context.getWiki().getDocument(
                    (currentdoc!=null) ? currentdoc.getWeb() : "Main",
                    newname, context);
            if ((doc==null)||doc.isNew())
                return false;
            else
                return true;
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
        XWikiContext context = getContext();

        String database = null;
        int colonIndex = name.indexOf(":");
        if (colonIndex!=-1) {
            database = name.substring(0,colonIndex);
            name = name.substring(colonIndex + 1);
        }

        String baseurl = context.getWiki().getBaseUrl(database, context);
        buffer.append("<span class=\"wikilink\"><a href=\"");
        buffer.append(baseurl);
        buffer.append("view");
        buffer.append("/");


        String newname = StringUtils.replace(name, " ", "");

        if (newname.indexOf(".")!=-1) {
            newname = StringUtils.replace(newname, ".","/", 1);
        } else {
            newname = ((XWikiDocInterface)context.get("doc")).getWeb() + "/" + newname;
        }

        buffer.append(newname);
        if (anchor!=null) {
            buffer.append("#");
            buffer.append(anchor);
        }

        buffer.append("\">");
        buffer.append(view);
        buffer.append("</a></span>");
    }


    public void appendLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer, name, view, null);
    }

    public void appendCreateLink(StringBuffer buffer, String name, String view) {
        XWikiContext context = getContext();

        String database = null;
        int colonIndex = name.indexOf(":");
        if (colonIndex!=-1) {
            database = name.substring(0,colonIndex);
            name = name.substring(colonIndex + 1);
        }

        String baseurl = context.getWiki().getBaseUrl(database, context);
        buffer.append("<span class=\"wikicreatelink\">");
        buffer.append(view);
        buffer.append("<a href=\"");
        buffer.append(baseurl);
        buffer.append("edit");
        buffer.append("/");

        String newname = StringUtils.replace(name, " ", "");

        if (newname.indexOf(".")!=-1) {
            newname = StringUtils.replace(newname, ".","/", 1);
        } else {
            newname = ((XWikiDocInterface)context.get("doc")).getWeb() + "/" + newname;
        }

        buffer.append(newname);
        XWikiDocInterface currentdoc = ((XWikiDocInterface) context.get("doc"));
        if (currentdoc!=null) {
            buffer.append("?parent=");
            buffer.append(currentdoc.getFullName());
        }

        buffer.append("\">");
        buffer.append("?");
        buffer.append("</a></span>");
    }


}
