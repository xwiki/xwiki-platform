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
package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.filter.XWikiFilter;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.ImageRenderEngine;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.filter.Filter;
import org.radeox.filter.FilterPipe;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.context.BaseFilterContext;
import org.radeox.util.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Before this class can be used you need to call setXWikiContext().
 */
public class XWikiRadeoxRenderEngine extends BaseRenderEngine implements WikiRenderEngine, ImageRenderEngine {
    private static Log log = LogFactory.getLog(XWikiRadeoxRenderEngine.class);
    private XWikiContext xwikiContext;
    protected FilterPipe fp;

    public XWikiRadeoxRenderEngine(InitialRenderContext ircontext) {
        super(ircontext);
        init();
    }

    public XWikiContext getXWikiContext() {
        return this.xwikiContext;
    }

    public void setXWikiContext(XWikiContext context) {
        this.xwikiContext = context;
    }

    /**
     * We override this method from {@link BaseRenderEngine} in order to provide our own initialization of Filters.
     * In this manner we can load our filter definition from the
     * META-INF/services/com.xpn.xwiki.render.filter.XWikiFilter file.
     */
    protected void init()
    {
        fp = new FilterPipe(initialContext);

        Iterator iterator = Service.providers(XWikiFilter.class);
        while (iterator.hasNext()) {
            try {
                Filter filter = (Filter) iterator.next();
                fp.addFilter(filter);
                log.debug("Radeox filter [" + filter.getClass().getName() + "] loaded");
            } catch (Exception e) {
                log.error("Failed to load Radeox filter", e);
            }
        }

        fp.init();
    }

    /**
     * Render an input with text markup and return a String with
     * e.g. HTML
     *
     * @param content String with the input to render
     * @param context Special context for the filter engine, e.g. with
     *                configuration information
     * @return result Output with rendered content
     */
    public String render(String content, RenderContext context) {
      FilterContext filterContext = new BaseFilterContext();
      filterContext.setRenderContext(context);
      return fp.filter(content, filterContext);
    }

    public String noaccents(String name) {
//        Util util = context.getUtil();
        return StringUtils.replace( Util.noaccents(name), " ", "");
    }

    /**
     * @param name the name of a wiki page
     * @return true if the page exists or false otherwise
     * @see org.radeox.api.engine.WikiRenderEngine#exists(String)
     */
    public boolean exists(String name) {
        String database = getXWikiContext().getDatabase();
        try {
            int colonIndex = name.indexOf(":");
            if (colonIndex!=-1) {
                String db = name.substring(0,colonIndex);
                name = name.substring(colonIndex + 1);
                getXWikiContext().setDatabase(db);
            }

            XWikiDocument currentdoc = getXWikiContext().getDoc();

            int qsIndex = name.indexOf("?");
            if (qsIndex!=-1) {
                name = name.substring(0, qsIndex);
            }

            // + is use for spaces
            name = name.replaceAll("\\+", " ");
            String newname = noaccents(name);
            XWikiDocument doc = new XWikiDocument(
                    (currentdoc!=null) ? currentdoc.getSpace() : "Main",
                    newname);
            boolean exists = getXWikiContext().getWiki().exists(doc.getFullName(), getXWikiContext());

            // If the document exists with the spaces and accents converted then we use this one
            if (exists)
                return true;

            // if the document does not exists then we check the one not converted
            doc = new XWikiDocument(
                    (currentdoc!=null) ? currentdoc.getSpace() : "Main",
                     name);
            return getXWikiContext().getWiki().exists(doc.getFullName(), getXWikiContext());
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            getXWikiContext().setDatabase(database);
        }

    }

    public boolean showCreate() {
        return true;
    }

