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
package org.xwiki.rest.jersey.internal;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * Extends {@link ServletContainer} to add XWiki specific pieces.
 * <ul>
 * <li>Injection of XWikiResource components</li>
 * <li>Make sure "+" is decoded like it used to before Jersey</li>
 * </ul>
 * <p>
 * While the class is much older, the since annotation was moved to 17.0.0RC1 because it implement a completely
 * different API from Java point of view.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 */
public class XWikiRESTServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private volatile JerseyServletContainer container;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
    {
        if (this.container == null) {
            // Do the initialization at the beginning of the first request because we need the previous filters to be
            // executed (which is not the case in #init)
            initializeContainer(req.getServletContext());
        }

        // Only HTTP requests are supported
        if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse)) {
            throw new ServletException("non-HTTP request or response");
        }

        // Make sure "+" is decoded like it used to before Jersey
        HttpServletRequest httpRequest = (HttpServletRequest) req;
        String requestURI = httpRequest.getRequestURI();
        if (StringUtils.contains(requestURI, '+')) {
            String encodedRequestURI = encodePlus(requestURI);
            @SuppressWarnings("java:S1149")
            StringBuffer encodedRequestUrl = new StringBuffer(encodePlus(httpRequest.getRequestURL().toString()));
            httpRequest = new HttpServletRequestWrapper(httpRequest)
            {
                @Override
                public String getRequestURI()
                {
                    return encodedRequestURI;
                }

                @Override
                public StringBuffer getRequestURL()
                {
                    return encodedRequestUrl;
                }
            };
        }

        this.container.service(httpRequest, res);
    }

    private String encodePlus(String path)
    {
        return path.replace("+", "%20");
    }

    private synchronized void initializeContainer(ServletContext servletContext) throws ServletException
    {
        if (this.container != null) {
            return;
        }

        // Get the XWiki component manager
        ComponentManager rootComponentManager =
            (ComponentManager) servletContext.getAttribute(ComponentManager.class.getName());

        // Set the component manager as parent of the injection manager
        servletContext.setAttribute(ServletProperties.SERVICE_LOCATOR, rootComponentManager);

        // Lookup the Jersey container
        try {
            JerseyServletContainer newContainer = rootComponentManager.getInstance(JerseyServletContainer.class);

            // Initialize the Jersey container
            newContainer.init(getServletConfig());

            // Remember the container
            this.container = newContainer;
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to lookup the Jersey container", e);
        }
    }

    @Override
    public void destroy()
    {
        this.container.destroy();
    }

    @Override
    public ServletContext getServletContext()
    {
        return this.container.getServletContext();
    }
}
