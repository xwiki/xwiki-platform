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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.url.URLSecurityManager;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Filter which inject a response wrapper in charge of converting the redirect location into a safe URL.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class SafeRedirectFilter implements Filter
{
    /**
     * Response wrapper in charge of checking the references.
     * 
     * @version $Id$
     */
    public class SafeRedirectResponse extends HttpServletResponseWrapper
    {
        private static final Logger LOGGER = LoggerFactory.getLogger(SafeRedirectResponse.class);

        private final URLSecurityManager urlSecurityManager;

        /**
         * @param urlSecurityManager the tool in charge of validating the URL
         * @param response the wrapped response
         */
        public SafeRedirectResponse(URLSecurityManager urlSecurityManager, HttpServletResponse response)
        {
            super(response);

            this.urlSecurityManager = urlSecurityManager;
        }

        @Override
        public void sendRedirect(String location) throws IOException
        {
            if (!StringUtils.isBlank(location)) {
                URI uri;
                try {
                    uri = this.urlSecurityManager.parseToSafeURI(location);

                    super.sendRedirect(uri.toString());
                } catch (URISyntaxException | SecurityException e) {
                    LOGGER.warn(
                        "Possible phishing attack, attempting to redirect to [{}], this request has been blocked. "
                            + "If the request was legitimate, please check the URL security configuration. You "
                            + "might need to add the domain related to this request in the list of trusted domains in "
                            + "the configuration: it can be configured in xwiki.properties in url.trustedDomains.",
                        location);
                    LOGGER.debug("Original error preventing the redirect: ", e);
                }
            }
        }
    }

    private URLSecurityManager urlSecurityManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletResponse finalResponse = response;

        if (finalResponse instanceof HttpServletResponse httpResponse) {
            finalResponse = new SafeRedirectResponse(this.urlSecurityManager, httpResponse);
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
            this.urlSecurityManager = rootComponentManager.getInstance(URLSecurityManager.class);
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to access configuration", e);
        }
    }
}
