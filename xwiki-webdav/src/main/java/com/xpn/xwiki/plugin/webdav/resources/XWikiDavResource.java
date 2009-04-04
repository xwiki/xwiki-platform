package com.xpn.xwiki.plugin.webdav.resources;

import java.util.List;
import java.util.Stack;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import com.xpn.xwiki.plugin.webdav.utils.XWikiDavContext;

/**
 * The super interface for all xwiki related dav resources. Adds extra xwiki specific methods for
 * the basic {@link DavResource}.
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
    void init(String name, DavResourceLocator locator, XWikiDavContext context)
        throws DavException;

    /**
     * This method is responsible for recursively decoding a url which has been split into segments
     * ("/"). The 'next' integer represents the segment which is to be processed by this resource.
     * It will decode the next segment, create a resource (depending on the value of the segment),
     * put it into the stack and recursively call decode() on that newly created resource. At the
     * end, top of the stack will contain the resource which corresponds to the last segment of the
     * url, which is the resource representing the whole url.
     * 
     * @param stack The stack to put the decoded child resource.
     * @param tokens Segmented url urlString.split("/").
     * @param next The next index to be processed by this resource.
     * @throws DavException If it's not possible to decode the url.
     */
    void decode(Stack<XWikiDavResource> stack, String[] tokens, int next) throws DavException;

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
