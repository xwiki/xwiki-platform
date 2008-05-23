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

import org.xwiki.container.RequestInitializerManager;
import org.xwiki.container.Container;
import org.xwiki.container.RequestInitializerException;

public class DefaultPortletContainerInitializer implements PortletContainerInitializer
{
    private RequestInitializerManager requestInitializerManager;

    private Container container;

    public void initializeApplicationContext(PortletContext portletContext)
    {
        this.container.setApplicationContext(new PortletApplicationContext(portletContext));
    }

    public void initializeRequest(javax.portlet.PortletRequest portletRequest, Object xwikiContext)
        throws PortletContainerException
    {
        // 1) Create an empty request. From this point forward request initializers can use the
        // Container object to get any data they want from the Request.
        this.container.setRequest(new PortletRequest(portletRequest));

        // 2) Bridge with old code to play well with new components. Old code relies on the
        // XWikiContext object whereas new code uses the Container component.
        this.container.getRequest().setProperty("xwikicontext", xwikiContext);

        // 3) Call the request initializers to populate the Request.
        try {
            this.requestInitializerManager.initializeRequest(this.container.getRequest());
        } catch (RequestInitializerException e) {
            throw new PortletContainerException("Failed to initialize request", e);
        }
    }

    public void initializeResponse(javax.portlet.PortletResponse portletResponse)
    {
        this.container.setResponse(new PortletResponse(portletResponse));
    }

    public void initializeSession(javax.portlet.PortletRequest portletRequest)
    {
        this.container.setSession(new PortletSession(portletRequest));
    }
}
