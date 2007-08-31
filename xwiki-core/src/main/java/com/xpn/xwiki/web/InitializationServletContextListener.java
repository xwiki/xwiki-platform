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
 */
package com.xpn.xwiki.web;

import java.io.InputStream;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.view.ToolboxManager;
import org.apache.velocity.tools.view.XMLToolboxManager;
import org.apache.velocity.tools.view.servlet.ServletToolboxManager;
import org.apache.commons.collections.ExtendedProperties;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;

/**
 * XWiki's initialization. This is where we set up everything that should be initialized prior to
 * XWiki servicing requests. For example this is where we initialize Velocity.
 *
 * @version $Id: $
 */
public class InitializationServletContextListener implements ServletContextListener
{
    /**
     * Key used to access the toolbox configuration file path from the
     * Servlet or webapp init parameters ("org.apache.velocity.toolbox").
     */
    protected static final String TOOLBOX_KEY = "org.apache.velocity.toolbox";

    /**
     * Key used to access the velocity configuration file path from the
     * Servlet or webapp init parameters ("org.apache.velocity.properties").
     */
    protected static final String VELOCITY_PROPERTIES_KEY = "org.apache.velocity.properties";

    /**
     * Default toolbox configuration file path. If no alternate value for
     * this is specified, the servlet will look here.
     */
    protected static final String DEFAULT_TOOLBOX_PATH =
        "/WEB-INF/toolbox.xml";

    /**
     * {@inheritDoc}
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent event)
    {
        try {
            initializeVelocity(event.getServletContext());
            initializeVelocityTools(event.getServletContext());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize XWiki", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)  
     */
    public void contextDestroyed(ServletContextEvent event)
    {
        // Nothing to do
    }

    private void initializeVelocity(ServletContext servletContext) throws Exception
    {
        // There are 2 ways to use Velocity: using the singleton approach or using a VelocityEngine
        // instance (preferred). XWiki is currenty using the singleton approach so we need to
        // initialize the singleton here.
        // TODO: Once we have moved to a component approach, move to the VelocityEngine approach
        // which is cleaner and recommended.
        
        // Start by loading default Velocity properties. This is required so that the Velocity
        // Webapp Loader is configured. This allows specifying paths relative to the webapp dir
        // in our velocity.properties file.
        ExtendedProperties defaultProperties = new ExtendedProperties();
        defaultProperties.addProperty("resource.loader", "webapp");
        defaultProperties.addProperty("webapp.resource.loader.class",
            "org.apache.velocity.tools.view.servlet.WebappLoader");
        Velocity.setExtendedProperties(defaultProperties);

        // Note: The WebappLoader requires that the property named ServletContext.class.getName()
        // be set as an Application Attribute. It requires the Servlet Context to load webapp
        // resources
        Velocity.setApplicationAttribute(ServletContext.class.getName(), servletContext);

        // Get our custom Velocity configuration file location from context-param defined in web.xml
        String velocityPropertiesFile =
            servletContext.getInitParameter(VELOCITY_PROPERTIES_KEY);
        ExtendedProperties customProperties = new ExtendedProperties();
        customProperties.load(servletContext.getResourceAsStream(velocityPropertiesFile));
        Velocity.setExtendedProperties(customProperties);

        Velocity.init();
    }

    private void initializeVelocityTools(ServletContext servletContext) throws Exception
    {
        XMLToolboxManager toolboxManager;
        /* check the servlet config and context for a toolbox param */
        String toolboxConfigFile =
            servletContext.getInitParameter(TOOLBOX_KEY);
        if (toolboxConfigFile == null)
        {
            // ok, look in the default location
            toolboxConfigFile = DEFAULT_TOOLBOX_PATH;
        }

        /* try to get a manager for this toolbox file */
        toolboxManager = new XMLToolboxManager();
        toolboxManager.load(servletContext.getResourceAsStream(toolboxConfigFile));

        Velocity.setApplicationAttribute(ToolboxManager.class.getName(), toolboxManager);
    }
}
