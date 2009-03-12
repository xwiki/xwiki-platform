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

import org.codehaus.plexus.servlet.PlexusServletContextListener;
import org.codehaus.plexus.servlet.PlexusServletUtils;
import org.codehaus.plexus.PlexusContainer;
import org.xwiki.plexus.manager.PlexusComponentManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

public class XWikiPlexusServletContextListener extends PlexusServletContextListener
{
    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.servlet.PlexusServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        // Initializes Plexus
        super.contextInitialized(servletContextEvent);

        // Initializes XWiki's Container with the Servlet Context.
        try {
            ServletContainerInitializer containerInitializer =
                (ServletContainerInitializer) PlexusServletUtils.lookup(servletContextEvent.getServletContext(),
                    ServletContainerInitializer.ROLE);
            containerInitializer.initializeApplicationContext(servletContextEvent.getServletContext());
        } catch (ServletException se) {
            throw new RuntimeException("Failed to initialize application contextt", se);
        }

        // This is a temporary bridge to allow non XWiki components to lookup XWiki components.
        // We're putting the XWiki Component Manager instance in the Servlet Context so that it's
        // available in the XWikiAction class which in turn puts it into the XWikiContext instance.
        // Class that need to lookup then just need to get it from the XWikiContext instance.
        // This is of course not necessary for XWiki components since they just need to implement
        // the Composable interface to get access to the Component Manager or better they simply
        // need to define the Components they require as field members and configure the Plexus
        // deployment descriptors (components.xml) so that they are automatically injected.
        PlexusContainer plexusContainer =
            PlexusServletUtils.getPlexusContainer(servletContextEvent.getServletContext());
        org.xwiki.component.manager.ComponentManager xwikiManager = new PlexusComponentManager(plexusContainer);
        servletContextEvent.getServletContext().setAttribute(
            org.xwiki.component.manager.ComponentManager.class.getName(), xwikiManager);
    }
}
