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

import java.io.IOException;
import java.io.OutputStream;

import javax.portlet.RenderResponse;

import org.xwiki.container.Response;

public class PortletResponse implements Response
{
    private javax.portlet.PortletResponse portletResponse;

    public PortletResponse(javax.portlet.PortletResponse portletResponse)
    {
        this.portletResponse = portletResponse;
    }

    public javax.portlet.PortletResponse getPortletResponse()
    {
        return this.portletResponse;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        if (this.portletResponse instanceof RenderResponse) {
            return ((RenderResponse) this.portletResponse).getPortletOutputStream();
        }
        return null;
    }

    @Override
    public void setContentLength(int length)
    {
        // No content length for portlets, do nothing
    }

    @Override
    public void setContentType(String mimeType)
    {
        if (this.portletResponse instanceof RenderResponse) {
            ((RenderResponse) this.portletResponse).setContentType(mimeType);
        }
    }
}
