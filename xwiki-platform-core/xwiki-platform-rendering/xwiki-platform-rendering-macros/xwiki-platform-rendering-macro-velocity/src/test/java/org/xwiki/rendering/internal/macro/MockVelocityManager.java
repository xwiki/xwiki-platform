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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Mock VelocityManager implementation used for testing, since we don't want to pull any dependency on the
 * Model/Skin/etc for the Rendering module's unit tests.
 * 
 * @version $Id$
 * @since 1.5M2
 */
@Component
@Singleton
public class MockVelocityManager implements VelocityManager, Initializable
{
    @Inject
    private ComponentManager componentManager;

    /**
     * Note that we use a single Velocity Engine instance in this Mock.
     */
    @Inject
    private VelocityEngine velocityEngine;

    private VelocityContext velocityContext = new VelocityContext();

    @Override
    public void initialize() throws InitializationException
    {
        try {
            // Configure the Velocity Engine not to use the Resource Webapp Loader since we don't
            // need it and we would need to setup the Container component's ApplicationContext
            // otherwise.
            Properties properties = new Properties();
            properties.setProperty("resource.loader", "file");
            this.velocityEngine.initialize(properties);
        } catch (XWikiVelocityException e) {
            throw new InitializationException("Failed to initialize Velocity Engine", e);
        }
    }

    @Override
    public VelocityContext getVelocityContext()
    {
        return this.velocityContext;
    }

    @Override
    public VelocityEngine getVelocityEngine() throws XWikiVelocityException
    {
        return this.velocityEngine;
    }
}
