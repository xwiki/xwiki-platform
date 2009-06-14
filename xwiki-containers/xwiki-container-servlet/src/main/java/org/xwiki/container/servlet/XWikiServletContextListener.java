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
import javax.servlet.ServletContextListener;

import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.internal.StackingComponentEventManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;

public class XWikiServletContextListener implements ServletContextListener
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
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        // Initializes the Embeddable Component Manager 
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();
        ecm.initialize(this.getClass().getClassLoader());
        this.componentManager = ecm;

        // Use a Component Event Manager that stacks Component instance creation events till we tell it to flush them.
        // The reason is that the Observation Manager used to send the events is a component itself and we need the
        // Application Context to be set up before we start sending events since there can be Observation Listener
        // components that require the Application Context (this is the case for example for the Office Importer 
        // Lifecycle Listener).
        StackingComponentEventManager eventManager = new StackingComponentEventManager();
        ecm.setComponentEventManager(eventManager);
        
        // Initializes XWiki's Container with the Servlet Context.
        try {
            ServletContainerInitializer containerInitializer = 
                this.componentManager.lookup(ServletContainerInitializer.class);
            containerInitializer.initializeApplicationContext(servletContextEvent.getServletContext());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to initialize the Application Context", e);
        }

        // Send an Observation event to signal the XWiki application is started. This allows components who need to do
        // something on startup to do it.
        ObservationManager observationManager;
        try {
            observationManager = this.componentManager.lookup(ObservationManager.class);
            observationManager.notify(new ApplicationStartedEvent(), this);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to find the Observation Manager component", e);
        }

        // Now that the Application Context is set up, send the Component instance creation events we had stacked up.
        eventManager.setObservationManager(observationManager);
        eventManager.shouldStack(false);
        eventManager.flushEvents();

        // This is a temporary bridge to allow non XWiki components to lookup XWiki components.
        // We're putting the XWiki Component Manager instance in the Servlet Context so that it's
        // available in the XWikiAction class which in turn puts it into the XWikiContext instance.
        // Class that need to lookup then just need to get it from the XWikiContext instance.
        // This is of course not necessary for XWiki components since they just need to implement
        // the Composable interface to get access to the Component Manager or better they simply
        // need to define the Components they require as field members and configure the Plexus
        // deployment descriptors (components.xml) so that they are automatically injected.
        servletContextEvent.getServletContext().setAttribute(
            org.xwiki.component.manager.ComponentManager.class.getName(), this.componentManager);
    }

    /**
     * {@inheritDoc}
     */
    public void contextDestroyed(ServletContextEvent sce)
    {
        // Send an Observation event to signal the XWiki application is stopped. This allows components who need to do
        // something on stop to do it.
        try {
            ObservationManager observationManager = this.componentManager.lookup(ObservationManager.class);
            observationManager.notify(new ApplicationStoppedEvent(), this);
        } catch (ComponentLookupException e) {
            // Nothing to do here.
            // TODO: Log a warning
        }
        
        try {
            ApplicationContextListenerManager applicationContextListenerManager =
                (ApplicationContextListenerManager) this.componentManager
                    .lookup(ApplicationContextListenerManager.class);
            Container container = (Container) this.componentManager.lookup(Container.class);
            applicationContextListenerManager.destroyApplicationContext(container.getApplicationContext());
        } catch (ComponentLookupException ex) {
            // Nothing to do here.
            // TODO: Log a warning
        }
    }
}
