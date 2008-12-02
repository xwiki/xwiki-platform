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
package com.xpn.xwiki.plugin.webdav;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.WebdavResponseImpl;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.LocatorFactoryImplEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavSessionProvider;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavResourceFactory;
import com.xpn.xwiki.web.Utils;

/**
 * The servlet responsible for handling WebDAV requests on /xwiki.
 * 
 * @version $Id$
 */
public class DavServlet extends AbstractWebdavServlet
{
    /**
     * Class version.
     */
    private static final long serialVersionUID = 7255582612577585483L;

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DavServlet.class);

    /**
     * Hard coded authenticate header (for the moment).
     */
    private static final String AUTHENTICATE_HEADER_VALUE = "Basic realm=\"XWiki Webdav Server\"";

    /**
     * WWW-Authenticate header string.
     */
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    /**
     * Locator factory. {@link DavLocatorFactory}
     */
    private DavLocatorFactory locatorFactory;
    
    /**
     * Resource factory. {@link DavResourceFactory}
     */
    private DavResourceFactory resourceFactory;

    /**
     * Session provider. {@link DavSessionProvider}
     */
    private DavSessionProvider davSessionProvider;

    /**
     * Map used to remember any webdav lock created without being reflected in the underlying
     * repository. This is needed because some clients rely on a successful locking mechanism in
     * order to perform properly (e.g. mac OSX built-in dav client).
     */
    private LockManager lockManager;

    /**
     * {@inheritDoc}
     */
    protected boolean isPreconditionValid(WebdavRequest request, DavResource resource)
    {
        return !resource.exists() || request.matchesIfHeader(resource);
    }

    /**
     * {@inheritDoc}
     */
    public DavLocatorFactory getLocatorFactory()
    {
        if (locatorFactory == null) {
            locatorFactory = new LocatorFactoryImplEx("");
        }
        return locatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void setLocatorFactory(DavLocatorFactory locatorFactory)
    {
        this.locatorFactory = locatorFactory;
    }

    /**
     * {@inheritDoc}
     */
    public DavResourceFactory getResourceFactory()
    {
        if (resourceFactory == null) {
            resourceFactory = new XWikiDavResourceFactory(getLockManager());
            ((XWikiDavResourceFactory) resourceFactory).setServletContext(getServletContext());
        }
        return resourceFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void setResourceFactory(DavResourceFactory resourceFactory)
    {
        this.resourceFactory = resourceFactory;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized DavSessionProvider getDavSessionProvider()
    {
        if (davSessionProvider == null) {
            davSessionProvider = new XWikiDavSessionProvider();
        }
        return davSessionProvider;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setDavSessionProvider(DavSessionProvider sessionProvider)
    {
        this.davSessionProvider = sessionProvider;
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthenticateHeaderValue()
    {
        return AUTHENTICATE_HEADER_VALUE;
    }

    /**
     * @return The {@link LockManager}. If no lock manager has been set or created, a new
     *         instance of {@link SimpleLockManager} is returned.
     */
    public LockManager getLockManager()
    {
        if (lockManager == null) {
            lockManager = new SimpleLockManager();
        }
        return lockManager;
    }

    /**
     * Sets the {@link LockManager}.
     * 
     * @param lockManager The {@link LockManager} to be used by this webdav-servlet.
     */
    public void setLockManager(LockManager lockManager)
    {
        this.lockManager = lockManager;
    }
    
    /**
     * Initialize the Container fields (request, response, session). Note that this is a bridge
     * between the old core and the component architecture. In the new component architecture we use
     * ThreadLocal to transport the request, response and session to components which require them.
     * In the future this Servlet will be replaced by the XWikiPlexusServlet Servlet.
     * 
     * @param context The {@link XWikiContext}.
     * @throws ServletException If one of Request, Response or Session initializations fails.
     */
    protected void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.ROLE);
        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(),
                context);
            containerInitializer.initializeResponse(context.getResponse()
                .getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    /**
     * We must ensure we clean the ThreadLocal variables located in the Container and Execution
     * components as otherwise we will have a potential memory leak.
     */
    protected void cleanupComponents()
    {
        Container container = (Container) Utils.getComponent(Container.ROLE);
        Execution execution = (Execution) Utils.getComponent(Execution.ROLE);
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

    /**
     * {@inheritDoc}
     */
    public void init() throws ServletException
    {
        super.init();
    }

    /**
     * {@inheritDoc}
     */
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        XWikiContext context = null;
        WebdavRequest webdavRequest = new WebdavRequestImpl(request, getLocatorFactory());
        // DeltaV requires 'Cache-Control' header for all methods except
        // 'VERSION-CONTROL' and 'REPORT'.
        int methodCode = DavMethods.getMethodCode(request.getMethod());

        // We need to add this to inform Windows to use WEBDAV.
        // response.setHeader("MS-Author-Via", "DAV");

        boolean noCache =
            DavMethods.isDeltaVMethod(webdavRequest)
                && !(DavMethods.DAV_VERSION_CONTROL == methodCode || DavMethods.DAV_REPORT == methodCode);
        WebdavResponse webdavResponse = new WebdavResponseImpl(response, noCache);
        try {
            // Attach session information for this request.
            if (!getDavSessionProvider().attachSession(webdavRequest)) {
                return;
            }
            // Create the WebDAV resource (uses the resource locator).
            XWikiDavResource resource =
                (XWikiDavResource) getResourceFactory().createResource(webdavRequest.getRequestLocator(),
                    webdavRequest, webdavResponse);

            context = resource.getXwikiContext();
            // Make sure we have plexus
            initializeContainerComponent(resource.getXwikiContext());

            // Make sure there is an authenticated user.
            if ("XWiki.XWikiGuest".equals(context.getUser())) {
                webdavResponse.setStatus(DavServletResponse.SC_UNAUTHORIZED);
                webdavResponse.setHeader(WWW_AUTHENTICATE_HEADER, getAuthenticateHeaderValue());
                return;
            }
            // Check for preconditions.
            if (!isPreconditionValid(webdavRequest, resource)) {
                webdavResponse.sendError(DavServletResponse.SC_PRECONDITION_FAILED);
                return;
            }
            // Finally, execute the corresponding DAV method.
            if (!execute(webdavRequest, webdavResponse, methodCode, resource)) {
                super.service(request, response);
            }
        } catch (DavException e) {
            if (e.getErrorCode() == HttpServletResponse.SC_UNAUTHORIZED) {
                webdavResponse.setHeader(WWW_AUTHENTICATE_HEADER, getAuthenticateHeaderValue());
                webdavResponse.sendError(e.getErrorCode(), e.getStatusPhrase());
            } else {
                LOG.error("WebDAV Exception Occurred : ", e);
                webdavResponse.sendError(e);
            }
        } finally {
            try {
                cleanupComponents();
                getDavSessionProvider().releaseSession(webdavRequest);
            } catch (Throwable e) {
                LOG.error("Exception Ocurred while cleaning up : ", e);
            }
            // Make sure we cleanup database connections
            // There could be cases where we have some
            if ((context != null) && (context.getWiki() != null)) {
                context.getWiki().getStore().cleanUp(context);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean execute(WebdavRequest request, WebdavResponse response, int method,
        DavResource resource) throws ServletException, IOException, DavException
    {
        XWikiDavResource res = (XWikiDavResource) resource;
        LOG.debug("Resource : [" + res.getDisplayName() + "] Method : [" + request.getMethod() + "]");
        return super.execute(request, response, method, resource);
    }
}
