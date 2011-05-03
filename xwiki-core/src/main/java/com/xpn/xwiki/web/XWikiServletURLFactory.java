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
package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

public class XWikiServletURLFactory extends XWikiDefaultURLFactory
{
    private static final Log LOG = LogFactory.getLog(XWikiServletURLFactory.class);

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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#init(com.xpn.xwiki.XWikiContext)
     */
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
        // Tests usually set the context URL but don't set the request object.
        if (context.getRequest() != null) {
            // Check reverse-proxy mode (e.g. Apache's mod_proxy_http).
            String proxyHost = StringUtils.substringBefore(context.getRequest().getHeader("x-forwarded-host"), ",");
            if (!StringUtils.isEmpty(proxyHost)) {
                return proxyHost;
            }
        }
        URL url = context.getURL();
        return url.getHost() + (url.getPort() < 0 ? "" : (":" + url.getPort()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#getServerURL(com.xpn.xwiki.XWikiContext)
     */
    public URL getServerURL(XWikiContext context) throws MalformedURLException
    {
        return getServerURL(context.getDatabase(), context);
    }

    public URL getServerURL(String xwikidb, XWikiContext context) throws MalformedURLException
    {
        if (xwikidb == null || xwikidb.equals(context.getOriginalDatabase())) {
            return this.serverURL;
        }

        if (context.isMainWiki(xwikidb)) {
            String surl = context.getWiki().Param("xwiki.home", null);
            if (!StringUtils.isEmpty(surl)) {
                return new URL(surl);
            }
        }

        URL url = context.getWiki().getServerURL(xwikidb, context);
        return url == null ? this.serverURL : url;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createURL(java.lang.String, java.lang.String, java.lang.String, boolean,
     *      com.xpn.xwiki.XWikiContext)
     */
    public URL createURL(String web, String name, String action, boolean redirect, XWikiContext context)
    {
        return createURL(web, name, action, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createURL(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
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
            newpath.append(StringUtils.chomp(StringUtils.chomp(querystring, "&"), "&amp;"));
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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createExternalURL(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public URL createExternalURL(String web, String name, String action, String querystring, String anchor,
        String xwikidb, XWikiContext context)
    {
        return this.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createSkinURL(java.lang.String, java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createSkinURL(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, com.xpn.xwiki.XWikiContext)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createAttachmentURL(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
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
                if (LOG.isErrorEnabled()) {
                    LOG.error("Exception while trying to get attachment version !", e);
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
            newpath.append(StringUtils.chomp(StringUtils.chomp(querystring, "&"), "&amp;"));
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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiURLFactory#createAttachmentRevisionURL(java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
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
        newpath.append(StringUtils.chomp(StringUtils.chomp(qstring, "&"), "&amp;"));

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
        try {
            if (url == null) {
                return "";
            }

            String surl = url.toString();
            if (!surl.startsWith(serverURL.toString())) {
                // External URL: leave it as is.
                return surl;
            } else {
                // Internal XWiki URL: convert to relative.
                StringBuffer sbuf = new StringBuffer(url.getPath());
                String querystring = url.getQuery();
                if (!StringUtils.isEmpty(querystring)) {
                    sbuf.append("?");
                    sbuf.append(StringUtils.chomp(StringUtils.chomp(querystring, "&"), "&amp;"));
                    // sbuf.append(querystring.replaceAll("&","&amp;"));
                }

                String anchor = url.getRef();
                if (!StringUtils.isEmpty(anchor)) {
                    sbuf.append("#");
                    sbuf.append(anchor);
                }
                return Util.escapeURL(sbuf.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.web.XWikiDefaultURLFactory#getRequestURL(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public URL getRequestURL(XWikiContext context)
    {
        final URL url = super.getRequestURL(context);
        try {
            final URL servurl = getServerURL(context);
            // if use apache mod_proxy we needed to know external host address
            return new URL(servurl.getProtocol(), servurl.getHost(), servurl.getPort(), url.getFile());
        } catch (MalformedURLException ex) {
            // This should not happen
            ex.printStackTrace();
            return url;
        }
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
