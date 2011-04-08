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
package org.xwiki.wysiwyg.server.internal.filter.http;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.wysiwyg.server.filter.MutableServletRequest;


/**
 * {@link MutableServletRequest} implementation for the HTTP protocol.
 * 
 * @version $Id$
 */
public class MutableHttpServletRequest extends HttpServletRequestWrapper implements MutableServletRequest
{
    /**
     * Parameters used instead of those from the wrapped request. This way exiting request parameters can be overwritten
     * and also new parameters can be added.
     */
    private final Map<String, String[]> params = new HashMap<String, String[]>();

    /**
     * Wraps the specified request and copies its parameters to {@link #params} where they can be overwritten later.
     * 
     * @param request The request to be wrapped.
     */
    @SuppressWarnings("unchecked")
    public MutableHttpServletRequest(HttpServletRequest request)
    {
        super(request);

        params.putAll(request.getParameterMap());
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableServletRequest#setParameter(String, String)
     */
    public String setParameter(String name, String value)
    {
        String[] previousValues = params.put(name, new String[] {value});
        return (previousValues == null || previousValues.length == 0) ? null : previousValues[0];
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableServletRequest#setParameterValues(String, String[])
     */
    public String[] setParameterValues(String name, String[] values)
    {
        return params.put(name, values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableServletRequest#removeParameter(String)
     */
    public String removeParameter(String name)
    {
        String[] previousValues = params.remove(name);
        return (previousValues == null || previousValues.length == 0) ? null : previousValues[0];
    }

    /**
     * {@inheritDoc}
     * 
     * @see HttpServletRequestWrapper#getParameter(String)
     */
    public String getParameter(String name)
    {
        String[] values = params.get(name);
        return (values == null || values.length == 0) ? null : values[0];
    }

    /**
     * {@inheritDoc}
     * 
     * @see HttpServletRequestWrapper#getParameterMap()
     */
    public Map<String, String[]> getParameterMap()
    {
        return Collections.unmodifiableMap(params);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HttpServletRequestWrapper#getParameterNames()
     */
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(params.keySet());
    }

    /**
     * {@inheritDoc}
     * 
     * @see HttpServletRequestWrapper#getParameterValues(String)
     */
    public String[] getParameterValues(String name)
    {
        return params.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableHttpServletRequest#sendRedirect(ServletResponse, String)
     */
    public void sendRedirect(ServletResponse res, String url) throws IOException
    {
        ((HttpServletResponse) res).sendRedirect(url);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableServletRequest#getReferer()
     */
    public String getReferer()
    {
        return getHeader("Referer");
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableServletRequest#setSessionAttribute(String, Object)
     */
    public Object getSessionAttribute(String attrName)
    {
        return getSession().getAttribute(attrName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MutableServletRequest#setSessionAttribute(String, Object)
     */
    public Object setSessionAttribute(String attrName, Object attrValue)
    {
        Object oldValue = getSession().getAttribute(attrName);
        getSession().setAttribute(attrName, attrValue);
        return oldValue;
    }
}
