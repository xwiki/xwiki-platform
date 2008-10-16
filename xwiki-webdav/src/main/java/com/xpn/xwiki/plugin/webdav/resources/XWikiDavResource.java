package com.xpn.xwiki.plugin.webdav.resources;

import java.util.List;
import java.util.Stack;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;

import com.xpn.xwiki.XWikiContext;

/**
 * The super interface for all xwiki related dav resources. Adds extra xwiki specific methods for
 * the basic {@link DavResource}.
 * 
 * @version $Id$
 */
public interface XWikiDavResource extends DavResource
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = XWikiDavResource.class.getName(); 
    
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
    void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException;

    /**
     * Initializes this resource with the given attributes.
     * 
     * @param name Name of this resource.
     * @param locator Resource Locator.
     * @param factory Resource Factory.
     * @param session Dav Session.
     * @param lockManager Lock Manager.
     * @param xwikiContext XWiki Context.
     * @throws DavException If the initialization fails.
     */
    void init(String name, DavResourceLocator locator, DavResourceFactory factory,
        DavSession session, LockManager lockManager, XWikiContext xwikiContext)
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
     * @return The set of virtual resources (some clients need such resources).
     */
    List<XWikiDavResource> getSessionResources();

    /**
     * @return The {@link XWikiContext} associated with this resource.
     */
    XWikiContext getXwikiContext();
    
    /**
     * @return The {@link LockManager} associated with this request (global).
     */
    LockManager getLockManager();
}
