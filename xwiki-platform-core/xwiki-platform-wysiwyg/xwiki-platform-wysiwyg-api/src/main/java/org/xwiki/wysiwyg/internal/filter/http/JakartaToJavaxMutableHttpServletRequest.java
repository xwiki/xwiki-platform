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

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.jakartabridge.servlet.internal.JakartaToJavaxServletRequest;
import org.xwiki.wysiwyg.filter.MutableJakartaServletRequest;
import org.xwiki.wysiwyg.filter.MutableServletRequest;

/**
 * @version $Id$
 */
public class JakartaToJavaxMutableHttpServletRequest extends JakartaToJavaxServletRequest<MutableServletRequest>
    implements MutableJakartaServletRequest
{
    /**
     * @param javax
     */
    public JakartaToJavaxMutableHttpServletRequest(MutableServletRequest javax)
    {
        super(javax);
    }

    @Override
    public String setParameter(String name, String value)
    {
        return this.javax.setParameter(name, value);
    }

    @Override
    public String[] setParameterValues(String name, String[] values)
    {
        return this.javax.setParameterValues(name, values);
    }

    @Override
    public String removeParameter(String name)
    {
        return this.javax.removeParameter(name);
    }

    @Override
    public void sendRedirect(ServletResponse response, String url) throws IOException
    {
        this.javax.sendRedirect(JakartaServletBridge.toJavax(response), url);
    }

    @Override
    public String getReferer()
    {
        return this.javax.getReferer();
    }

    @Override
    public Object getSessionAttribute(String attrName)
    {
        return this.javax.getSessionAttribute(attrName);
    }

    @Override
    public Object setSessionAttribute(String attrName, Object attrValue)
    {
        return this.javax.setSessionAttribute(attrName, attrValue);
    }

    @Override
    public ServletRequest getRequest()
    {
        return JakartaServletBridge.toJakarta(this.javax.getRequest());
    }
}
