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
package org.xwiki.velocity;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletApplicationContext;

import java.util.Enumeration;
import java.util.Properties;

public class DefaultVelocityManager extends AbstractLogEnabled
    implements VelocityManager, Initializable, LogSystem
{
    private VelocityEngine engine;

    private Properties properties;

    private RuntimeServices runtimeServices;

    private Container container;
    
    public void initialize() throws InitializationException
    {
        this.engine = new VelocityEngine();

        // If the Velocity configuration uses the Velocity Tools
        // org.apache.velocity.tools.view.servlet.WebappLoader class then we need to set the
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

        if (this.properties != null) {
            for (Enumeration e = this.properties.propertyNames(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                String value = this.properties.getProperty(key);
                getEngine().setProperty(key, value);
                getLogger().debug("Setting property [" + key + "] = [" + value + "]");
            }
        }

        try {
            getEngine().init();
        }
        catch (Exception e) {
            throw new InitializationException("Cannot start the Velocity engine", e);
        }
    }

    public VelocityEngine getEngine()
    {
        return this.engine;
    }

    public void init(RuntimeServices runtimeServices)
    {
        this.runtimeServices = runtimeServices;
    }

    public void logVelocityMessage(int level, String message)
    {
        switch (level) {
            case LogSystem.WARN_ID:
                getLogger().warn(message);
                break;
            case LogSystem.INFO_ID:
                // Velocity info messages are too verbose, just consider them as debug messages...
                getLogger().debug(message);
                break;
            case LogSystem.DEBUG_ID:
                getLogger().debug(message);
                break;
            case LogSystem.ERROR_ID:
                getLogger().error(message);
                break;
            default:
                getLogger().debug(message);
                break;
        }
    }
}
