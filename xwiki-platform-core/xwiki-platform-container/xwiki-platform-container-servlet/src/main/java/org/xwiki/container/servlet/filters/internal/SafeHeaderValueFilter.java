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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.lang3.StringUtils;

/**
 * Filter which inject a response wrapper in charge of replacing the line breaks in header values by spaces.
 * <p>
 * Line breaks are forbidden in HTTP header values. Servlet containers replace them by spaces over HTTP/1.1, but not
 * necessarily over HTTP/2, where browsers then reject the whole response.
 * 
 * @version $Id$
 * @since 18.9.0RC1
 * @since 18.4.8
 * @since 17.10.15
 */
public class SafeHeaderValueFilter implements Filter
{
    /**
     * Response wrapper in charge of replacing the line breaks in header values.
     * 
     * @version $Id$
     */
    public static class SafeHeaderValueResponse extends HttpServletResponseWrapper
    {
        /**
         * @param response the wrapped response
         */
        public SafeHeaderValueResponse(HttpServletResponse response)
        {
            super(response);
        }

        @Override
        public void setHeader(String name, String value)
        {
            super.setHeader(name, cleanHeaderValue(value));
        }

        @Override
        public void addHeader(String name, String value)
        {
            super.addHeader(name, cleanHeaderValue(value));
        }

        private static String cleanHeaderValue(String value)
        {
            return StringUtils.replaceChars(value, "\r\n", "  ");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletResponse filteredResponse = response;

        if (filteredResponse instanceof HttpServletResponse httpResponse) {
            filteredResponse = new SafeHeaderValueResponse(httpResponse);
        }

        chain.doFilter(request, filteredResponse);
    }
}
