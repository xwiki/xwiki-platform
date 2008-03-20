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
package org.xwiki.velocity;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletApplicationContext;

import java.util.Enumeration;
import java.util.Properties;
import java.io.Writer;
import java.io.Reader;

/**
 * Default implementation of the Velocity service which initializes the Velocity system using
 * configuration values defined in the component's configuration. Note that the {@link #initialize}
 * method has to be executed before any other method can be called.
 */
public class DefaultVelocityManager extends AbstractLogEnabled
    implements VelocityManager, LogChute
{
    private VelocityEngine engine;

    private Properties properties;

    private Container container;

    /**
     * {@inheritDoc}
     * @see VelocityManager#initialize(Properties)
     */
    public void initialize(Properties properties) throws XWikiVelocityException
    {
        this.engine = new VelocityEngine();

        // If the Velocity configuration uses the Velocity Tools
        // <code>org.apache.velocity.tools.view.servlet.WebappLoader</code> class then we need to set the
        // ServletContext object as a Velocity Application Attribute as it's used to load resources
        // from the webapp directory in WebapLoader.
        ApplicationContext context = this.container.getApplicationContext();
        if (context instanceof ServletApplicationContext) {
            getEngine().setApplicationAttribute("javax.servlet.ServletContext",
                ((ServletApplicationContext) context).getServletContext());
        }

        // Avoid "unable to find resource 'VM_global_library.vm' in any resource loader."
        getEngine().setProperty("velocimacro.library", "");

        getEngine().setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, this);

        // Configure Velocity by passing the properties defined in this component's configuration
        if (this.properties != null) {
            for (Enumeration<?> e = this.properties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                // Only set a property if it's not overridden by one of the passed properties
                if (!properties.containsKey(key)) {
	                String value = this.properties.getProperty(key);
	                getEngine().setProperty(key, value);
	                getLogger().debug("Setting property [" + key + "] = [" + value + "]");
                }
            }
        }

        // Override the component's static properties with the ones passed in parameter
        if (properties != null) {
            for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                String value = properties.getProperty(key);
                getEngine().setProperty(key, value);
                getLogger().debug("Overriding property [" + key + "] = [" + value + "]");
            }
        }
        
        try {
            getEngine().init();
        }
        catch (Exception e) {
            throw new XWikiVelocityException("Cannot start the Velocity engine", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see VelocityManager#evaluate(Context, java.io.Writer, String, String)
     */
    public boolean evaluate(Context context, Writer out, String templateName,
        String source) throws XWikiVelocityException
    {
        try {
            return getEngine().evaluate(context, out, templateName, source);
        } catch (Exception e) {
            throw new XWikiVelocityException("Failed to evaluate content with id [" + templateName
                + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * @see VelocityManager#evaluate(Context, java.io.Writer, String, String)
     */
    public boolean evaluate(Context context, Writer out, String templateName,
        Reader source) throws XWikiVelocityException
    {
        try {
            return getEngine().evaluate(context, out, templateName, source);
        } catch (Exception e) {
            throw new XWikiVelocityException("Failed to evaluate content with id [" + templateName
                + "]", e);
        }
    }

    /**
     * @return the initialized Velocity engine which can be used to call all Velocity services
     */
    private VelocityEngine getEngine()
    {
        return this.engine;
    }

    /**
     * {@inheritDoc}
     * @see LogChute#init(org.apache.velocity.runtime.RuntimeServices)   
     */
    public void init(RuntimeServices runtimeServices)
    {
        // Do nothing.
    }

    /**
     * {@inheritDoc}
     * @see LogChute#log(int, String) 
     */
    public void log(int level, String message)
    {
        switch (level) {
            case LogChute.WARN_ID:
                getLogger().warn(message);
                break;
            case LogChute.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                getLogger().debug(message);
                break;
            case LogChute.DEBUG_ID:
                getLogger().debug(message);
                break;
            case LogChute.ERROR_ID:
                getLogger().error(message);
                break;
            default:
                getLogger().debug(message);
                break;
        }
    }

    /**
     * {@inheritDoc}
     * @see LogChute#log(int, String, Throwable)
     */
    public void log(int level, String message, Throwable throwable)
    {
        switch (level) {
            case LogChute.WARN_ID:
                getLogger().warn(message, throwable);
                break;
            case LogChute.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                getLogger().debug(message, throwable);
                break;
            case LogChute.DEBUG_ID:
                getLogger().debug(message, throwable);
                break;
            case LogChute.ERROR_ID:
                getLogger().error(message, throwable);
                break;
            default:
                getLogger().debug(message, throwable);
                break;
        }
    }

    /**
     * {@inheritDoc}
     * @see LogChute#isLevelEnabled(int) 
     */
    public boolean isLevelEnabled(int level)
    {
        boolean isEnabled;

        switch (level) {
            case LogChute.WARN_ID:
                isEnabled = getLogger().isWarnEnabled();
                break;
            case LogChute.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                isEnabled = getLogger().isDebugEnabled();
                break;
            case LogChute.DEBUG_ID:
                isEnabled = getLogger().isDebugEnabled();
                break;
            case LogChute.ERROR_ID:
                isEnabled = getLogger().isErrorEnabled();
                break;
            default:
                isEnabled = getLogger().isDebugEnabled();
                break;
        }

        return isEnabled;
    }
}
