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
package org.xwiki.container.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.container.RequestInitializerManager;
import org.xwiki.container.Container;

public class DefaultServletContainerInitializer implements ServletContainerInitializer
{
    private RequestInitializerManager requestInitializerManager;

    private Container container;

    public void initializeApplicationContext(ServletContext servletContext)
    {
        this.container.setApplicationContext(new ServletApplicationContext(servletContext));
    }

    public void initializeRequest(HttpServletRequest httpServletRequest)
        throws ServletContainerException
    {
        // 1) Create an empty request. From this point forward request initializers can use the
        // Container object to get any data they want from the Request.
        this.container.setRequest(new ServletRequest(httpServletRequest));

        // 2) Call the request initializers to populate the Request.
        try {
            this.requestInitializerManager.initializeRequest(this.container.getRequest());
        } catch (ComponentLookupException e) {
            throw new ServletContainerException("Failed to initialize request", e);
        }
    }

    public void initializeResponse(HttpServletResponse httpServletResponse)
    {
        this.container.setResponse(new ServletResponse(httpServletResponse));
    }

    public void initializeSession(HttpServletRequest httpServletRequest)
    {
        this.container.setSession(new ServletSession(httpServletRequest));
    }
}
