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
package org.xwiki.lesscss.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.LESSConfiguration}.
 *
 */
public class LESSConfigurationTest {

    @Rule
    public MockitoComponentMockingRule<LESSConfiguration> mocker =
            new MockitoComponentMockingRule<>(LESSConfiguration.class);

    private ConfigurationSource xwikiPropertiesSource;

    @Before
    public void setUp() throws Exception
    {
        this.xwikiPropertiesSource = this.mocker.getInstance(ConfigurationSource.class);
    }

    @Test
    public void maxSimultaneousCompilations() throws Exception
    {
        when(xwikiPropertiesSource.getProperty("lesscss.maximumSimultaneousCompilations", 4)).thenReturn(4);
        mocker.getComponentUnderTest().getMaximumSimultaneousCompilations();
        verify(xwikiPropertiesSource).getProperty("lesscss.maximumSimultaneousCompilations", 4);
    }

    @Test
    public void generateSourceMaps() throws Exception
    {
        when(xwikiPropertiesSource.getProperty("lesscss.generateInlineSourceMaps", false)).thenReturn(false);
        mocker.getComponentUnderTest().isGenerateInlineSourceMaps();
        verify(xwikiPropertiesSource).getProperty("lesscss.generateInlineSourceMaps", false);
    }
}
