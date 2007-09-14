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
package org.xwiki.plexus;

import org.codehaus.plexus.servlet.PlexusServlet;
import org.xwiki.action.ActionException;
import org.xwiki.action.ActionManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainer;
import org.xwiki.container.servlet.ServletContainerException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class XWikiPlexusServlet extends PlexusServlet
{
    protected void service(HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        ActionManager manager = (ActionManager) lookup(ActionManager.ROLE);

        // Initializes XWiki's Container with the Servlet request/response/session so that
        // components needing them can depend on the Container Manager component to get them.
        ServletContainer containerManager =
            (ServletContainer) lookup(Container.ROLE, "servlet");
        try {
            containerManager.initialize(httpServletRequest, httpServletResponse);
        } catch (ServletContainerException e) {
            try {
                // Call the error Action to handle the exception
                manager.handleRequest(containerManager, "error", e);
                return;
            } catch (ActionException ae) {
                throw new ServletException("Failed to call the error Action", ae);
            }
        }

        // Call the Action Manager to handle the request
        try {
            manager.handleRequest(containerManager);
        } catch (ActionException e) {
            // We haven't been able to handle the exception in ActionManager so generate a
            // container exception.
            throw new ServletException("Failed to handle request ["
                + containerManager.getRequest() + "]", e);
        }
    }
}
