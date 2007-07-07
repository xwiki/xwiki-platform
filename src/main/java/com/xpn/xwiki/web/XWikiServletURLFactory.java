/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class XWikiServletURLFactory extends XWikiDefaultURLFactory
{
    protected URL serverURL;

    protected String servletPath;

    protected String actionPath;

    public XWikiServletURLFactory()
    {
    }

    // Used by tests
    public XWikiServletURLFactory(URL serverURL, String servletPath, String actionPath)
    {
        this.serverURL = serverURL;
        this.servletPath = servletPath;
        this.actionPath = actionPath;
    }

    // Used by tests
    public XWikiServletURLFactory(XWikiContext context)
    {
        init(context);
    }

    public void init(XWikiContext context)
    {
        URL url = context.getURL();
        String path = url.getPath();
        String servletpath = context.getRequest().getServletPath();

        servletPath =
            (context.getWiki() == null) ? "" : context.getWiki().Param("xwiki.servletpath", "");
        if (servletPath.equals("")) {
            try {
                servletPath =
                    ((XWikiServletContext) context.getEngineContext()).getServletContext()
                        .getServletContextName()
                        + "/";
            } catch (Exception e) {
                servletPath = path.substring(0, path.indexOf('/', 1) + 1);
            }
        }

        actionPath = context.getWiki().Param("xwiki.actionpath", "");
        if (actionPath.equals("")) {
            if (servletpath.startsWith("/bin")) {
                actionPath = "bin/";
            } else if (context.getRequest().getServletPath().startsWith("/testbin")) {
                actionPath = "testbin/";
            } else {
                actionPath = context.getWiki().Param("xwiki.defaultactionpath", "xwiki/");
            }
        }

        try {
            serverURL = new URL(url, "/");
        } catch (MalformedURLException e) {
            // This can't happen
        }
    }

    public String getServletPath()
    {
        return servletPath;
    }

    private URL getServerURL(XWikiContext context) throws MalformedURLException
    {
        return getServerURL(context.getDatabase(), context);
    }

    private URL getServerURL(String xwikidb, XWikiContext context) throws MalformedURLException
    {
        URL serverURL = this.serverURL;
        if (context.getRequest() != null) { // necessary to the tests
            final String host = context.getRequest().getHeader("x-forwarded-host"); // apache
            // modproxy host
            if (host != null) {
                int comaind = host.indexOf(',');
                final String host1 = comaind > 0 ? host.substring(0, comaind) : host;
                if (!host1.equals(""))
                    serverURL = new URL("http://" + host1);
            }
        }
        if (xwikidb == null)
            return serverURL;

        if (xwikidb.equals(context.getOriginalDatabase())) {
            return serverURL;
        }

        if (xwikidb.equals("xwiki")) {
            String surl = context.getWiki().Param("xwiki.home", "");
            if (!surl.equals(""))
                return new URL(surl);
        }

        URL url = context.getWiki().getServerURL(xwikidb, context);
        if (url == null) {
            return serverURL;
        } else {
            return url;
        }
    }

    public URL createURL(String web, String name, String action, boolean redirect,
        XWikiContext context)
    {
        return createURL(web, name, action, context);
    }

    public URL createURL(String web, String name, String action, String querystring,
        String anchor, String xwikidb, XWikiContext context)
    {
        // Action and Query String transformers
        if (("view".equals(action)) && (context.getLinksAction() != null)) {
            action = context.getLinksAction();
        }
        if (context.getLinksQueryString() != null) {
            if (querystring == null)
                querystring = context.getLinksQueryString();
            else
                querystring = querystring + "&" + context.getLinksQueryString();
        }

        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);

        addAction(newpath, action, context);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);

        if ((querystring != null) && (!querystring.equals(""))) {
            newpath.append("?");
            newpath.append(querystring);
            // newpath.append(querystring.replaceAll("&","&amp;"));
        }

        if ((anchor != null) && (!anchor.equals(""))) {
            newpath.append("#");
            newpath.append(anchor);
        }

        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    private void addAction(StringBuffer newpath, String action, XWikiContext context)
    {
        boolean showViewAction = context.getWiki().showViewAction(context);
        if ((!"view".equals(action) || (showViewAction))) {
            newpath.append(action);
            newpath.append("/");
        }
    }

    private void addSpace(StringBuffer newpath, String web, String action, XWikiContext context)
    {
        boolean useDefaultWeb = context.getWiki().useDefaultWeb(context);
        if (useDefaultWeb) {
            String defaultWeb = context.getWiki().getDefaultWeb(context);
            useDefaultWeb = (web.equals(defaultWeb)) && ("view".equals(action));
        }
        if (!useDefaultWeb) {
            newpath.append(encode(web, context));
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

    private void addFileName(StringBuffer newpath, String filename, XWikiContext context)
    {
        addFileName(newpath, filename, true, context);
    }

    private void addFileName(StringBuffer newpath, String filename, boolean encode,
        XWikiContext context)
    {
        newpath.append("/");
        if (encode)
            newpath.append(encode(filename, context));
        else
            newpath.append(filename);
    }

    private String encode(String name, XWikiContext context)
    {
        return Utils.encode(name, context);
    }

    public URL createExternalURL(String web, String name, String action, String querystring,
        String anchor, String xwikidb, XWikiContext context)
    {
        return this.createURL(web, name, action, querystring, anchor, xwikidb, context);
    }

    public URL createSkinURL(String filename, String skin, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(servletPath);
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

    public URL createSkinURL(String filename, String web, String name, String xwikidb,
        XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);
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

    public URL createTemplateURL(String filename, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append("templates");
        addFileName(newpath, filename, false, context);
        try {
            return new URL(getServerURL(context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    public URL createAttachmentURL(String filename, String web, String name, String action,
        String querystring, String xwikidb, XWikiContext context)
    {
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);
        addAction(newpath, action, context);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);
        addFileName(newpath, filename, context);
        XWikiAttachment attachment = null;
        XWikiAttachment attLastVer = null;
        String revdoc = null;
        String lastver = null;

        if ((querystring != null) && (!querystring.equals(""))) {
            newpath.append("?");
            newpath.append(querystring);
        }

        if ((context != null) && "viewrev".equals(context.getAction())) {
            revdoc = context.get("rev").toString();
            Log log = LogFactory.getLog(XWikiServletURLFactory.class);
            try {
                lastver =
                    context.getWiki().getDocument(context.getDoc().getFullName(), context)
                        .getVersion();
                attLastVer =
                    context.getWiki().getDocument(context.getDoc().getFullName(), context)
                        .getAttachment(filename);
                attachment =
                    findAttachmentForDocRevision(context.getDoc(), revdoc, filename, context);
            } catch (XWikiException e) {
                if (log.isErrorEnabled())
                    log.error("Exception while trying to get attachment version !", e);
            }
        }

        try {
            if (attLastVer != null) {
                if (revdoc != null && !revdoc.equals(lastver)) {
                    String veratt = attachment.getVersion();
                    String lastveratt = attLastVer.getVersion();
                    if (!veratt.equals(lastveratt)) {
                        return createAttachmentRevisionURL(filename, web, name, veratt,
                            querystring, xwikidb, context);
                    }
                }
            }

            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public URL createAttachmentRevisionURL(String filename, String web, String name,
        String revision, String querystring, String xwikidb, XWikiContext context)
    {
        String action = "downloadrev";
        StringBuffer newpath = new StringBuffer(servletPath);
        newpath.append(actionPath);
        addAction(newpath, action, context);
        addSpace(newpath, web, action, context);
        addName(newpath, name, action, context);
        addFileName(newpath, filename, context);

        String qstring = "rev=" + revision;
        if ((querystring != null) && (!querystring.equals("")))
            qstring += "&" + querystring;
        if ((qstring != null) && (!qstring.equals(""))) {
            newpath.append("?");
            newpath.append(qstring);
        }

        try {
            return new URL(getServerURL(xwikidb, context), newpath.toString());
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    public String getURL(URL url, XWikiContext context)
    {
        try {
            if (url == null)
                return "";

            String surl = url.toString();
            if (!surl.startsWith(serverURL.toString()))
                return surl;
            else {
                StringBuffer sbuf = new StringBuffer(url.getPath());
                String querystring = url.getQuery();
                if ((querystring != null) && (!querystring.equals(""))) {
                    sbuf.append("?");
                    sbuf.append(querystring);
                    // sbuf.append(querystring.replaceAll("&","&amp;"));
                }

                String anchor = url.getRef();
                if ((anchor != null) && (!anchor.equals(""))) {
                    sbuf.append("#");
                    sbuf.append(anchor);
                }
                return sbuf.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public URL getRequestURL(XWikiContext context)
    {
        final URL url = super.getRequestURL(context);
        try {
            final URL servurl = getServerURL(context);
            // if use apache mod_proxy we needed to know external host address
            return new URL(url.getProtocol(), servurl.getHost(), servurl.getPort(), url.getFile());
        } catch (MalformedURLException ex) {
            // This should not happen
            ex.printStackTrace();
            return url;
        }
    }

    public XWikiAttachment findAttachmentForDocRevision(XWikiDocument doc, String revdoc,
        String filename, XWikiContext context) throws XWikiException
    {
        XWikiAttachment attachment = null;
        XWikiDocument rdoc = context.getWiki().getDocument(doc, revdoc, context);
        if (filename != null) {
            attachment = rdoc.getAttachment(filename);
        } else {
            return null;
        }

        return attachment;
    }
}
