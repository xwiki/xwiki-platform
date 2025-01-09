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
package org.xwiki.container.servlet;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.internal.StackingComponentEventManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.internal.HttpSessionManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.extension.handler.ExtensionInitializer;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;

/**
 * Implementation of the {@link ServletContextListener}. Initializes component manager and application context.
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely different
 * API from Java point of view.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class XWikiServletContextListener implements ServletContextListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiServletContextListener.class);

    private static final String TOMCAT_CATALINA = "Catalina";

    /**
     * Logger to use to log shutdown information (opposite of initialization).
     */
    private static final Logger SHUTDOWN_LOGGER = LoggerFactory.getLogger("org.xwiki.shutdown");

    /** The component manager used to lookup other components. */
    private EmbeddableComponentManager componentManager;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent)
    {
        // Initializes the Embeddable Component Manager
        EmbeddableComponentManager ecm = new EmbeddableComponentManager();

        // Initialize all the components. Note that this can fail with a Runtime Exception. This is done voluntarily so
        // that the XWiki webapp will not be available if one component fails to load. It's better to fail-fast.
        ecm.initialize(this.getClass().getClassLoader());
        this.componentManager = ecm;

        // We're putting the XWiki Component Manager instance in the Servlet Context so that it's
        // available in Servlets and Filters.
        servletContextEvent.getServletContext()
            .setAttribute(org.xwiki.component.manager.ComponentManager.class.getName(), this.componentManager);

        // Use a Component Event Manager that stacks Component instance creation events till we tell it to flush them.
        // The reason is that the Observation Manager used to send the events but we need the Application Context to
        // be set up before we start sending events since there can be Observation Listener components that require
        // the Application Context (this is the case for example for the Office Importer Lifecycle Listener).
        StackingComponentEventManager eventManager = new StackingComponentEventManager();
        this.componentManager.setComponentEventManager(eventManager);

        // Initialize the Environment
        try {
            ServletEnvironment servletEnvironment = this.componentManager.getInstance(Environment.class);
            servletEnvironment.setServletContext(servletContextEvent.getServletContext());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to initialize the Servlet Environment", e);
        }

        // Initializes the Application Context.
        // Even though the notion of ApplicationContext has been deprecated in favor of the notion of Environment we
        // still keep this initialization for backward-compatibility.
        // TODO: Add an Observation Even that we send when the Environment is initialized so that we can move the code
        // below in an Event Listener and move it to the legacy module.
        try {
            ServletContainerInitializer containerInitializer =
                this.componentManager.getInstance(ServletContainerInitializer.class);
            containerInitializer
                .initializeApplicationContext(JakartaServletBridge.toJavax(servletContextEvent.getServletContext()));
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to initialize the Application Context", e);
        }

        // Send an Observation event to signal the XWiki application is started. This allows components who need to do
        // something on startup to do it.
        ObservationManager observationManager;
        try {
            observationManager = this.componentManager.getInstance(ObservationManager.class);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to find the Observation Manager component", e);
        }

        // Make sure installed extensions are initialized before sending ApplicationStartedEvent
        try {
            this.componentManager.getInstance(ExtensionInitializer.class);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to initialize installed extensions", e);
        }

        // Register the HttpSessionManager as a listener.
        try {
            HttpSessionManager httpSessionManager = this.componentManager.getInstance(HttpSessionManager.class);
            servletContextEvent.getServletContext().addListener(httpSessionManager);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to initialize HttpSessionManager", e);
        }

        // Now that the Application Context is set up and the component are registered, send the Component instance
        // creation events we had stacked up.
        eventManager.setObservationManager(observationManager);
        eventManager.shouldStack(false);
        eventManager.flushEvents();

        // Force allowing any character in the input URLs, contrary to what Servlet 6 specifications indicate
        // FIXME: Remove when https://jira.xwiki.org/browse/XWIKI-19167 is fully fixed
        allowAllURLCharacters(servletContextEvent.getServletContext());

        // Indicate to the various components that XWiki is ready
        observationManager.notify(new ApplicationStartedEvent(), this);
    }

    private void allowAllURLCharacters(ServletContext servletContext)
    {
        if (isForceAllowAnyCharacter()) {
            // Tomcat
            allowAllURLCharactersTomcat();

            // Jetty
            allowAllURLCharactersJetty(servletContext);
        }
    }

    private boolean isForceAllowAnyCharacter()
    {
        ConfigurationSource configuration;
        try {
            configuration = this.componentManager.getInstance(ConfigurationSource.class, "xwikiproperties");

            return configuration.getProperty("url.forceAllowAnyCharacter", true);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to access the configuration", e);
        }
    }

    private void allowAllURLCharactersTomcat()
    {
        try {
            ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer(null);
            if (!mbeanServers.isEmpty()) {
                MBeanServer mBeanServer = mbeanServers.get(0);
                ObjectName name = new ObjectName(TOMCAT_CATALINA, "type", "Server");
                Object server = mBeanServer.getAttribute(name, "managedResource");
                Object service = MethodUtils.invokeMethod(server, "findService", TOMCAT_CATALINA);
                Object connectors = MethodUtils.invokeMethod(service, "findConnectors");
                for (int i = 0; i < Array.getLength(connectors); ++i) {
                    Object connector = Array.get(connectors, i);

                    // Allow backslash (\)
                    MethodUtils.invokeMethod(connector, "setAllowBackslash", true);
                    // Allow slash/solidus (/)
                    MethodUtils.invokeMethod(connector, "setEncodedSolidusHandling", "passthrough");
                    // Try as much as possible to have XWiki being called directly for any URI
                    MethodUtils.invokeMethod(connector, "setRejectSuspiciousURIs", false);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to configure Tomcat URL constraints", e);
        }
    }

    private void allowAllURLCharactersJetty(ServletContext servletContext)
    {
        try {
            Object contextHandler = MethodUtils.invokeMethod(servletContext, "getContextHandler");

            // setDecodeAmbiguousURIs
            Object servletHandler = MethodUtils.invokeMethod(contextHandler, "getServletHandler");
            MethodUtils.invokeMethod(servletHandler, "setDecodeAmbiguousURIs", true);

            // URI compliance
            Object server = MethodUtils.invokeMethod(contextHandler, "getServer");
            Object connectors = MethodUtils.invokeMethod(server, "getConnectors");
            Object uriCompliance = null;
            for (int i = 0; i < Array.getLength(connectors); ++i) {
                uriCompliance = configureConnectorJetty(Array.get(connectors, i), uriCompliance);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to configure Jetty URL constraints", e);
        }
    }

    private Object configureConnectorJetty(Object connector, Object inputUriCompliance)
    {
        Object uriCompliance = inputUriCompliance;

        Object factories;
        try {
            factories = MethodUtils.invokeMethod(connector, "getConnectionFactories");
            if (factories instanceof Collection factoriesCollection) {
                for (Object factory : factoriesCollection) {
                    uriCompliance = configureFactoryJetty(factory, uriCompliance);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get factories", e);
        }

        return uriCompliance;
    }

    private Object configureFactoryJetty(Object factory, Object inputUriCompliance)
    {
        Object uriCompliance = inputUriCompliance;

        try {
            Object httpConfiguration = MethodUtils.invokeMethod(factory, "getHttpConfiguration");

            if (uriCompliance == null) {
                uriCompliance = getUriCompliance(httpConfiguration);
            }

            MethodUtils.invokeMethod(httpConfiguration, "setUriCompliance", uriCompliance);
        } catch (Exception e) {
            LOGGER.debug("Failed to set the URI compliance", e);
        }

        return uriCompliance;
    }

    private Object getUriCompliance(Object httpConfiguration)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        // Create a UriCompliance with the right class (we cannot use the one in the current classloader since it's
        // coming from XWiki WAR)

        Class<?> uriComplianceClass = MethodUtils.invokeMethod(httpConfiguration, "getUriCompliance").getClass();
        Class violationsClass = getViolationClass(uriComplianceClass);
        Set<?> violations = Set.of(Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_SEGMENT"),
            Enum.valueOf(violationsClass, "AMBIGUOUS_EMPTY_SEGMENT"),
            Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_SEPARATOR"),
            Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_PARAMETER"),
            Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_ENCODING"));

        return ConstructorUtils.invokeConstructor(uriComplianceClass, "XWiki", violations);
    }

    private Class getViolationClass(Class<?> uriComplianceClass)
    {
        for (Class<?> innerClass : uriComplianceClass.getDeclaredClasses()) {
            // We cannot manipulate the class directly because it would probably be the wrong one, so we use reflection
            @SuppressWarnings("java:S1872")
            boolean isViolationClass = innerClass.getName().equals("org.eclipse.jetty.http.UriCompliance$Violation");
            if (isViolationClass) {
                return innerClass;
            }
        }

        return null;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        SHUTDOWN_LOGGER.debug("Stopping XWiki...");

        // It's possible that the Component Manager failed to initialize some of the required components.
        if (this.componentManager != null) {
            // Send an Observation event to signal the XWiki application is stopped. This allows components who need
            // to do something on stop to do it.
            try {
                ObservationManager observationManager = this.componentManager.getInstance(ObservationManager.class);
                observationManager.notify(new ApplicationStoppedEvent(), this);
            } catch (ComponentLookupException e) {
                // Nothing to do here.
                // TODO: Log a warning
            }

            // Even though the notion of ApplicationContext has been deprecated in favor of the notion of Environment we
            // still keep this destruction for backward-compatibility.
            // TODO: Add an Observation Even that we send when the Environment is destroyed so that we can move the code
            // below in an Event Listener and move it to the legacy module.
            try {
                ApplicationContextListenerManager applicationContextListenerManager =
                    this.componentManager.getInstance(ApplicationContextListenerManager.class);
                Container container = this.componentManager.getInstance(Container.class);
                applicationContextListenerManager.destroyApplicationContext(container.getApplicationContext());
            } catch (ComponentLookupException ex) {
                // Nothing to do here.
                // TODO: Log a warning
            }

            // Make sure to dispose all components before leaving
            this.componentManager.dispose();
        }

        SHUTDOWN_LOGGER.debug("XWiki has been stopped!");
    }
}
