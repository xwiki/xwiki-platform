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
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.servlet.HttpServletUtils;

/**
 * Workaround bugs in various application servers not properly taking into account proxy headers indicating the source
 * URL.
 * 
 * @version $Id$
 * @since 17.4.0RC1
 * @since 16.10.9
 */
public class SourceURLResolverFilter implements Filter
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(SourceURLResolverFilter.class);

    /**
     * Workaround bugs in various application servers not properly taking into account proxy headers indicating the
     * source URL.
     * 
     * @version $Id$
     */
    public class SourceURLResolverRequest extends HttpServletRequestWrapper
    {
        private final URI sourceURI;

        private final StringBuffer requestURL;

        /**
         * @param request the request
         * @param sourceURI the source URI resolved by XWiki
         * @throws MalformedURLException when failing to get the current URL
         */
        public SourceURLResolverRequest(HttpServletRequest request, URI sourceURI) throws MalformedURLException
        {
            super(request);

            this.sourceURI = sourceURI;
            this.requestURL = new StringBuffer(sourceURI.toString());
        }

        @Override
        public String getScheme()
        {
            return this.sourceURI.getScheme();
        }

        @Override
        public String getServerName()
        {
            return this.sourceURI.getHost();
        }

        @Override
        public int getServerPort()
        {
            return this.sourceURI.getPort();
        }

        @Override
        public StringBuffer getRequestURL()
        {
            return this.requestURL;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletRequest filteredRequest = request;

        if (filteredRequest instanceof HttpServletRequest httpRequest) {
            try {
                filteredRequest =
                    new SourceURLResolverRequest(httpRequest, HttpServletUtils.getSourceURL(httpRequest).toURI());
            } catch (URISyntaxException | MalformedURLException e) {
                // It's very unlikely that the source URL would be invalid, but just ignore it in this case
                LOGGER.warn("Failed to resolve the source URL ([{}]), falling back to standard ServletRequest.",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        chain.doFilter(filteredRequest, response);
    }
}
