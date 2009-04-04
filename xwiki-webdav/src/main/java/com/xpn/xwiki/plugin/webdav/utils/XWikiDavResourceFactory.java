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
import javax.servlet.ServletException;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.views.RootView;
import com.xpn.xwiki.web.Utils;

/**
 * Responsible for forming WebDAV resources corresponding to a given WebDAV request.
 * 
 * @version $Id$
 */
public class XWikiDavResourceFactory implements DavResourceFactory
{
    /**
     * The servlet context.
     */
    private ServletContext servletContext;

    /**
     * The lock manager.
     */
    private final LockManager lockManager;

    /**
     * Create a new {@link XWikiDavResourceFactory}.
     */
    public XWikiDavResourceFactory(ServletContext servletContext) throws ServletException
    {
        this.lockManager = new SimpleLockManager();
        this.servletContext = servletContext;       
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
        XWikiDavContext context =
            new XWikiDavContext(request, response, servletContext, this, session, lockManager);
        RootView rootView =
            (RootView) Utils.getComponent(XWikiDavResource.class, "root");
        rootView.init("webdav", rootLocator, context);
        return rootView.decode(locator);
    }
}
