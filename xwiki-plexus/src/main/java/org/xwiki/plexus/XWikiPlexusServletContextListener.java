/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import org.codehaus.plexus.servlet.PlexusServletContextListener;
import org.codehaus.plexus.servlet.PlexusServletUtils;
import org.xwiki.action.ActionException;
import org.xwiki.action.ActionManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainer;
import org.xwiki.container.servlet.ServletContainerException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

public class XWikiPlexusServletContextListener extends PlexusServletContextListener
{
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        // Initializes Plexus
        super.contextInitialized(servletContextEvent);

        // Initializes XWiki's Container with the Servlet Context
        ServletContainer containerManager = null;
        try {
            containerManager = (ServletContainer) PlexusServletUtils.lookup(
                servletContextEvent.getServletContext(), Container.ROLE, "servlet");
            containerManager.initialize(servletContextEvent.getServletContext());
        } catch (ServletException se) {
            throw new RuntimeException("Failed to lookup component role [" + Container.ROLE
                + "] for hint [servlet]", se);
        } catch (ServletContainerException sce) {
            ActionManager manager = lookupActionManager(servletContextEvent.getServletContext());
            try {
                manager.handleRequest(containerManager, "error", sce);
            } catch (ActionException ae) {
                throw new RuntimeException("Failed to call the error Action", ae);
            }
        }
    }

    private ActionManager lookupActionManager(ServletContext servletContext)
    {
        ActionManager manager;
        try {
            manager = (ActionManager) PlexusServletUtils.lookup(servletContext, ActionManager.ROLE);
        } catch (ServletException e) {
            throw new RuntimeException("Failed to lookup component role [" + ActionManager.ROLE
                + "]", e);
        }
        return manager;
    }
}
