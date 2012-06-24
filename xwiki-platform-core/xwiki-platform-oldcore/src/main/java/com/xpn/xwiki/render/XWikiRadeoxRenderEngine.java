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
 */
package com.xpn.xwiki.render;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.radeox.api.engine.ImageRenderEngine;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.filter.FilterPipe;
import org.radeox.filter.context.BaseFilterContext;
import org.radeox.filter.context.FilterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

public class XWikiRadeoxRenderEngine extends BaseRenderEngine implements WikiRenderEngine, ImageRenderEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiRadeoxRenderEngine.class);

    private XWikiContext xwikiContext;

    protected FilterPipe fp;

    public XWikiRadeoxRenderEngine(XWikiContext xwikiContext)
    {
        setXWikiContext(xwikiContext);
    }

    public XWikiRadeoxRenderEngine(InitialRenderContext ircontext, FilterPipe filterPipe, XWikiContext xwikiContext)
    {
        super(ircontext);

        setXWikiContext(xwikiContext);
        this.fp = filterPipe;
    }

    public XWikiContext getXWikiContext()
    {
        return this.xwikiContext;
    }

    public void setXWikiContext(XWikiContext context)
    {
        this.xwikiContext = context;
    }

    /**
     * @see com.xpn.xwiki.render.XWikiRadeoxRenderer#initFilterPipe
     */
    @Override
    protected void init()
    {
        // Do nothing and thus ensure that the filter Pipe is not initialized here. We are intializing it in
        // XWikiRadeoxRenderer so that it's initialized only once in XWiki's lifetime.
    }

    /**
     * Render an input with text markup and return a String with e.g. HTML
     * 
     * @param content String with the input to render
     * @param context Special context for the filter engine, e.g. with configuration information
     * @return result Output with rendered content
     */
    @Override
    public String render(String content, RenderContext context)
    {
        FilterContext filterContext = new BaseFilterContext();
        filterContext.setRenderContext(context);
        return this.fp.filter(content, filterContext);
    }

    public String noaccents(String name)
    {
        return StringUtils.replace(Util.noaccents(name), " ", "");
    }

    /**
     * @param name the name of a wiki page
     * @return true if the page exists or false otherwise
     * @see org.radeox.api.engine.WikiRenderEngine#exists(String)
     */
    @Override
    public boolean exists(String name)
    {
        String database = getXWikiContext().getDatabase();
        try {
            int colonIndex = name.indexOf(":");
            if (colonIndex != -1) {
                String db = name.substring(0, colonIndex);
                name = name.substring(colonIndex + 1);
                getXWikiContext().setDatabase(db);
            }

            name = StringUtils.substringBefore(StringUtils.substringBefore(name, "?"), "#");

            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(name);

            // If the document exists with the initial name, then we use this one
            if (getXWikiContext().getWiki().exists(doc.getFullName(), getXWikiContext())) {
                return true;
            }

            // If the document does not exists then we check the one converted (no accents and no spaces)
            doc.setFullName(noaccents(name));
            return getXWikiContext().getWiki().exists(doc.getFullName(), getXWikiContext());
        } catch (Exception e) {
            LOGGER.error("Failed to check if a document exists", e);

            return false;
        } finally {
            // Reset the current wiki to the original one
            getXWikiContext().setDatabase(database);
        }

    }

    @Override
    public boolean showCreate()
    {
        return true;
    }

    /**
     * Appends for example the &lt;a href&gt; HTML code for linking to a wiki page with the given name to the passed
     * buffer.
     * 
     * @param buffer the string to append to
     * @param name the name of the wiki page pointed to by the link
     * @param view the text that will be shown to the user for the link
     * @param anchor the anchor specified in the link if any (can be null)
     * @see org.radeox.api.engine.WikiRenderEngine#appendLink(StringBuffer, String, String, String)
     */
    @Override
    public void appendLink(StringBuffer buffer, String name, String view, String anchor)
    {
        if (name.length() == 0 && anchor != null) {
            appendInternalLink(buffer, view, anchor);
        } else {
            String database = getXWikiContext().getDatabase();
            XWikiContext context = getXWikiContext();

            try {
                String db = null;
                int colonIndex = name.indexOf(":");
                if (colonIndex != -1) {
                    db = name.substring(0, colonIndex);
                    name = name.substring(colonIndex + 1);
                    context.setDatabase(db);
                }

                String querystring = null;
                int qsIndex = name.indexOf("?");
                if (qsIndex != -1) {
                    querystring = name.substring(qsIndex + 1);
                    name = name.substring(0, qsIndex);
                }

                buffer.append("<span class=\"wikilink\"><a href=\"");

                // If the document exists with the conversion of spaces and accents
                // then we use this one
                XWikiDocument newdoc = new XWikiDocument();
                newdoc.setFullName(name, context);

                // If the document does not exist, then we use the cleaned name (no accents and no spaces)
                if (!context.getWiki().exists(newdoc.getFullName(), context)) {
                    newdoc.setFullName(noaccents(name), context);
                }

                if ((db == null) || (database.equals(db))) {
                    addLinkToContext(newdoc.getFullName(), context);
                }

                URL url =
                    context.getURLFactory().createURL(newdoc.getSpace(), newdoc.getName(), "view", querystring, anchor,
                        context);
                buffer.append(context.getURLFactory().getURL(url, context));
                buffer.append("\">");
                buffer.append(cleanText(view));
                buffer.append("</a></span>");
            } finally {
                context.setDatabase(database);
            }
        }
    }

    private String cleanText(String text)
    {
        return Util.escapeText(text);
    }

    private void addLinkToContext(String docname, XWikiContext context)
    {
        // Add to backlinks in context object
        try {
            @SuppressWarnings("unchecked")
            List<String> links = (List<String>) context.get("links");
            if (links == null) {
                links = new ArrayList<String>();
                context.put("links", links);
            }
            // We restrict the number of bytes in the document name, since:
            // 1. It will throw an error on some DBMSs, as 255 is the column length
            // 2. Such a long document name is not likely to occur, since the same limit is
            // imposed on the document name length.
            if (!links.contains(docname) && docname.getBytes().length <= 255) {
                links.add(docname);
            }
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Error adding link to context", e);
            }
        }
    }

    @Override
    public void appendLink(StringBuffer buffer, String name, String view)
    {
        appendLink(buffer, name, view, null);
    }

    public void appendInternalLink(StringBuffer buffer, String text, String anchor)
    {
        buffer.append("<span class=\"wikilink\"><a href=\"#");
        buffer.append(Util.encodeURI(anchor, getXWikiContext()));
        buffer.append("\">");
        if (text.length() == 0) {
            text = anchor;
        }
        buffer.append(cleanText(text));
        buffer.append("</a></span>");
    }

    @Override
    public void appendCreateLink(StringBuffer buffer, String name, String view)
    {
        String database = getXWikiContext().getDatabase();
        XWikiContext context = getXWikiContext();

        try {
            String db = null;
            int colonIndex = name.indexOf(":");
            if (colonIndex != -1) {
                db = name.substring(0, colonIndex);
                name = name.substring(colonIndex + 1);
                context.setDatabase(db);
            }

            StringBuilder querystring = new StringBuilder();
            int qsIndex = name.indexOf("?");
            if (qsIndex != -1) {
                querystring.append(name.substring(qsIndex + 1));
                name = name.substring(0, qsIndex);
            }

            String newname = name;
            XWikiDocument newdoc = new XWikiDocument();
            newdoc.setFullName(newname, context);

            XWikiDocument currentdoc = context.getDoc();
            if (currentdoc != null) {
                querystring.append(querystring.length() == 0 ? "" : "&amp;");
                querystring.append("parent=").append(Util.encodeURI(currentdoc.getFullName(), context));
            }

            if ((db == null) || (database.equals(db))) {
                // Backlinks computation
                addLinkToContext(newdoc.getFullName(), context);
            }

            URL url =
                context.getURLFactory().createURL(newdoc.getSpace(), newdoc.getName(), "edit", querystring.toString(),
                    null, context);
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
     * Get a link to an image. This can be used by filters or macros to get images for e.g. external links or icons
     * Should be refactored to get other images as well
     * 
     * @return result String with an HTML link to an image
     */
    public String getExternalImageLink()
    {
        return "";
    }
}
