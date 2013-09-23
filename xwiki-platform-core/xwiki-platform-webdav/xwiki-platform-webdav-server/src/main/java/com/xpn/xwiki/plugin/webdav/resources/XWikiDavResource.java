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
package com.xpn.xwiki.plugin.webdav.resources;

import java.util.List;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import com.xpn.xwiki.plugin.webdav.utils.XWikiDavContext;

/**
 * The super interface for all xwiki related dav resources. Adds extra xwiki specific methods for the basic
 * {@link DavResource}.
 * 
 * @version $Id$
 */
public interface XWikiDavResource extends DavResource
{
    /**
     * This URI segment is used as the base workspace name.
     */
    public static final String BASE_URI = "/webdav";

    /**
     * Initializes this resource with common attributes inherited from the parent.
     * 
     * @param parent Parent resource.
     * @param name Name of this resource.
     * @param relativePath Path relative to the parent resource.
     * @throws DavException If the initialization fails.
     */
    void init(XWikiDavResource parent, String name, String relativePath) throws DavException;

    /**
     * Initializes this resource with the given parameters.
     * 
     * @param name Name of this resource.
     * @param locator Dav resource locator.
     * @param context XWiki dav context.
     * @throws DavException If the initialization fails.
     */
    void init(String name, DavResourceLocator locator, XWikiDavContext context) throws DavException;

    /**
     * Method responsible for recursively decoding a URL which has been split into segments ("/"). The 'next' variable
     * points to that URL segment (within tokens array) representing the next resource in chain.
     * 
     * @param tokens segmented URL.
     * @param next next index to be processed by this resource.
     * @return the {@link XWikiDavResource} corresponding to the given URL.
     * @throws DavException If it's not possible to decode the URL.
     */
    XWikiDavResource decode(String[] tokens, int next) throws DavException;

    /**
     * @return List of virtual members for this resource.
     */
    List<XWikiDavResource> getVirtualMembers();

    /**
     * @return Set of virtual properties for this resource.
     */
    DavPropertySet getVirtualProperties();

    /**
     * @return List of members that is added to the initial virtual member list.
     */
    List<XWikiDavResource> getInitMembers();

    /**
     * Removes everything belonging to this resource from the cache.
     */
    void clearCache();

    /**
     * @return The {@link XWikiDavContext} for this resource.
     */
    XWikiDavContext getContext();
}
