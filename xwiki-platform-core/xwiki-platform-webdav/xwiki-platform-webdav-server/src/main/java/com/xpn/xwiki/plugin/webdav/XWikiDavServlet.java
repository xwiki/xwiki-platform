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
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.jackrabbit.webdav.simple.LocatorFactoryImplEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavContext;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavResourceFactory;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavSessionProvider;
import com.xpn.xwiki.web.Utils;

/**
 * The servlet responsible for handling WebDAV requests on /xwiki/webdav.
 * 
 * @version $Id$
 */
public class XWikiDavServlet extends AbstractWebdavServlet
{
    /**
     * Class version.
     */
    private static final long serialVersionUID = 7255582612577585483L;

    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(XWikiDavServlet.class);
    
    /**
     * WWW-Authenticate header string.
     */
    public static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    /**
     * Locator factory. {@link DavLocatorFactory}
     */
    private transient DavLocatorFactory locatorFactory;

    /**
     * Resource factory. {@link DavResourceFactory}
     */
    private transient DavResourceFactory resourceFactory;

    /**
     * Session provider. {@link DavSessionProvider}
     */
    private transient DavSessionProvider sessionProvider;

    @Override
    public void init() throws ServletException
    {
        super.init();
        setLocatorFactory(new LocatorFactoryImplEx(""));
        setResourceFactory(new XWikiDavResourceFactory(getServletContext()));
        setDavSessionProvider(new XWikiDavSessionProvider());
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        XWikiDavContext context = null;
        WebdavRequest webdavRequest = new WebdavRequestImpl(request, getLocatorFactory());
        WebdavResponse webdavResponse = new WebdavResponseImpl(response, false);
        int methodCode = DavMethods.getMethodCode(request.getMethod());
        try {
            // Attach session information for this request.
            if (!getDavSessionProvider().attachSession(webdavRequest)) {
                return;
            }
            // Create the WebDAV resource (uses the resource locator).
            XWikiDavResource resource =
                (XWikiDavResource) getResourceFactory().createResource(
                    webdavRequest.getRequestLocator(), webdavRequest, webdavResponse);

            context = resource.getContext();
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
                logger.error("WebDAV Exception Occurred : ", e);
                webdavResponse.sendError(e);
            }
        } finally {
            cleaUp(webdavRequest, context);
        }
    }

    @Override
    protected boolean execute(WebdavRequest request, WebdavResponse response, int method,
        DavResource resource) throws ServletException, IOException, DavException
    {        
        logger.debug(String.format("Resource: [%s] Method: [%s]", resource.getDisplayName(), request.getMethod()));
        return super.execute(request, response, method, resource);
    }

    @Override
    protected boolean isPreconditionValid(WebdavRequest request, DavResource resource)
    {
        return !resource.exists() || request.matchesIfHeader(resource);
    }
    
    @Override
    public String getAuthenticateHeaderValue()
    {
        return "Basic realm=\"XWiki Webdav Server\"";
    }
    
    @Override
    public DavLocatorFactory getLocatorFactory()
    {        
        return this.locatorFactory;
    }
    
    @Override
    public void setLocatorFactory(DavLocatorFactory locatorFactory)
    {
        this.locatorFactory = locatorFactory;
    }
    
    @Override
    public DavResourceFactory getResourceFactory()
    {        
        return this.resourceFactory;
    }
    
    @Override
    public void setResourceFactory(DavResourceFactory resourceFactory)
    {
        this.resourceFactory = resourceFactory;
    }

    @Override
    public synchronized DavSessionProvider getDavSessionProvider()
    {        
        return this.sessionProvider;
    }
    
    @Override
    public synchronized void setDavSessionProvider(DavSessionProvider sessionProvider)
    {
        this.sessionProvider = sessionProvider;
    }
    
    /**
     * We must ensure we clean the ThreadLocal variables located in the Container and Execution
     * components as otherwise we will have a potential memory leak.
     */
    public void cleaUp(WebdavRequest request, XWikiDavContext context)
    {
        Container container = (Container) Utils.getComponent(Container.class);
        Execution execution = (Execution) Utils.getComponent(Execution.class);
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
        getDavSessionProvider().releaseSession(request);
        if (context != null) {
            context.cleanUp();
        }
    }
}
