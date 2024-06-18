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
package org.xwiki.resource.servlet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.url.ExtendedURL;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles any Resource Reference discovered by the Routing Filter and put in the HTTP Request. Any module who wish to
 * add a new Resource Type in the XWiki URL simply needs to register a Handler component (of role
 * {@link org.xwiki.resource.ResourceReferenceHandler}) and any URL matching the corresponding {@link ResourceType} will
 * be handled.
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implements a completely
 * different API from Java point of view.
 *
 * @version $Id$
 * @since 42.0.0
 */
public class ResourceReferenceHandlerServlet extends HttpServlet
{
    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    private transient ComponentManager rootComponentManager;

    @Override
    public void init() throws ServletException
    {
        super.init();

        // Get the Component Manager which has been initialized first in a Servlet Context Listener.
        this.rootComponentManager =
            (ComponentManager) getServletContext().getAttribute(ComponentManager.class.getName());
    }

    @Override
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
        throws ServletException, IOException
    {
        try {
            // Before handling the Resource Reference we need to setup the Request/Response so that it's available to
            // the Handlers (for writing content to the response for example!)
            // Note that we don't initialize other things such as the XWiki Contexts for example since we assume that
            // not all Resource Handlers require the XWiki Contexts (the WebJar Resource Handler doesn't need it for
            // example). Thus it's up for the specific Resource Handlers to initialize anything else they need to work.
            // We just initialize the Request/Response/Session here as we consider them to be basic needs for all
            // Resource Handlers.
            initializeContainerComponent(httpRequest, httpResponse);

            handleResourceReference(getResourceReference(httpRequest));
        } finally {
            cleanupComponents();
        }
    }

    private ResourceReference getResourceReference(HttpServletRequest httpRequest) throws ServletException
    {
        // Get the Resource Type from the request's attribute, where it's been put by the RoutingFilter.
        ResourceType resourceType = (ResourceType) httpRequest.getAttribute(RoutingFilter.RESOURCE_TYPE_NAME);

        // Get the ExtendedURL from the request's attribute too (so that we don't have to compute it again).
        ExtendedURL extendedURL = (ExtendedURL) httpRequest.getAttribute(RoutingFilter.RESOURCE_EXTENDEDURL);

        // Extract the Resource Reference, passing the already extracted Resource Type
        ResourceReferenceResolver<ExtendedURL> urlResolver = getResourceReferenceResolver();
        try {
            // Note that before this code executes a valid Execution Context must be available as it's required to
            // resolve the wiki referenced by the URL (since this means looking for Wiki Descriptors and do queries on
            // the store.
            return urlResolver.resolve(extendedURL, resourceType, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            // This shouldn't happen, raise an exception
            throw new ServletException(String.format("Failed to extract the Resource Reference from the URL [%s]",
                extendedURL.getWrappedURL()), e);
        }
    }

    private ResourceReferenceResolver<ExtendedURL> getResourceReferenceResolver() throws ServletException
    {
        ResourceReferenceResolver<ExtendedURL> urlResolver;
        try {
            Type role = new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class);
            urlResolver = this.rootComponentManager.getInstance(role);
        } catch (ComponentLookupException e) {
            // Should not happen since a URL provider should exist on the system.
            throw new ServletException("Failed to locate an ExtendedURL Resource Reference Resolver component", e);
        }
        return urlResolver;
    }

    private void initializeContainerComponent(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
        throws ServletException
    {
        ServletContainerInitializer containerInitializer;
        try {
            containerInitializer = this.rootComponentManager.getInstance(ServletContainerInitializer.class);
        } catch (Exception e) {
            // This shouldn't happen, raise an exception
            throw new ServletException("Failed to locate a ServletContainerInitializer component", e);
        }
        try {
            containerInitializer.initializeRequest(httpRequest, httpResponse);
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    private void handleResourceReference(ResourceReference resourceReference) throws ServletException
    {
        ResourceReferenceHandlerManager<?> resourceReferenceHandlerManager;
        try {
            Type role = new DefaultParameterizedType(null, ResourceReferenceHandlerManager.class, ResourceType.class);
            resourceReferenceHandlerManager = this.rootComponentManager.getInstance(role);
        } catch (ComponentLookupException e) {
            // Should not happen since a Resource Reference Handler should always exist on the system.
            throw new ServletException("Failed to locate a Resource Reference Handler Manager component", e);
        }

        try {
            resourceReferenceHandlerManager.handle(resourceReference);
        } catch (ResourceReferenceHandlerException e) {
            throw new ServletException(String.format("Failed to handle Resource Reference [%s]", resourceReference), e);
        }
    }

    private void cleanupComponents() throws ServletException
    {
        Container container;
        try {
            container = this.rootComponentManager.getInstance(Container.class);
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to locate a Container component", e);
        }

        Execution execution;
        try {
            execution = this.rootComponentManager.getInstance(Execution.class);
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to locate a Execution component", e);
        }

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution components as
        // otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }
}
