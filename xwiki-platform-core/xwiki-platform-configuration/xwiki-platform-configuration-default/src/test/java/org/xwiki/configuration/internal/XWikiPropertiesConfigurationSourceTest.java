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
package org.xwiki.configuration.internal;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiPropertiesConfigurationSource}.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class XWikiPropertiesConfigurationSourceTest
{
    @Rule
    public MockitoComponentMockingRule<ConfigurationSource> mocker =
        new MockitoComponentMockingRule<>(XWikiPropertiesConfigurationSource.class);

    private Environment environment;

    @Before
    public void before() throws ComponentLookupException
    {
        this.environment = this.mocker.getInstance(Environment.class);
    }

    @Test
    public void testInitializeWhenNoPropertiesFile() throws Exception
    {
        // Verifies that we can get a property from the source (i.e. that it's correctly initialized)
        this.mocker.getComponentUnderTest().getProperty("key");

        verify(this.mocker.getMockedLogger()).debug(
            "No configuration file [{}] found. Using default configuration values.", "/WEB-INF/xwiki.properties");
    }

    @Test
    public void testListParsing() throws ComponentLookupException
    {
        when(environment.getResource("/WEB-INF/xwiki.properties"))
            .thenReturn(getClass().getResource("/xwiki.properties"));

        assertEquals(Arrays.asList("value1", "value2"),
            this.mocker.getComponentUnderTest().getProperty("listProperty"));

        Properties properties = this.mocker.getComponentUnderTest().getProperty("propertiesProperty", Properties.class);
        assertEquals("value1", properties.get("prop1"));
    }
}
