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
package org.xwiki.velocity.internal;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.VelocityContextInitializer;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Default implementation for {@link VelocityContextFactory}.
 * 
 * @version $Id$
 */
@Component
public class DefaultVelocityContextFactory extends AbstractLogEnabled implements VelocityContextFactory, Initializable
{
    /**
     * The component manager we used to find all components implementing the
     * {@link org.xwiki.velocity.VelocityContextInitializer} role.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * Velocity configuration to get the list of configured Velocity tools.
     */
    @Requirement
    private VelocityConfiguration velocityConfiguration;

    /**
     * An internal read-only Velocity Context containing the Tools defined in the component's configuration. We reuse
     * them across Contexts for better performance.
     */
    private Context toolsContext;

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.toolsContext = new VelocityContext();

        // Instantiate Velocity tools
        Properties properties = this.velocityConfiguration.getTools();
        if (properties != null) {
            for (Enumeration< ? > props = properties.propertyNames(); props.hasMoreElements();) {
                String key = props.nextElement().toString();
                String value = properties.getProperty(key);
                Object toolInstance;
                try {
                    toolInstance = Class.forName(value).newInstance();
                } catch (Exception e) {
                    throw new InitializationException("Failed to initialize tool [" + value + "]", e);
                }
                this.toolsContext.put(key, toolInstance);
                getLogger().debug("Setting tool [" + key + "] = [" + value + "]");
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see VelocityContextFactory#createContext()
     */
    public VelocityContext createContext() throws XWikiVelocityException
    {
        // Note: This constructor uses the passed context as an internal read-only context.
        VelocityContext context = new VelocityContext(this.toolsContext);

        // Call all components implementing the VelocityContextInitializer's role.
        try {
            for (Object interceptor : this.componentManager.lookupList(VelocityContextInitializer.class)) {
                ((VelocityContextInitializer) interceptor).initialize(context);
            }
        } catch (ComponentLookupException e) {
            throw new XWikiVelocityException("Failed to locate some Velocity Context initializers", e);
        }

        return context;
    }
}
