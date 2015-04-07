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
package org.xwiki.wiki.internal.configuration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.configuration.DefaultWikiConfiguration}.
 *
 * @version $Id$
 * @since 5.4.4
 */
public class DefaultWikiConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiConfiguration> mocker =
            new MockitoComponentMockingRule(DefaultWikiConfiguration.class);

    private ConfigurationSource configuration;

    @Before
    public void setUp() throws Exception
    {
        configuration = mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
    }

    @Test
    public void getAliasSuffix() throws Exception
    {
        when(configuration.getProperty("wiki.alias.suffix", "")).thenReturn("xwiki.org");
        assertEquals("xwiki.org", mocker.getComponentUnderTest().getAliasSuffix());

        when(configuration.getProperty("wiki.alias.suffix", "")).thenReturn("blabla.org");
        assertEquals("blabla.org", mocker.getComponentUnderTest().getAliasSuffix());
    }
}
