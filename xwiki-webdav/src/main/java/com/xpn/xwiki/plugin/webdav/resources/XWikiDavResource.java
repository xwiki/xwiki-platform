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
