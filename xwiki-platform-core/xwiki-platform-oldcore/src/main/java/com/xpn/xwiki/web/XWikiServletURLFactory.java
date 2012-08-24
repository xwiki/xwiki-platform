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
package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

public class XWikiServletURLFactory extends XWikiDefaultURLFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiServletURLFactory.class);

    /**
     * This is the URL which was requested by the user possibly with the host modified if x-forwarded-host header is set
     * or if xwiki.home parameter is set and we are viewing the main page.
     */
    protected URL serverURL;

    protected String contextPath;

    public XWikiServletURLFactory()
    {
    }

    // Used by tests
    public XWikiServletURLFactory(URL serverURL, String contextPath, String actionPath)
    {
        this.serverURL = serverURL;
        this.contextPath = contextPath;
    }

    /**
     * Creates a new URL factory that uses the server URL and context path specified by the given XWiki context. This
     * constructor should be used only in tests. Make sure {@link XWikiContext#setURL(URL)} is called before this
     * constructor.
     * 
     * @param context
     */
    public XWikiServletURLFactory(XWikiContext context)
    {
        init(context);
    }

    @Override
    public void init(XWikiContext context)
    {
        this.contextPath = context.getWiki().getWebAppPath(context);
        try {
            this.serverURL = new URL(getProtocol(context) + "://" + getHost(context));
        } catch (MalformedURLException e) {
            // This can't happen.
        }
    }

    /**
     * Returns the part of the URL identifying the web application. In a normal install, that is <tt>xwiki/</tt>.
     * 
     * @return The configured context path.
     */
    public String getContextPath()
    {
        return this.contextPath;
    }

    /**
     * @param context the XWiki context used to access the request object
     * @return the value of the {@code xwiki.url.protocol} configuration parameter, if defined, otherwise the protocol
     *         used to make the request to the proxy server if we are behind one, otherwise the protocol of the URL used
     *         to make the current request
     */
    private String getProtocol(XWikiContext context)
    {
        // Tests usually set the context URL but don't set the request object.
        String protocol = context.getURL().getProtocol();
        if (context.getRequest() != null) {
            protocol = context.getRequest().getScheme();
            if ("http".equalsIgnoreCase(protocol) && context.getRequest().isSecure()) {
                // This can happen in reverse proxy mode, if the proxy server receives HTTPS requests and forwards them
                // as HTTP to the internal web server running XWiki.
                protocol = "https";
            }
        }
        // Detected protocol can be overwritten by configuration.
        return context.getWiki().Param("xwiki.url.protocol", protocol);
    }

    /**
     * @param context the XWiki context used to access the request object
     * @return the proxy host, if we are behind one, otherwise the host of the URL used to make the current request
     */
    private String getHost(XWikiContext context)
    {
        URL url = context.getURL();

        // Check reverse-proxy mode (e.g. Apache's mod_proxy_http).
        String proxyHost = StringUtils.substringBefore(context.getRequest().getHeader("x-forwarded-host"), ",");
        if (!StringUtils.isEmpty(proxyHost)) {
            return proxyHost;
        }
        // If the reverse proxy does not support the x-forwarded-host parameter
        // we allow the user to force the the host name by using the xwiki.home param.
        final URL homeParam = getXWikiHomeParameter(context);
        if (homeParam != null && context.isMainWiki()) {
            url = homeParam;
        }

        return url.getHost() + (url.getPort() < 0 ? "" : (":" + url.getPort()));
    }

    /** @return a URL made from the xwiki.cfg parameter xwiki.home or null if undefined or unparsable. */
    private static URL getXWikiHomeParameter(final XWikiContext context)
    {
        final String surl = getXWikiHomeParameterAsString(context);
        if (!StringUtils.isEmpty(surl)) {
            try {
                return new URL(surl);
            } catch (MalformedURLException e) {
                LOGGER.warn("Could not create URL from xwiki.cfg xwiki.home parameter: " + surl
                    + " Ignoring parameter.");
            }
        }
        return null;
    }

    /** @return the xwiki.home parameter or null if undefined. */
    private static String getXWikiHomeParameterAsString(final XWikiContext context)
    {
        return context.getWiki().Param("xwiki.home", null);
    }

    @Override
    public URL getServerURL(XWikiContext context) throws MalformedURLException
    {
        return getServerURL(context.getDatabase(), context);
    }

    /**
     * Get the url of the server EG: http://www.xwiki.org/ This function sometimes will return a URL with a trailing /
     * and other times not. This is because the xwiki.home param is recommended to have a trailing / but this.serverURL
     * never does.
     * 
     * @param xwikidb the name of the database (subwiki) if null it is assumed to be the same as the wiki which we are
     *            currently displaying.
     * @param context the XWikiContext used to determine the current wiki and the value if the xwiki.home parameter if
     *            needed as well as access the xwiki server document if in virtual mode.
     * @return a URL containing the protocol, host, and port (if applicable) of the server to use for the given
     *         database.
     */
    public URL getServerURL(String xwikidb, XWikiContext context) throws MalformedURLException
    {
        if (xwikidb == null || xwikidb.equals(context.getOriginalDatabase())) {
            // This is the case if we are getting a URL for a page which is in
            // the same wiki as the page which is now being displayed.
            return this.serverURL;
        }

        if (context.isMainWiki(xwikidb)) {
            // Not in the same wiki so we are in a subwiki and we want a URL which points to the main wiki.
            // if xwiki.home is set then lets return that.
            final URL homeParam = getXWikiHomeParameter(context);
            if (homeParam != null) {
                return homeParam;
            }
        }

        URL url = context.getWiki().getServerURL(xwikidb, context);
        return url == null ? this.serverURL : url;
    }

    @Override
    public URL createURL(String web, String name, String action, boolean redirect, XWikiContext context)
    {
        return createURL(web, name, action, context);
    }

    @Override
    public URL createURL(String web, String name, String action, String querystring, String anchor, String xwikidb,
        XWikiContext context)
    {
        // Action and Query String transformers
        if (("view".equals(action)) && (context.getLinksAction() != null)) {
            action = context.getLinksAction();
        }
        if (context.getLinksQueryString() != null) {
            if (querystring == null) {
                querystring = context.getLinksQueryString();
            } else {
                querystring = querystring + "&" + context.getLinksQueryString();
            }
        }

        StringBuffer newpath = new StringBuffer(this.contextPath);
        addServletPath(newpath, xwikidb, context);
        addAction(newpath, action, context);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);

        if (!StringUtils.isEmpty(querystring)) {
            newpath.append("?");
            newpath.append(StringUtils.removeEnd(StringUtils.removeEnd(querystring, "&"), "&amp;"));
            // newpath.append(querystring.replaceAll("&","&amp;"));
        }

        if (!StringUtils.isEmpty(anchor)) {
            newpath.append("#");
            newpath.append(encode(anchor, context));
        }

        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    private void addServletPath(StringBuffer newpath, String xwikidb, XWikiContext context)
    {
        if (xwikidb == null) {
            xwikidb = context.getDatabase();
        }

        String spath = context.getWiki().getServletPath(xwikidb, context);
        newpath.append(spath);
    }

    private void addAction(StringBuffer newpath, String action, XWikiContext context)
    {
        boolean showViewAction = context.getWiki().showViewAction(context);
        if ((!"view".equals(action) || (showViewAction))) {
            newpath.append(action);
            newpath.append("/");
        }
    }

    private void addSpace(StringBuffer newpath, String space, String action, XWikiContext context)
    {
        boolean skipDefaultSpace = context.getWiki().skipDefaultSpaceInURLs(context);
        if (skipDefaultSpace) {
            String defaultSpace = context.getWiki().getDefaultSpace(context);
            skipDefaultSpace = (space.equals(defaultSpace)) && ("view".equals(action));
        }
        if (!skipDefaultSpace) {
            newpath.append(encode(space, context));
            newpath.append("/");
        }
    }

    private void addName(StringBuffer newpath, String name, String action, XWikiContext context)
    {
        XWiki xwiki = context.getWiki();
        if ((xwiki.useDefaultAction(context))
            || (!name.equals(xwiki.getDefaultPage(context)) || (!"view".equals(action)))) {
            newpath.append(encode(name, context));
        }
    }

    protected void addFileName(StringBuffer newpath, String filename, XWikiContext context)
    {
        addFileName(newpath, filename, true, context);
    }

    protected void addFileName(StringBuffer newpath, String filename, boolean encode, XWikiContext context)
    {
        newpath.append("/");
        if (encode) {
            newpath.append(encode(filename, context).replace("+", "%20"));
        } else {
            newpath.append(filename);
        }
    }

    private String encode(String name, XWikiContext context)
    {
        return Util.encodeURI(name, context);
    }

    @Override
    public URL createExternalURL(String web, String name, String action, String querystring, String anchor,
        String xwikidb, XWikiContext context)
    {
        return this.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

    @Override
    public URL createSkinURL(String filename, String skin, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(this.contextPath);
        newpath.append("skins/");
        newpath.append(skin);
        addFileName(newpath, filename, false, context);
        try {
            return new URL(getServerURL(context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    @Override
    public URL createSkinURL(String filename, String web, String name, String xwikidb, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(this.contextPath);
        addServletPath(newpath, xwikidb, context);
        addAction(newpath, "skin", context);
        addSpace(newpath, web, "skin", context);
        addName(newpath, name, "skin", context);
        addFileName(newpath, filename, false, context);
        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    @Override
    public URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(this.contextPath);
        if (forceSkinAction) {
            addServletPath(newpath, context.getDatabase(), context);
            addAction(newpath, "skin", context);
        }
        newpath.append("resources");
        addFileName(newpath, filename, false, context);
        try {
            return new URL(getServerURL(context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    public URL createTemplateURL(String filename, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(this.contextPath);
        newpath.append("templates");
        addFileName(newpath, filename, false, context);
        try {
            return new URL(getServerURL(context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    @Override
    public URL createAttachmentURL(String filename, String web, String name, String action, String querystring,
        String xwikidb, XWikiContext context)
    {
        if ((context != null) && "viewrev".equals(context.getAction()) && context.get("rev") != null
            && isContextDoc(xwikidb, web, name, context)) {
            try {
                String docRevision = context.get("rev").toString();
                XWikiAttachment attachment =
                    findAttachmentForDocRevision(context.getDoc(), docRevision, filename, context);
                if (attachment == null) {
                    action = "viewattachrev";
                } else {
                    long arbId = findDeletedAttachmentForDocRevision(context.getDoc(), docRevision, filename, context);
                    return createAttachmentRevisionURL(filename, web, name, attachment.getVersion(), arbId,
                        querystring, xwikidb, context);
                }
            } catch (XWikiException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Exception while trying to get attachment version !", e);
                }
            }
        }

        StringBuffer newpath = new StringBuffer(this.contextPath);
        addServletPath(newpath, xwikidb, context);
        addAction(newpath, action, context);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);
        addFileName(newpath, filename, context);

        if (!StringUtils.isEmpty(querystring)) {
            newpath.append("?");
            newpath.append(StringUtils.removeEnd(StringUtils.removeEnd(querystring, "&"), "&amp;"));
        }

        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a document is the original context document. This is needed when generating attachment revision URLs,
     * since only attachments of the context document should also be versioned.
     * 
     * @param wiki the wiki name of the document to check
     * @param space the space name of the document to check
     * @param name the document name of the document to check
     * @param context the current request context
     * @return {@code true} if the provided document is the same as the current context document, {@code false}
     *         otherwise
     */
    protected boolean isContextDoc(String wiki, String space, String name, XWikiContext context)
    {
        if (context == null || context.getDoc() == null) {
            return false;
        }
        XWikiDocument doc = context.getDoc();
        return doc.getDocumentReference().getLastSpaceReference().getName().equals(space)
            && doc.getDocumentReference().getName().equals(name)
            && (wiki == null || doc.getDocumentReference().getWikiReference().getName().equals(wiki));
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String web, String name, String revision,
        String querystring, String xwikidb, XWikiContext context)
    {
        return createAttachmentRevisionURL(filename, web, name, revision, -1, querystring, xwikidb, context);
    }

    public URL createAttachmentRevisionURL(String filename, String web, String name, String revision, long recycleId,
        String querystring, String xwikidb, XWikiContext context)
    {
        String action = "downloadrev";
        StringBuffer newpath = new StringBuffer(this.contextPath);
        addServletPath(newpath, xwikidb, context);
        addAction(newpath, action, context);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);
        addFileName(newpath, filename, context);

        String qstring = "rev=" + revision;
        if (recycleId >= 0) {
            qstring += "&rid=" + recycleId;
        }
        if (!StringUtils.isEmpty(querystring)) {
            qstring += "&" + querystring;
        }
        newpath.append("?");
        newpath.append(StringUtils.removeEnd(StringUtils.removeEnd(qstring, "&"), "&amp;"));

        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    /**
     * Converts a URL to a relative URL if it's a XWiki URL (keeping only the path + query string + anchor) and leave
     * the URL unchanged if it's an external URL.
     * <p>
     * An URL is considered to be external if its server component doesn't match the server of the current request URL.
     * This means that URLs are made relative with respect to the current request URL rather than the current wiki set
     * on the XWiki context. Let's take an example:
     * 
     * <pre>
     * {@code
     * request URL: http://playground.xwiki.org/xwiki/bin/view/Sandbox/TestURL
     * current wiki: code (code.xwiki.org)
     * URL (1): http://code.xwiki.org/xwiki/bin/view/Main/WebHome
     * URL (2): http://playground.xwiki.org/xwiki/bin/view/Spage/Page
     * 
     * The result will be:
     * (1) http://code.xwiki.org/xwiki/bin/view/Main/WebHome
     * (2) /xwiki/bin/view/Spage/Page
     * }
     * </pre>
     * 
     * @param url the URL to convert
     * @return the converted URL as a string
     * @see com.xpn.xwiki.web.XWikiDefaultURLFactory#getURL(java.net.URL, com.xpn.xwiki.XWikiContext)
     */
    @Override
    public String getURL(URL url, XWikiContext context)
    {
        String relativeURL = "";

        try {
            if (url != null) {
                String surl = url.toString();

                if (!surl.startsWith(serverURL.toString())) {
                    // External URL: leave it as is.
                    relativeURL = surl;
                } else {
                    // Internal XWiki URL: convert to relative.
                    StringBuffer sbuf = new StringBuffer(url.getPath());
                    String querystring = url.getQuery();
                    if (!StringUtils.isEmpty(querystring)) {
                        sbuf.append("?");
                        sbuf.append(StringUtils.removeEnd(StringUtils.removeEnd(querystring, "&"), "&amp;"));
                        // sbuf.append(querystring.replaceAll("&","&amp;"));
                    }

                    String anchor = url.getRef();
                    if (!StringUtils.isEmpty(anchor)) {
                        sbuf.append("#");
                        sbuf.append(anchor);
                    }

                    relativeURL = Util.escapeURL(sbuf.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create URL", e);
        }

        return relativeURL;
    }

    @Override
    public URL getRequestURL(XWikiContext context)
    {
        final URL url = super.getRequestURL(context);

        try {
            final URL servurl = getServerURL(context);
            // if use apache mod_proxy we needed to know external host address
            return new URL(servurl.getProtocol(), servurl.getHost(), servurl.getPort(), url.getFile());
        } catch (MalformedURLException e) {
            // This should not happen
            LOGGER.error("Failed to create request URL", e);
        }

        return url;
    }

    public XWikiAttachment findAttachmentForDocRevision(XWikiDocument doc, String docRevision, String filename,
        XWikiContext context) throws XWikiException
    {
        XWikiAttachment attachment = null;
        XWikiDocument rdoc = context.getWiki().getDocument(doc, docRevision, context);
        if (filename != null) {
            attachment = rdoc.getAttachment(filename);
        }

        return attachment;
    }

    public long findDeletedAttachmentForDocRevision(XWikiDocument doc, String docRevision, String filename,
        XWikiContext context) throws XWikiException
    {
        XWikiAttachment attachment = null;
        XWikiDocument rdoc = context.getWiki().getDocument(doc, docRevision, context);
        if (context.getWiki().hasAttachmentRecycleBin(context) && filename != null) {
            attachment = rdoc.getAttachment(filename);
            if (attachment != null) {
                List<DeletedAttachment> deleted =
                    context.getWiki().getAttachmentRecycleBinStore()
                        .getAllDeletedAttachments(attachment, context, true);
                Collections.reverse(deleted);
                for (DeletedAttachment entry : deleted) {
                    if (entry.getDate().after(rdoc.getDate())) {
                        return entry.getId();
                    }
                }
            }
        }

        return -1;
    }
}
