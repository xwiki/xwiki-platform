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

import javax.servlet.ServletContextEvent;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.servlet.PlexusServletContextListener;
import org.codehaus.plexus.servlet.PlexusServletUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.plexus.manager.PlexusComponentManagerFactory;

public class XWikiPlexusServletContextListener extends PlexusServletContextListener
{
    /**
     * The component manager used to lookup for other components.
     */
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.servlet.PlexusServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        super.contextInitialized(servletContextEvent);

        // Initializes XWiki's Component Manager using Plexus
        PlexusContainer plexusContainer =
            PlexusServletUtils.getPlexusContainer(servletContextEvent.getServletContext());
        PlexusComponentManagerFactory plexusFactory = new PlexusComponentManagerFactory();
        this.componentManager = plexusFactory.createComponentManager(plexusContainer);
        
        // Initializes XWiki's Container with the Servlet Context.
        try {
            ServletContainerInitializer containerInitializer = 
                this.componentManager.lookup(ServletContainerInitializer.class);
            containerInitializer.initializeApplicationContext(servletContextEvent.getServletContext());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to initialize application context", e);
        }

        // This is a temporary bridge to allow non XWiki components to lookup XWiki components.
        servletContextEvent.getServletContext().setAttribute(
            org.xwiki.component.manager.ComponentManager.class.getName(), this.componentManager);
    }

    /**
     * {@inheritDoc}
     */
    public void contextDestroyed(ServletContextEvent sce)
    {
        try {
            ApplicationContextListenerManager applicationContextListenerManager = 
                this.componentManager.lookup(ApplicationContextListenerManager.class);
            Container container = this.componentManager.lookup(Container.class);
            applicationContextListenerManager.destroyApplicationContext(container.getApplicationContext());
        } catch (ComponentLookupException e) {
            sce.getServletContext().log("Failed to clean up application context", e);
        }
        super.contextDestroyed(sce);
    }
}
