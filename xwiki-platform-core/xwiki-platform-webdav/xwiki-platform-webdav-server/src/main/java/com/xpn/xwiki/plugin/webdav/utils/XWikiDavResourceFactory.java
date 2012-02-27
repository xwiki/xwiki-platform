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

    @Override
    public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response)
        throws DavException
    {
        return createResource(locator, request.getDavSession(), request, response);
    }

    @Override
    public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException
    {
        return createResource(locator, session, null, null);
    }

    public XWikiDavResource createResource(DavResourceLocator locator, DavSession session, DavServletRequest request,
        DavServletResponse response) throws DavException
    {
        String baseURI = XWikiDavResource.BASE_URI;
        DavResourceLocator rootLocator =
            locator.getFactory().createResourceLocator(locator.getPrefix(), baseURI, baseURI);
        XWikiDavContext context = new XWikiDavContext(request, response, servletContext, this, session, lockManager);
        XWikiDavResource root = new RootView();
        root.init("webdav", rootLocator, context);
        String workspacePath = locator.getWorkspacePath();
        String[] tokens = locator.getResourcePath().split("/");
        if (workspacePath != null && workspacePath.equals(baseURI) && (tokens.length >= 2)) {
            return (tokens.length == 2) ? root : root.decode(tokens, 2);
        } else {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
    }
}
