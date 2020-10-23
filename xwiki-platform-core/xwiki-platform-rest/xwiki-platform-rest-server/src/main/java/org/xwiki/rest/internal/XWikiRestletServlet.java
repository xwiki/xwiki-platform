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
package org.xwiki.rest.internal;

import java.io.InputStream;
import java.util.logging.LogManager;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.ext.jaxrs.JaxRsApplication;
import org.restlet.ext.servlet.ServerServlet;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * <p>
 * The XWiki Restlet servlet is used to provide additional initialization logic to the base Restlet servlet. This
 * servlet does three things:
 * </p>
 * <ul>
 * <li>Creates the Restlet application.</li>
 * <li>Initialize the logging system by reading the configuration from well-defined locations.</li>
 * <li>Injects the component manager in the Restlet application context so that it will be accessible by all the other
 * Restlet components.</li>
 * <li>Set the object factory for the JAX-RS application to a factory that will use the component manager in order to
 * create instances (this will allow us to declare JAX-RS resources as XWiki components)</li>
 * </ul>
 * 
 * @version $Id$
 */
public class XWikiRestletServlet extends ServerServlet
{
    private static final String JAVA_LOGGING_PROPERTY_FILE = "java-logging.properties";

    private static final long serialVersionUID = 9148448182654390153L;

    @Override
    protected Application createApplication(Context context)
    {
        Context applicationContext = context.createChildContext();

        /* Retrieve the component manager and make it available in the restlet application context. */
        ComponentManager componentManager = getComponentManager();
        applicationContext.getAttributes().put(Constants.XWIKI_COMPONENT_MANAGER, componentManager);

        JaxRsApplication application;
        try {
            application = componentManager.getInstance(JaxRsApplication.class);
        } catch (ComponentLookupException e) {
            log("Failed to lookup default JAX-RS Application", e);

            return null;
        }
        application.setContext(applicationContext);

        // Make the servlet available
        try {
            JaxRsServletProvider applicationProvider = componentManager.getInstance(JaxRsServletProvider.class);
            applicationProvider.setApplication(this);
        } catch (ComponentLookupException e) {
            log("Failed to lookup JaxRsApplicationProvider. Dyncamically added/removed resources won'tbe supported", e);
        }

        return application;
    }

    @Override
    public void destroy()
    {
        super.destroy();

        // FIXME: not super nice but destroy() is pretty much useless and Restlet is really not designed in a dynamic
        // way...
        try {
            FieldUtils.writeField(this, "application", null, true);
            FieldUtils.writeField(this, "component", null, true);
            FieldUtils.writeField(this, "helper", null, true);
            getServletContext().removeAttribute("org.restlet.ext.servlet.ServerServlet.server.RestletServlet");
            getServletContext().removeAttribute("org.restlet.ext.servlet.ServerServlet.application.RestletServlet");
            getServletContext().removeAttribute("org.restlet.ext.servlet.ServerServlet.component.RestletServlet");
        } catch (IllegalAccessException e) {
            log("Failed to destroy Restlet servlet", e);
        }
    }

    @Override
    public void init() throws ServletException
    {
        super.init();

        try {
            // Try first in WEB-INF
            InputStream is =
                getServletContext().getResourceAsStream(String.format("/WEB-INF/%s", JAVA_LOGGING_PROPERTY_FILE));

            // If nothing is there then try in the current jar
            if (is == null) {
                is = getClass().getClassLoader().getResourceAsStream(JAVA_LOGGING_PROPERTY_FILE);
            }

            if (is != null) {
                try {
                    LogManager.getLogManager().readConfiguration(is);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (Exception e) {
            log("Unable to initialize Java logging framework. Using defaults", e);
        }
    }

    /**
     * Finds the correct Component Manager to use to find REST Resource components. This is important so that components
     * registered in a children Component Manager are found (for example a REST Resource Component added in a subwiki).
     *
     * @return the Context Component Manager or if it doesn't exist the Root Component Manager
     */
    private ComponentManager getComponentManager()
    {
        ComponentManager result =
            (ComponentManager) getServletContext().getAttribute("org.xwiki.component.manager.ComponentManager");
        try {
            result = result.getInstance(ComponentManager.class, "context");
        } catch (ComponentLookupException e) {
            // Return the root CM since there's no Context CM!
        }
        return result;
    }
}
