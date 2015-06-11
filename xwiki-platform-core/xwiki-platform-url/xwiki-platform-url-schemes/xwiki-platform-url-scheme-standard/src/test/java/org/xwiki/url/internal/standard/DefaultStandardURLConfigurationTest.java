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
package org.xwiki.url.internal.standard;

import org.junit.*;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultStandardURLConfiguration}.
 *
 * @version $Id$
 * @since 5.3M1
 */
public class DefaultStandardURLConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultStandardURLConfiguration> mocker =
        new MockitoComponentMockingRule<>(DefaultStandardURLConfiguration.class);

    @Test
    public void isPathBasedMultiWiki() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(source.getProperty("url.standard.multiwiki.isPathBased", Boolean.TRUE)).thenReturn(Boolean.TRUE);

        assertTrue(this.mocker.getComponentUnderTest().isPathBasedMultiWiki());
    }

    @Test
    public void getWikiPathPrefix() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(source.getProperty("url.standard.multiwiki.wikiPathPrefix", "wiki")).thenReturn("wiki");

        assertEquals("wiki", this.mocker.getComponentUnderTest().getWikiPathPrefix());
    }

    @Test
    public void getEntityPathPrefix() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(source.getProperty("url.standard.entityPathPrefix", "bin")).thenReturn("bin");

        assertEquals("bin", this.mocker.getComponentUnderTest().getEntityPathPrefix());
    }

    @Test
    public void isViewActionHidden() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        when(source.getProperty("url.standard.hideViewAction", false)).thenReturn(false);

        assertFalse(this.mocker.getComponentUnderTest().isViewActionHidden());
    }
}
