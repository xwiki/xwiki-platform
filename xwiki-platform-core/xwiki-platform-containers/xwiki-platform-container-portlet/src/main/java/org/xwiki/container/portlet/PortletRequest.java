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
package org.xwiki.container.portlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xwiki.container.Request;
import org.xwiki.url.XWikiURL;

public class PortletRequest implements Request
{
    private javax.portlet.PortletRequest portletRequest;
    private XWikiURL xwikiURL;

    public PortletRequest(javax.portlet.PortletRequest portletRequest)
    {
        this.portletRequest = portletRequest;
    }

    public javax.portlet.PortletRequest getPortletRequest()
    {
        return this.portletRequest;
    }

    public XWikiURL getURL()
    {
        return this.xwikiURL;
    }

    public void setXWikiURL(XWikiURL url)
    {
        this.xwikiURL = url;
    }

    @Override
    public Object getProperty(String key)
    {
        Object result;

        // Look first in the Query Parameters and then in the Query Attributes
        result = this.portletRequest.getParameter(key);
        if (result == null) {
            result = this.portletRequest.getAttribute(key);
        }

        return result;
    }

    @Override
    public List<Object> getProperties(String key)
    {
        List<Object> result = new ArrayList<Object>();

        // Look first in the Query Parameters and then in the Query Attributes
        result.addAll(Arrays.asList(this.portletRequest.getParameterValues(key)));
        result.add(this.portletRequest.getAttribute(key));

        return result;
    }

    @Override
    public void setProperty(String key, Object value)
    {
        this.portletRequest.setAttribute(key, value);
    }

    @Override
    public void removeProperty(String key)
    {
        this.portletRequest.removeAttribute(key);
    }
}
