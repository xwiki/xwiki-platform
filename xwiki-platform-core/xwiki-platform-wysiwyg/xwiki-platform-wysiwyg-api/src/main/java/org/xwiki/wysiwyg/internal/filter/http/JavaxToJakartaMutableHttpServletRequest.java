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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.jakartabridge.servlet.internal.JavaxToJakartaServletRequest;
import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;
import org.xwiki.wysiwyg.filter.MutableServletRequest;

/**
 * @version $Id$
 */
public class JavaxToJakartaMutableHttpServletRequest extends JavaxToJakartaServletRequest<MutableJakartaServletRequest>
    implements MutableServletRequest
{
    /**
     * @param wrapped
     */
    public JavaxToJakartaMutableHttpServletRequest(MutableJakartaServletRequest wrapped)
    {
        super(wrapped);
    }

    @Override
    public String setParameter(String name, String value)
    {
        return this.wrapped.setParameter(name, value);
    }

    @Override
    public String[] setParameterValues(String name, String[] values)
    {
        return this.wrapped.setParameterValues(name, values);
    }

    @Override
    public String removeParameter(String name)
    {
        return this.wrapped.removeParameter(name);
    }

    @Override
    public void sendRedirect(ServletResponse response, String url) throws IOException
    {
        this.wrapped.sendRedirect(JakartaServletBridge.toJakarta(response), url);
    }

    @Override
    public String getReferer()
    {
        return this.wrapped.getReferer();
    }

    @Override
    public Object getSessionAttribute(String attrName)
    {
        return this.wrapped.getSessionAttribute(attrName);
    }

    @Override
    public Object setSessionAttribute(String attrName, Object attrValue)
    {
        return this.wrapped.setSessionAttribute(attrName, attrValue);
    }

    @Override
    public ServletRequest getRequest()
    {
        return JakartaServletBridge.toJavax(this.wrapped.getRequest());
    }
}
