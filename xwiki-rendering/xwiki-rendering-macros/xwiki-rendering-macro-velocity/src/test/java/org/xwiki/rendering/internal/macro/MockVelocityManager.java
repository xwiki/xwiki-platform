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
package org.xwiki.rendering.internal.macro;

import org.apache.velocity.VelocityContext;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentLookupException;

import java.util.Properties;

/**
 * Mock VelocityManager implementation used for testing, since we don't want to pull any dependency on the
 * Model/Skin/etc for the Rendering module's unit tests.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class MockVelocityManager implements VelocityManager, Composable
{
    private ComponentManager componentManager;

    private VelocityContext velocityContext = new VelocityContext();

    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor getComponentDescriptor()
    {
        DefaultComponentDescriptor componentDescriptor = new DefaultComponentDescriptor();

        componentDescriptor.setRole(VelocityManager.class);
        componentDescriptor.setImplementation(MockVelocityManager.class.getName());

        return componentDescriptor;
    }

    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public VelocityContext getVelocityContext()
    {
        return this.velocityContext;
    }

    public VelocityEngine getVelocityEngine() throws XWikiVelocityException
    {
        VelocityEngine engine;
        try {
            engine = this.componentManager.lookup(VelocityEngine.class);
        } catch (ComponentLookupException e) {
            throw new XWikiVelocityException("Failed to look up Velocity Engine", e);
        }

        // Configure the Velocity Engine not to use the Resource Webapp Loader since we don't
        // need it and we would need to setup the Container component's ApplicationContext
        // otherwise.
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "file");
        engine.initialize(properties);

        return engine;
    }
}
