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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * Default implementation for {@link VelocityContextFactory}.
 */
public class DefaultVelocityContextFactory extends AbstractLogEnabled
    implements VelocityContextFactory, Initializable
{
    /**
     * The Velocity tools coming from the component's configuration and injected
     * by the Component Manager.
     */
    private Properties properties;

    /**
     * The Velocity Tools (instantiated as Objects) that we use to create new
     * Velocity Contexts.
     */
    private Map<String, Object> tools;
    
    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.tools = new HashMap<String, Object>();
        
        // Instantiate Velocity tools
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
                this.tools.put(key, toolInstance);
                getLogger().debug("Setting tool [" + key + "] = [" + value + "]");
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * @see VelocityContextFactory#createContext()
     */
    public VelocityContext createContext()
    {
        return new VelocityContext(this.tools);
    }
}
