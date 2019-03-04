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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.url.ExtendedURL;

/**
 * Decides how to route an incoming URL into the XWiki system. There are various possibilities:
 * <ul>
 *   <li>If there's a registered component of type {@link org.xwiki.resource.ResourceReferenceHandler} matching the
 *       {@link ResourceType} passed in the URL (for example when using the {@code standard} URL scheme, the Resource
 *       Type is the segment path just after the Context Path, i.e. {@code bin} in
 *       {@code http://<server>/xwiki/bin/view/Space/Page}), then the {@code resourceReferenceHandler} Servlet is
 *       called to handle it.</li>
 *   <li>If not, then continue executing the rest of the {@code web.xml} file, thus bridging to the old system,
 *       including the existing Struts Action Servlet.</li>
 * </ul>
 * As time progresses it is expected that more and more Resource Types will have registered
 * {@link org.xwiki.resource.ResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class RoutingFilter implements Filter
{
    static final String RESOURCE_TYPE_NAME = "resourceType";
    static final String RESOURCE_EXTENDEDURL = "resourceURL";

    private ComponentManager rootComponentManager;

    private ServletContext servletContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // Get the Component Manager which has been initialized first in a Servlet Context Listener.
        this.rootComponentManager =
            (ComponentManager) filterConfig.getServletContext().getAttribute(ComponentManager.class.getName());
        // Save the Servlet Context to be able to do forwards later on
        this.servletContext = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        // Only handle HTTP Servlet requests...
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Step 1: Construct an ExtendedURL to make it easy to manipulate the URL segments
        ExtendedURL extendedURL = constructExtendedURL(httpRequest);

        // Step 2: Extract the Resource Type from the ExtendedURL
        ResourceTypeResolver<ExtendedURL> urlResourceTypeResolver = getResourceTypeResolver();
        ResourceType resourceType;
        try {
            resourceType = urlResourceTypeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());
        } catch (Exception e) {
            // Failed to resolve the passed ExtendedURL. This means it's not a URL that should be handled by a Resource
            // Reference Handler and we let it go through so that the next Filter or Servlet from web.xml will handle
            // it. Note that since some URL schemes may want to handle features like short URLs where the Resource Type
            // is omitted, this catch will not be called in this case and this is why we need Step 2 below in order
            // to recognize static resources and serve them!
            chain.doFilter(request, response);
            return;
        }

        // Step 3: Handle static resources simply by letting them go through so that the Servlet Container will use its
        // default Servlet to serve static content.
        // Note: This step is a performance optimization only as it would also work to go directly at step 4 since there
        // would be no handler found for static resources and thus the Servlet Container would continue processing the
        // content of web.xml and would serve static files using its File Servlet.
        if (resourceType.isStatic()) {
            chain.doFilter(request, response);
            return;
        }

        // Step 4: Check if there's a Handler available for the Resource Type
        ResourceReferenceHandlerManager<ResourceType> resourceReferenceHandlerManager =
            getResourceReferenceHandlerManager();
        // Note that ATM the EntityResourceReferenceHandler is configured to NOT handle "bin" Resource Types. See
        // the comment in EntityResourceReferenceHandler#getSupportedResourceReferences() for more details. This allows
        // to fallback on the Filter/Servlet chain as defined by the web.xml and have XWikiAction be called to handle
        // "bin" Resource Types. It also allows other mappings to be used to handle other resource types such as
        // "rest", "webdav" and "xmlrpc" Resource Types.
        if (!resourceReferenceHandlerManager.canHandle(resourceType)) {
            // Let it go through so that the next Filter or Servlet from web.xml will handle it.
            chain.doFilter(request, response);
            return;
        }

        // Step 4: There is a Handler to handle our request, call the Resource Handler Servlet. Note that calling a
        // Sevlet gives us more flexibility if we wish to execute some more Filters before the Servlet executes for
        // example.
        //
        // However before doing that, we save the Resource Type so that the Servlet doesn't have to extract it again!
        // We also save the URL since we don't want to have to compute the full URL again in the Resource Reference
        // Handler Servlet!
        request.setAttribute(RESOURCE_TYPE_NAME, resourceType);
        request.setAttribute(RESOURCE_EXTENDEDURL, extendedURL);
        this.servletContext.getNamedDispatcher("resourceReferenceHandler").forward(request, response);
    }

    @Override
    public void destroy()
    {
        this.rootComponentManager = null;
        this.servletContext = null;
    }

    private ResourceReferenceHandlerManager<ResourceType> getResourceReferenceHandlerManager() throws ServletException
    {
        ResourceReferenceHandlerManager<ResourceType> resourceReferenceHandlerManager;
        try {
            resourceReferenceHandlerManager = this.rootComponentManager.getInstance(
                new DefaultParameterizedType(null, ResourceReferenceHandlerManager.class, ResourceType.class));
        } catch (ComponentLookupException e) {
            // Should not happen since a Resource Reference Handler should always exist on the system.
            throw new ServletException("Failed to locate a Resource Reference Handler Manager component", e);
        }
        return resourceReferenceHandlerManager;
    }

    private ResourceTypeResolver<ExtendedURL> getResourceTypeResolver() throws ServletException
    {
        ResourceTypeResolver<ExtendedURL> urlResourceTypeResolver;
        try {
            urlResourceTypeResolver = this.rootComponentManager.getInstance(
                new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));
        } catch (ComponentLookupException e) {
            // Should not happen since an ExtendedURL Resource Type Resolver should exist on the system.
            throw new ServletException("Failed to locate an ExtendedURL Resource Type Resolver component", e);
        }
        return urlResourceTypeResolver;
    }

    private ExtendedURL constructExtendedURL(HttpServletRequest httpRequest) throws ServletException
    {
        ExtendedURL extendedURL;
        URL url = getRequestURL(httpRequest);
        try {
            extendedURL = new ExtendedURL(url, httpRequest.getContextPath());
        } catch (CreateResourceReferenceException e) {
            throw new ServletException(String.format("Invalid URL [%s]", url), e);
        }
        return extendedURL;
    }

    /**
     * Reconstruct the full URL since the Servlet API doesn't provide a way to access it directly.
     */
    private URL getRequestURL(HttpServletRequest request) throws ServletException
    {
        URL url;
        try {
            StringBuffer requestURL = request.getRequestURL();
            String qs = request.getQueryString();
            if (!StringUtils.isEmpty(qs)) {
                url = new URL(requestURL.toString() + "?" + qs);
            } else {
                url = new URL(requestURL.toString());
            }
        } catch (MalformedURLException e) {
            // Shouldn't happen normally!
            throw new ServletException(
                String.format("Failed to reconstruct URL from HTTP Servlet Request (URL [%s], Query String [%s])",
                    request.getRequestURL(), request.getQueryString()), e);
        }
        return url;
    }
}
