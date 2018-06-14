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
package org.xwiki.velocity;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for {@link XWikiWebappResourceLoader}.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents
public class XWikiWebappResourceLoaderTest
{
    @MockComponent
    private Environment environment;

    @MockComponent
    private VelocityConfiguration configuration;

    @InjectComponentManager
    private ComponentManager componentManager;

    @Test
    public void testVelocityInitialization() throws Exception
    {
        VelocityFactory factory = this.componentManager.getInstance(VelocityFactory.class);

        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.RESOURCE_LOADER, "xwiki");
        properties.setProperty("xwiki." + RuntimeConstants.RESOURCE_LOADER + ".class",
            XWikiWebappResourceLoader.class.getName());

        VelocityEngine engine = factory.createVelocityEngine("key", properties);

        // Ensure Velocity has been correctly initialized by trying to evaluate some content.
        StringWriter out = new StringWriter();
        engine.evaluate(new VelocityContext(), out, "template", "#set ($var = 'value')$var");
        assertEquals("value", out.toString());
    }
}
