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

import javax.servlet.ServletContext;

import org.apache.velocity.VelocityContext;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Integration test for {@link XWikiWebappResourceLoader}.
 *
 * @version $Id$
 * @since 3.0M3
 */
public class XWikiWebappResourceLoaderTest extends AbstractComponentTestCase
{
    @Test
    public void testVelocityInitialization() throws Exception
    {
        // Fake the initialization of the Servlet Environment
        ServletEnvironment environment = (ServletEnvironment) getComponentManager().getInstance(Environment.class);
        environment.setServletContext(getMockery().mock(ServletContext.class));
        
        Properties properties = new Properties();
        properties.setProperty("resource.loader", "xwiki");
        properties.setProperty("xwiki.resource.loader.class", XWikiWebappResourceLoader.class.getName());

        VelocityFactory factory = getComponentManager().getInstance(VelocityFactory.class);
        VelocityEngine engine = factory.createVelocityEngine("key", properties);

        // Ensure Velocity has been correctly initialized by trying to evaluate some content.
        StringWriter out = new StringWriter();
        engine.evaluate(new VelocityContext(), out, "template", "#set ($var = 'value')$var");
        Assert.assertEquals("value", out.toString());
    }
}
