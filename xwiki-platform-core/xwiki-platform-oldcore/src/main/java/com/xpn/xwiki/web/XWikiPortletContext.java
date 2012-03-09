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
package com.xpn.xwiki.web;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.portlet.PortletContext;

public class XWikiPortletContext implements XWikiEngineContext
{
    private PortletContext pcontext;

    public XWikiPortletContext(PortletContext pcontext)
    {
        this.pcontext = pcontext;
    }

    public PortletContext getPortletContext()
    {
        return pcontext;
    }

    @Override
    public Object getAttribute(String name)
    {
        return pcontext.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        pcontext.setAttribute(name, value);
    }

    @Override
    public String getRealPath(String path)
    {
        return pcontext.getRealPath(path);
    }

    @Override
    public URL getResource(String name) throws MalformedURLException
    {
        return pcontext.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        return pcontext.getResourceAsStream(name);
    }

    @Override
    public String getMimeType(String filename)
    {
        return pcontext.getMimeType(filename);
    }
}
