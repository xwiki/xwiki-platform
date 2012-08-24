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
import org.xwiki.script.ScriptContextManager;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;

import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

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

    /**
     * Used to get the current script context.
     */
    @Inject
    private ScriptContextManager scriptContextManager;

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
        // Copy current JSR223 ScriptContext binding
        for (Map.Entry<String, Object> entry : this.scriptContextManager.getScriptContext()
            .getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
            // Not ideal since it does not allow to modify a binding but it's too dangerous for existing velocity script
            // otherwise
            if (!this.velocityContext.containsKey(entry.getKey())) {
                this.velocityContext.put(entry.getKey(), entry.getValue());
            }
        }

        return this.velocityContext;
    }

    @Override
    public VelocityEngine getVelocityEngine() throws XWikiVelocityException
    {
        return this.velocityEngine;
    }
}
