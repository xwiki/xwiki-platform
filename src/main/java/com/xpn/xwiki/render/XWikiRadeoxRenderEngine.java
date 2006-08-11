/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ludovic
 * @author sdumitriu
 * @author thomas
 * @author tepich
 */


package com.xpn.xwiki.render;

import java.net.URL;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.ImageRenderEngine;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.filter.Filter;
import org.radeox.filter.FilterPipe;
import org.radeox.util.Service;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.filter.XWikiFilter;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import java.util.List;
import java.util.ArrayList;

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

            // + is use for spaces
            name = name.replace('+',' ');
            String newname = noaccents(name);
            XWikiDocument doc = new XWikiDocument(
                    (currentdoc!=null) ? currentdoc.getWeb() : "Main",
                    newname);
            boolean exists = context.getWiki().exists(doc.getFullName(), context);

            // If the document exists with the spaces and accents converted then we use this one
            if (exists)
                return true;

            // if the document does not exists then we check the one not converted
            doc = new XWikiDocument(
                    (currentdoc!=null) ? currentdoc.getWeb() : "Main",
                     name);
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
        // allow using spaces in links to anchors
        if (anchor != null) anchor = anchor.replace(' ','+');
        
        if (name.length() == 0 && anchor != null) {
            appendInternalLink(buffer, view, anchor); 
        } else {
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

                // + is use for spaces
                name = name.replace('+',' ');
                // If the document exists with the conversion of spaces and accents
                // then we use this one
                String newname = noaccents(name);
                XWikiDocument newdoc = new XWikiDocument();
                if (newname.indexOf(".")!=-1) {
                        newdoc.setFullName(newname, context);
                } else {
                    newdoc.setWeb(context.getDoc().getWeb());
                    newdoc.setName(newname);
                }

                // If the document does not exist, then we use the normal name as is
                if (!context.getWiki().exists(newdoc.getFullName(), context)) {
                    if (name.indexOf(".")!=-1) {
                            newdoc.setFullName(name, context);
                    } else {
                        newdoc.setWeb(context.getDoc().getWeb());
                        newdoc.setName(name);
                    }
                }


                if ((db==null)||(database.equals(db)))
                 addLinkToContext(newdoc.getFullName(), context);

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
    }

    private void addLinkToContext(String docname, XWikiContext context) {
        // Add to backlinks in context object
        try {
                List links = (List) context.get("links");
                if (links==null) {
                    links = new ArrayList();
                    context.put("links", links);
                }
                if (!links.contains(docname))
                    links.add(docname);
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Error adding link to context", e);
        }
    }


    public void appendLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer, name, view, null);
    }
    
    public void appendInternalLink(StringBuffer buffer, String view, String anchor) {
        buffer.append("<span class=\"wikilink\"><a href=\"#");
        buffer.append(anchor);
        buffer.append("\">");
        if (view.length() == 0) view = Utils.decode(anchor, context);
        buffer.append(view);
        buffer.append("</a></span>");
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

            // + is use for spaces
            name = name.replace('+',' ');

            String newname = name;
            XWikiDocument newdoc = new XWikiDocument();
            if (newname.indexOf(".")!=-1) {
                    newdoc.setFullName(newname, context);
            } else {
                newdoc.setWeb(context.getDoc().getWeb());
                newdoc.setName(newname);
            }

            String querystring = null;
            XWikiDocument currentdoc = context.getDoc();
            if (currentdoc!=null) {
                querystring = "parent=" + currentdoc.getFullName();
            }

            if ((db==null)||(database.equals(db)))
             addLinkToContext(newdoc.getFullName(), context);

            String editor = context.getWiki().getEditorPreference(context);
            if ((!editor.equals("")&&(!editor.equals("text")))&&(!editor.equals("---")))
                querystring += "&editor=" + editor;

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
