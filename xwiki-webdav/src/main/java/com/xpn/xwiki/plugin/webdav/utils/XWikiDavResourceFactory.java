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
package com.xpn.xwiki.plugin.webdav.utils;

import javax.servlet.ServletContext;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.views.RootView;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavUtils.ResourceHint;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.xmlrpc.XWikiXmlRpcResponse;

/**
 * Responsible for forming WebDAV resources corresponding to a given WebDAV request.
 * 
 * @version $Id$
 */
public class XWikiDavResourceFactory implements DavResourceFactory
{
    /**
     * {@link XWikiContext}.
     */
    private XWikiContext xwikiContext;

    /**
     * The lock manager.
     */
    private final LockManager lockManager;

    /**
     * The servlet context.
     */
    private ServletContext servletContext;

    /**
     * Create a new {@link XWikiDavResourceFactory} that uses the given lock manager and the default
     * {@link XWikiResourceConfig} configuration.
     * 
     * @param lockManager The {@link LockManager} to be used.
     */
    public XWikiDavResourceFactory(LockManager lockManager)
    {
        this.lockManager = lockManager;
    }

    /**
     * {@inheritDoc}
     */
    public DavResource createResource(DavResourceLocator locator, DavServletRequest request,
        DavServletResponse response) throws DavException
    {
        return createResource(locator, request.getDavSession(), request, response);
    }

    /**
     * {@inheritDoc}
     */
    public DavResource createResource(DavResourceLocator locator, DavSession session)
        throws DavException
    {
        return createResource(locator, session, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public XWikiDavResource createResource(DavResourceLocator locator, DavSession session,
        DavServletRequest request, DavServletResponse response) throws DavException
    {
        DavResourceLocator rootLocator =
            locator.getFactory().createResourceLocator(locator.getPrefix(),
                XWikiDavResource.BASE_URI, XWikiDavResource.BASE_URI);
        try {
            initXWikiContext(request, response, servletContext);
        } catch (XWikiException e) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
        RootView rootView =
            (RootView) Utils.getComponent(XWikiDavResource.ROLE, ResourceHint.ROOT);
        rootView.init("webdav", rootLocator, this, session, lockManager, xwikiContext);
        return rootView.decode(locator);
    }

    /**
     * Sets the {@link ServletContext} for this factory.
     * 
     * @param servletContext The servlet context.
     */
    public void setServletContext(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }

    /**
     * Initializes the {@link XWikiContext}.
     * 
     * @param drequest {@link DavServletRequest}.
     * @param dresponse {@link DavServletResponse}.
     * @param servletContext {@link ServletContext}.
     * @throws XWikiException If context initialization fails.
     */
    private void initXWikiContext(DavServletRequest drequest, DavServletResponse dresponse,
        ServletContext servletContext) throws XWikiException
    {
        XWikiEngineContext xwikiEngine = new XWikiServletContext(servletContext);
        XWikiRequest xwikiRequest = new XWikiServletRequest(drequest);
        XWikiResponse xwikiResponse = new XWikiXmlRpcResponse(dresponse);

        xwikiContext = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiEngine);
        xwikiContext.setMode(XWikiContext.MODE_GWT);
        xwikiContext.setDatabase("xwiki");

        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.ROLE);
        try {
            containerInitializer.initializeRequest(xwikiContext.getRequest()
                .getHttpServletRequest(), xwikiContext);
            containerInitializer.initializeResponse(xwikiContext.getResponse()
                .getHttpServletResponse());
            containerInitializer.initializeSession(xwikiContext.getRequest()
                .getHttpServletRequest());
            containerInitializer.initializeApplicationContext(servletContext);
        } catch (ServletContainerException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_INIT_FAILED,
                "Failed to initialize Request/Response or Session",
                e);
        }

        XWiki xwiki = XWiki.getXWiki(xwikiContext);
        XWikiURLFactory urlf =
            xwiki.getURLFactoryService().createURLFactory(xwikiContext.getMode(), xwikiContext);
        xwikiContext.setURLFactory(urlf);
        xwiki.prepareResources(xwikiContext);

        String username = "XWiki.XWikiGuest";
        XWikiUser user = xwikiContext.getWiki().checkAuth(xwikiContext);
        if (user != null) {
            username = user.getUser();
        }
        xwikiContext.setUser(username);

        if (xwikiContext.getDoc() == null) {
            xwikiContext.setDoc(new XWikiDocument("Fake", "Document"));
        }
        xwikiContext.put("ajax", Boolean.TRUE);
    }
}
