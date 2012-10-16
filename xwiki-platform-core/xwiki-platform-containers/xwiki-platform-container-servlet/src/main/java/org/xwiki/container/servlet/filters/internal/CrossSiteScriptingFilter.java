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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Use this filter when the portal has cross-site scripting protection enabled. It will decode the HTML user input that
 * was encoded by the portal to prevent XSS attacks.
 * 
 * @version $Id$
 */
public class CrossSiteScriptingFilter implements Filter
{
    /**
     * The name of the request attribute used to prevent the filter from being applied multiple times.
     */
    private static final String APPLIED = CrossSiteScriptingFilter.class.getName() + ".applied";

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException
    {
        if (request.getAttribute(APPLIED) != Boolean.TRUE) {
            request.setAttribute(APPLIED, Boolean.TRUE);
            if (request instanceof HttpServletRequest && request.getAttribute("javax.portlet.request") != null
                && requiresDecoding(request)) {
                chain.doFilter(decode((HttpServletRequest) request), response);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * @param request the request
     * @return {@code true} if the request has any parameter whose value contains {@code &lt;} or {@code &gt;},
     *         {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean requiresDecoding(ServletRequest request)
    {
        for (String[] values : ((Map<String, String[]>) request.getParameterMap()).values()) {
            for (String value : values) {
                if (value.contains("&lt;") || value.contains("&gt;")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Decodes the HTML in all the request parameters.
     * 
     * @param request the request
     * @return the new request with the modified parameters
     */
    private ServletRequest decode(HttpServletRequest request)
    {
        @SuppressWarnings("unchecked")
        final Map<String, String[]> parameters = decode(request.getParameterMap());
        return new HttpServletRequestWrapper(request)
        {
            @Override
            public Map<String, String[]> getParameterMap()
            {
                return parameters;
            }
        };
    }

    /**
     * Decodes the HTML in all the given parameters.
     * 
     * @param encodedParameters the parameters
     * @return the new parameter map
     */
    private Map<String, String[]> decode(Map<String, String[]> encodedParameters)
    {
        Map<String, String[]> parameters = new HashMap<String, String[]>(encodedParameters);
        for (String[] values : parameters.values()) {
            for (int i = 0; i < values.length; i++) {
                values[i] = decode(values[i]);
            }
        }
        return parameters;
    }

    /**
     * Decodes the HTML special characters from the given string.
     * 
     * @param value the value to be decoded
     * @return the result
     */
    private String decode(String value)
    {
        return StringEscapeUtils.unescapeXml(value);
    }

    @Override
    public void init(FilterConfig config) throws ServletException
    {
    }
}