    /**
     * Appends for example the &lt;a href&gt; HTML code for linking to a wiki
     * page with the given name to the passed buffer.
     *
     * @param buffer the string to append to
     * @param name the name of the wiki page pointed to by the link
     * @param view the text that will be shown to the user for the link
     * @param anchor the anchor specified in the link if any (can be null)
     * @see org.radeox.api.engine.WikiRenderEngine#appendLink(StringBuffer, String, String, String) 
     */
    public void appendLink(StringBuffer buffer, String name, String view, String anchor) {
        // allow using spaces in links to anchors
        if (anchor != null) anchor = anchor.replaceAll(" ", "+");

        if (name.length() == 0 && anchor != null) {
            appendInternalLink(buffer, view, anchor); 
        } else {
            String database = getXWikiContext().getDatabase();
            XWikiContext context = getXWikiContext();
    
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
                // TODO: This causes problems with [C++ Examples]
                name = name.replaceAll("\\+", " ");

                // If the document exists with the conversion of spaces and accents
                // then we use this one
                XWikiDocument newdoc = new XWikiDocument();
                String newname = noaccents(name);
                if (name.indexOf(".")!=-1) {
                        newdoc.setFullName(name, context);
                } else {
                    newdoc.setSpace(context.getDoc().getSpace());
                    newdoc.setName(name);
                }

                // If the document does not exist, then we use the normal name as is
                if (!context.getWiki().exists(newdoc.getFullName(), context)) {
                    if (newname.indexOf(".")!=-1) {
                            newdoc.setFullName(newname, context);
                    } else {
                        newdoc.setSpace(context.getDoc().getSpace());
                        newdoc.setName(newname);
                    }
                }

                if ((db==null)||(database.equals(db)))
                 addLinkToContext(newdoc.getFullName(), context);

                URL url = context.getURLFactory().createURL(newdoc.getSpace(), newdoc.getName(),
                        "view", querystring, anchor, context);
                buffer.append(context.getURLFactory().getURL(url, context));
                buffer.append("\">");
                buffer.append(cleanText(view));
                buffer.append("</a></span>");
            } finally {
                context.setDatabase(database);
            }
        }
    }

    private String cleanText(String text) {
        return Util.escapeText(text);
    }

    private void addLinkToContext(String docname, XWikiContext context) {
        // Add to backlinks in context object
        try {
                List links = (List) context.get("links");
                if (links==null) {
                    links = new ArrayList();
                    context.put("links", links);
                }
                // We restrict the number of bytes in the document name as:
                // 1. It will throw an error on some DBMSs, as 255 is the column length
                // 2. Such a long document name is not likely to occur, since the same limit is
                //    imposed on the document name length.
                if (!links.contains(docname) && docname.getBytes().length <= 255) {
                    links.add(docname);
                }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error adding link to context", e);
            }
        }
    }

    public void appendLink(StringBuffer buffer, String name, String view) {
        appendLink(buffer, name, view, null);
    }
    
    public void appendInternalLink(StringBuffer buffer, String view, String anchor) {
        buffer.append("<span class=\"wikilink\"><a href=\"#");
        buffer.append(anchor);
        buffer.append("\">");
        if (view.length() == 0) view = Utils.decode(anchor, getXWikiContext());
        buffer.append(cleanText(view));
        buffer.append("</a></span>");
    }

    public void appendCreateLink(StringBuffer buffer, String name, String view) {
        String database = getXWikiContext().getDatabase();
        XWikiContext context = getXWikiContext();

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

            // + is used for spaces
            name = name.replaceAll("\\+", " ");

            String newname = name;
            XWikiDocument newdoc = new XWikiDocument();
            if (newname.indexOf(".")!=-1) {
                    newdoc.setFullName(newname, context);
            } else {
                newdoc.setSpace(context.getDoc().getSpace());
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
                querystring += "&amp;editor=" + editor;

            URL url = context.getURLFactory().createURL(newdoc.getSpace(), newdoc.getName(),
                    "edit", querystring, null, context);
            String surl = context.getURLFactory().getURL(url, context);
            buffer.append("<a class=\"wikicreatelink\" href=\"");
            buffer.append(surl);
            buffer.append("\">");
            buffer.append("<span class=\"wikicreatelinktext\">");
            buffer.append(cleanText(view));
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
