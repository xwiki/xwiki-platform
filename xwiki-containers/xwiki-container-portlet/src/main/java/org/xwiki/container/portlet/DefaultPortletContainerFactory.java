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
 *
 */
package org.xwiki.container.portlet;

import javax.portlet.PortletContext;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializerManager;
import org.xwiki.container.Response;
import org.xwiki.container.Session;

public class DefaultPortletContainerFactory implements PortletContainerFactory
{
    private RequestInitializerManager requestInitializerManager;
    
    public ApplicationContext createApplicationContext(PortletContext portletContext)
    {
        return new PortletApplicationContext(portletContext);
    }

    public Request createRequest(javax.portlet.PortletRequest portletRequest)
        throws PortletContainerException
    {
        PortletRequest request = new PortletRequest(portletRequest);

        // There's no Request URL when in Portlet mode since the request is handled by the Portal
        // itself.
        // TODO: Fix this. I think we need to move the fields in XWikiURL to the Request object somehow (action, document name, space name, etc)
        request.setXWikiURL(null);

        try {
            this.requestInitializerManager.initializeRequest(request);
        } catch (ComponentLookupException e) {
            throw new PortletContainerException("Failed to initialize request", e);
        }

        return request;        
    }

    public Response createResponse(javax.portlet.PortletResponse portletResponse)
    {
        return new PortletResponse(portletResponse);
    }

    public Session createSession(javax.portlet.PortletRequest portletRequest)
    {
        return new PortletSession(portletRequest);
    }

}
