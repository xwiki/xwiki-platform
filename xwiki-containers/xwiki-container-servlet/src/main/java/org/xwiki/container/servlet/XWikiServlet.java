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

import org.xwiki.action.ActionException;
import org.xwiki.action.ActionManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * XWiki servlet implementation.
 * 
 * @version $Id$
 */
public class XWikiServlet extends HttpServlet
{
    /** Serial version ID. */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws ServletException, IOException
    {
        // Get the Component Manager instance set up in XWikiPlexusServletContextListener from the ServletContext
        ComponentManager componentManager =
            (ComponentManager) getServletContext().getAttribute(ComponentManager.class.getName());
        if (componentManager == null) {
            throw new ServletException("Plexus container is not initialized");
        }

        ActionManager manager;
        try {
            manager = componentManager.lookup(ActionManager.class);
        } catch (ComponentLookupException e) {
            // We cannot find the Action manager, not much we can do, abort...
            throw new ServletException("Failed to locate Action Manager component.", e);
        }

        // Initializes XWiki's Container with the Servlet request/response/session so that
        // components needing them can depend on the Container component to get them.
        try {
            ServletContainerInitializer containerInitializer =
                componentManager.lookup(ServletContainerInitializer.class);
            containerInitializer.initializeRequest(httpServletRequest);
            containerInitializer.initializeResponse(httpServletResponse);
            containerInitializer.initializeSession(httpServletRequest);
        } catch (Exception e) {
            try {
                // Call the error Action to handle the exception
                manager.handleRequest("error", e);
                return;
            } catch (ActionException ae) {
                throw new ServletException("Failed to call the error Action", ae);
            }
        }

        // Call the Action Manager to handle the request
        try {
            manager.handleRequest();
        } catch (ActionException e) {
            // We haven't been able to handle the exception in ActionManager so generate a
            // container exception.
            throw new ServletException("Failed to handle request", e);
        }
    }
}
