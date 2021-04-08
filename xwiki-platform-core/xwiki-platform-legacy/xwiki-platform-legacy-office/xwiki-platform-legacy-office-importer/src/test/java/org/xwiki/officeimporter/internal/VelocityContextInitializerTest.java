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
package org.xwiki.officeimporter.internal;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.VelocityContextInitializer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Integration tests for various {@link VelocityContextInitializer} implementations specific to the Office Importer
 * module.
 * 
 * @version $Id$
 * @since 1.9RC2
 */
@ComponentTest
@AllComponents
class VelocityContextInitializerTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    /**
     * Test the presence of velocity bridges.
     * 
     * @throws Exception
     */
    @Test
    void velocityBridges() throws Exception
    {
        this.componentManager.registerMockComponent(ScriptService.class, "officeimporter");
        this.componentManager.registerMockComponent(ScriptService.class, "officemanager");
        ConfigurationSource source = this.componentManager.registerMockComponent(ConfigurationSource.class);
        when(source.getProperty("logging.deprecated.enabled", true)).thenReturn(true);

        // Make sure the execution context is not null when velocity bridges are initialized.
        this.componentManager.<Execution>getInstance(Execution.class).setContext(new ExecutionContext());

        VelocityContextFactory factory = this.componentManager.getInstance(VelocityContextFactory.class);
        VelocityContext context = factory.createContext();

        assertNotNull(context.get("officeimporter"));
        assertNotNull(context.get("ooconfig"));
        assertNotNull(context.get("oomanager"));
    }
}
