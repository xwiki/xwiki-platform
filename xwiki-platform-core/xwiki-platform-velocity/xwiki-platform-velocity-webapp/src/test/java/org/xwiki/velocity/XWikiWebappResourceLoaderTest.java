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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.velocity.runtime.RuntimeConstants;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link XWikiWebappResourceLoader}.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents
class XWikiWebappResourceLoaderTest
{
    @MockComponent
    private ConfigurationSource configuration;

    @MockComponent
    private LocalizationContext localizationContext;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Test
    void testVelocityInitialization() throws Exception
    {
        this.componentManager.registerMemoryConfigurationSource();

        // Fake the initialization of the Servlet Environment
        ServletEnvironment environment = (ServletEnvironment) this.componentManager.getInstance(Environment.class);
        ServletContext servletContext = mock(ServletContext.class);
        environment.setServletContext(servletContext);
        when(servletContext.getResourceAsStream("/templates/macros.vm"))
            .thenReturn(new ByteArrayInputStream(new byte[0]));

        Properties properties = new Properties();
        properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "xwiki");
        properties.setProperty(RuntimeConstants.RESOURCE_LOADER + ".xwiki." + RuntimeConstants.RESOURCE_LOADER_CLASS,
            XWikiWebappResourceLoader.class.getName());

        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);

        // Ensure Velocity has been correctly initialized by trying to evaluate some content.
        StringWriter out = new StringWriter();
        velocityManager.evaluate(out, "template", new StringReader("#set ($var = 'value')$var"));
        assertEquals("value", out.toString());
    }
}
