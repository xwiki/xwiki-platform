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
 */package org.xwiki.velocity;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.velocity.context.Context;
import org.xwiki.component.logging.Logger;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.phase.LogEnabled;

/**
 * Default implementation for {@link VelocityContext}.
 */
public class DefaultVelocityContext extends org.apache.velocity.VelocityContext 
	implements VelocityContext, Initializable, Context, LogEnabled
{
    private Logger logger;

	private Properties properties;

	/**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
	public void initialize() throws InitializationException
	{
        // Configure Velocity tools
        if (this.properties != null) {
            for (Enumeration props = this.properties.propertyNames(); props.hasMoreElements();) {
                String key = props.nextElement().toString();
                String value = this.properties.getProperty(key);
                Object toolInstance;
                try {
                    toolInstance = Class.forName(value).newInstance();
                } catch (Exception e) {
                    throw new InitializationException("Failed to initialize tool [" 
                        + value + "]", e);
                }
                put(key, toolInstance);
                getLogger().debug("Setting tool [" + key + "] = [" + value + "]");
            }
        }
	}

	/**
     * {@inheritDoc}
     * @see LogEnabled#enableLogging(Logger)
     */
	public void enableLogging(Logger logger)
	{
        this.logger = logger;
	}
	
	/**
     * @return the logger to use for logging
     */
    private Logger getLogger()
    {
        return logger;
    }
}
