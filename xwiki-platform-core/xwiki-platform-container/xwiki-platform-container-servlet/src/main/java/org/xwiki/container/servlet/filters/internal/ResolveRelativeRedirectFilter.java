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
package org.xwiki.container.servlet.filters.internal;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.servlet.HttpServletUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Filter which inject a response wrapper in charge of resolving the relative URLs on XWiki side instead of application
 * server side, to reduce setup requirements.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class ResolveRelativeRedirectFilter implements Filter
{
    /**
     * Response wrapper in charge of resolving relative references to absolute ones.
     * 
     * @version $Id$
     */
    public class AbsoluteRedirectResponse extends HttpServletResponseWrapper
    {
        private final String sourceBaseURL;

        /**
         * @param sourceBaseURL the base of the URL sent by the client
         * @param response the wrapped response
         */
        public AbsoluteRedirectResponse(String sourceBaseURL, HttpServletResponse response)
        {
            super(response);

            this.sourceBaseURL = sourceBaseURL;
        }

        @Override
        public void sendRedirect(String location) throws IOException
        {
            // Resolve relative URLs
            if (StringUtils.isNotEmpty(location) && location.charAt(0) == '/') {
                sendRedirect(this.sourceBaseURL + location);
            }
        }
    }

    private boolean resolveRedirect = true;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletResponse finalResponse = response;

        if (this.resolveRedirect && finalResponse instanceof HttpServletResponse httpResponse) {
            String baseURLString = StringUtils
                .removeEnd(HttpServletUtils.getSourceBaseURL((HttpServletRequest) request).toExternalForm(), "/");
            finalResponse = new AbsoluteRedirectResponse(baseURLString, httpResponse);
        }

        chain.doFilter(request, finalResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        // Get the Component Manager which has been initialized first in a Servlet Context Listener.
        ComponentManager rootComponentManager =
            (ComponentManager) filterConfig.getServletContext().getAttribute(ComponentManager.class.getName());

        // Get the configuration
        try {
            ConfigurationSource properties =
                rootComponentManager.getInstance(ConfigurationSource.class, "xwikiproperties");

            this.resolveRedirect = properties.getProperty("container.request.resolveRelativeRedirect", true);
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to access configuration", e);
        }
    }
}
