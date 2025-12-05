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
import java.net.URL;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.servlet.HttpServletUtils;

/**
 * Workaround bugs in various application servers not properly taking into account proxy headers indicating the source
 * URL.
 * 
 * @version $Id$
 * @since 17.5.0
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
        private String superScheme;

        private String superServerName;

        private int superServerPort;

        // HttpServletRequest#getRequestURL() returns a StringBuffer so we don't really have a choice here
        @SuppressWarnings("java:S1149")
        private StringBuffer superRequestURL;

        private URL baseURL;

        // HttpServletRequest#getRequestURL() returns a StringBuffer so we don't really have a choice here
        @SuppressWarnings("java:S1149")
        private StringBuffer requestURL;

        /**
         * @param request the request
         */
        public SourceURLResolverRequest(HttpServletRequest request)
        {
            super(request);
        }

        // We just want to know if something changed in the wrapped request, we don't need to actually compare the
        // content of the Strings for that.
        @SuppressWarnings("java:S4973")
        private boolean superBaseURLChanged()
        {
            return this.superScheme != super.getScheme() || this.superServerName != super.getServerName()
                || this.superServerPort != super.getServerPort();
        }

        private boolean checkBaseURL()
        {
            // Check if super reference changes (which would suggest the values changed and we should recalculate the
            // URL)
            if (superBaseURLChanged()) {
                try {
                    this.baseURL = HttpServletUtils.getSourceBaseURL((HttpServletRequest) getRequest());
                } catch (MalformedURLException e) {
                    LOGGER.error("Failed to resolve the source base URL, falling back to standard ServletRequest.", e);

                    this.baseURL = null;
                }

                // Remember current super references
                this.superScheme = super.getScheme();
                this.superServerName = super.getServerName();
                this.superServerPort = super.getServerPort();

                return true;
            }

            return false;
        }

        private boolean checkURL()
        {
            if ((checkBaseURL() || this.superRequestURL != super.getRequestURL()) && this.baseURL != null) {
                try {
                    this.requestURL = new StringBuffer(new URL(this.baseURL, super.getRequestURI()).toString());
                } catch (MalformedURLException e) {
                    LOGGER.error("Failed to generate the source URL, falling back to standard ServletRequest.", e);

                    this.requestURL = null;
                }

                // Remember current super references
                this.superRequestURL = super.getRequestURL();

                return true;
            }

            return false;
        }

        @Override
        public String getScheme()
        {
            checkBaseURL();

            return this.baseURL != null ? this.baseURL.getProtocol() : super.getScheme();
        }

        @Override
        public String getServerName()
        {
            checkBaseURL();

            return this.baseURL != null ? this.baseURL.getHost() : super.getServerName();
        }

        @Override
        public int getServerPort()
        {
            checkBaseURL();

            return this.baseURL != null ? this.baseURL.getPort() : super.getServerPort();
        }

        @Override
        public StringBuffer getRequestURL()
        {
            checkURL();

            return this.requestURL != null ? this.requestURL : super.getRequestURL();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletRequest filteredRequest = request;

        if (filteredRequest instanceof HttpServletRequest httpRequest) {
            filteredRequest = new SourceURLResolverRequest(httpRequest);
        }

        chain.doFilter(filteredRequest, response);
    }
}
