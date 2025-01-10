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
package org.xwiki.container.wrap;

import java.util.Enumeration;

import org.xwiki.container.Request;
import org.xwiki.user.UserReference;

/**
 * A wrapper around {@link Request}.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class WrappingRequest implements Request
{
    protected final Request request;

    /**
     * @param request the wrapped request
     */
    public WrappingRequest(Request request)
    {
        this.request = request;
    }

    /**
     * @return the wrapped request
     */
    public Request getRequest()
    {
        return this.request;
    }

    @Override
    public Object getParameter(String key)
    {
        return this.request.getParameter(key);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return this.request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return this.request.getParameterValues(name);
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.request.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return this.request.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        this.request.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name)
    {
        this.request.removeAttribute(name);
    }

    @Override
    public UserReference getEffectiveAuthor()
    {
        return this.request.getEffectiveAuthor();
    }
}
