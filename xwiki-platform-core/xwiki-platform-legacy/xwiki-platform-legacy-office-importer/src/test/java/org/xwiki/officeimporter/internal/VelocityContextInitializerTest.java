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

import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Test class for various {@link VelocityContextInitializer} implementations.
 * 
 * @version $Id$
 * @since 1.9RC2
 */
public class VelocityContextInitializerTest extends AbstractMockingComponentTestCase
{
    /**
     * Test the presence of velocity bridges.
     * 
     * @throws Exception
     */
    @Test
    public void testVelocityBridges() throws Exception
    {
        registerMockComponent(ScriptService.class, "officeimporter", "importer");
        registerMockComponent(ScriptService.class, "officemanager", "manager");
        final ConfigurationSource configurationSource = registerMockComponent(ConfigurationSource.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(configurationSource).getProperty("velocity.tools", Properties.class);
                will(returnValue(new Properties()));
            }
        });

        // Make sure the execution context is not null when velocity bridges are initialized.
        getComponentManager().<Execution> getInstance(Execution.class).setContext(new ExecutionContext());

        VelocityContextFactory factory = getComponentManager().getInstance(VelocityContextFactory.class);
        VelocityContext context = factory.createContext();

        Assert.assertNotNull(context.get("officeimporter"));
        Assert.assertNotNull(context.get("ooconfig"));
        Assert.assertNotNull(context.get("oomanager"));
    }
}
