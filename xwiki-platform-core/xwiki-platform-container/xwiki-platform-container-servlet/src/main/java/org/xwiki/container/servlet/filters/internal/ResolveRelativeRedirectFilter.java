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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.servlet.HttpServletUtils;

/**
 * Filter which inject a response wrapper in charge of resolving the relative URLs on XWiki side instead of application
 * server side, to reduce setup requirements.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 */
public class ResolveRelativeRedirectFilter implements Filter
{
    /**
     * Response wrapper in charge of resolving relative references to absolute ones.
     * 
     * @version $Id$
     */
    public class ResolveRelativeRedirectResponse extends HttpServletResponseWrapper
    {
        private final HttpServletRequest request;

        private URI sourceURI;

        /**
         * @param request the request
         * @param response the wrapped response
         * @throws MalformedURLException when failing to get the current URL
         */
        public ResolveRelativeRedirectResponse(HttpServletRequest request, HttpServletResponse response)
            throws MalformedURLException
        {
            super(response);

            this.request = request;
        }

        /**
         * @return the sourceURL
         */
        public URI getSourceURI()
        {
            if (this.sourceURI == null) {
                try {
                    this.sourceURI = HttpServletUtils.getSourceURL(this.request).toURI();
                } catch (URISyntaxException | MalformedURLException e) {
                    // It's very unlikely that the source URL would be invalid, but just ignore it in this case
                }
            }

            return this.sourceURI;
        }

        @Override
        public void sendRedirect(String location) throws IOException
        {
            String url = location;

            // Resolve relative URLs
            if (StringUtils.isNotBlank(location)) {
                try {
                    URI locationURI = new URI(location);
                    if (!locationURI.isAbsolute()) {
                        URI referenceURI = getSourceURI();
                        if (referenceURI != null) {
                            url = referenceURI.resolve(location).toString();
                        }
                    }
                } catch (URISyntaxException e) {
                    // Let invalid URIs go through
                }
            }

            // Redirect
            super.sendRedirect(url);
        }
    }

    private boolean resolveRedirect = true;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletResponse filteredResponse = response;

        if (this.resolveRedirect && filteredResponse instanceof HttpServletResponse httpResponse) {
            filteredResponse = new ResolveRelativeRedirectResponse((HttpServletRequest) request, httpResponse);
        }

        chain.doFilter(request, filteredResponse);
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
