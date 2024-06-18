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
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that set the desired header of the HTTP response to the desired value.
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely
 * different API from Java point of view.
 *
 * @version $Id$
 * @since 42.0.0
 */
public class SetHTTPHeaderFilter implements Filter
{
    private String httpHeaderName;

    private String httpHeaderValue;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        httpHeaderName = filterConfig.getInitParameter("name");
        httpHeaderValue = filterConfig.getInitParameter("value");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        // If the response is an HTTP response
        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            // Set the attribute
            httpResponse.addHeader(httpHeaderName, httpHeaderValue);
        }

        // Pass control on to the next filter
        chain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
        // Nothing to do
    }
}
