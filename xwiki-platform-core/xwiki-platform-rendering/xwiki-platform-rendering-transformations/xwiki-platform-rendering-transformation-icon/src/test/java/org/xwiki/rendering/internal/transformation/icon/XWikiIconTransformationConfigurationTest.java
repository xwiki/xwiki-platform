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
package org.xwiki.rendering.internal.transformation.icon;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link XWikiIconTransformationConfiguration}.
 *
 * @version $Id$
 * @since 2.6RC1
 */
public class XWikiIconTransformationConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<XWikiIconTransformationConfiguration> mocker =
        new MockitoComponentMockingRule<>(XWikiIconTransformationConfiguration.class);

    @Test
    public void getMappings() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        Properties props = new Properties();
        props.setProperty("::", "test");
        when(source.getProperty("rendering.transformation.icon.mappings", Properties.class)).thenReturn(props);

        Properties mappings = this.mocker.getComponentUnderTest().getMappings();
        assertNotNull(mappings);
        // Make sure we have our mapping coming from the configuration source + the default mappings
        assertTrue(mappings.size() > 1);
        assertEquals("test", mappings.getProperty("::"));
    }
}
