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
package org.xwiki.wysiwyg.internal.filter.http;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;
import org.xwiki.wysiwyg.filter.MutableServletRequest;

import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link MutableServletRequest} implementation for the HTTP protocol.
 * 
 * @version $Id$
 */
public class MutableJakartaHttpServletRequest extends HttpServletRequestWrapper implements MutableJakartaServletRequest
{
    /**
     * Parameters used instead of those from the wrapped request. This way exiting request parameters can be overwritten
     * and also new parameters can be added.
     */
    private final Map<String, String[]> params = new HashMap<>();

    /**
     * Wraps the specified request and copies its parameters to {@link #params} where they can be overwritten later.
     * 
     * @param request The request to be wrapped.
     */
    public MutableJakartaHttpServletRequest(HttpServletRequest request)
    {
        super(request);

        params.putAll(request.getParameterMap());
    }

    @Override
    public String setParameter(String name, String value)
    {
        String[] previousValues = params.put(name, new String[] {value});
        return (previousValues == null || previousValues.length == 0) ? null : previousValues[0];
    }

    @Override
    public String[] setParameterValues(String name, String[] values)
    {
        return params.put(name, values);
    }

    @Override
    public String removeParameter(String name)
    {
        String[] previousValues = params.remove(name);
        return (previousValues == null || previousValues.length == 0) ? null : previousValues[0];
    }

    @Override
    public String getParameter(String name)
    {
        String[] values = params.get(name);
        return (values == null || values.length == 0) ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(params.keySet());
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return params.get(name);
    }

    @Override
    public void sendRedirect(ServletResponse res, String url) throws IOException
    {
        ((HttpServletResponse) res).sendRedirect(url);
    }

    @Override
    public String getReferer()
    {
        return getHeader("Referer");
    }

    @Override
    public Object getSessionAttribute(String attrName)
    {
        return getSession().getAttribute(attrName);
    }

    @Override
    public Object setSessionAttribute(String attrName, Object attrValue)
    {
        Object oldValue = getSession().getAttribute(attrName);
        getSession().setAttribute(attrName, attrValue);
        return oldValue;
    }
}
