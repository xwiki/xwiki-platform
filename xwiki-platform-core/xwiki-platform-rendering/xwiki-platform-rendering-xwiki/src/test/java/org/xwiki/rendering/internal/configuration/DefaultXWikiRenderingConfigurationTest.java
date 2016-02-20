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
package org.xwiki.rendering.internal.configuration;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.configuration.DefaultXWikiRenderingConfiguration}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultXWikiRenderingConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultXWikiRenderingConfiguration> mocker =
        new MockitoComponentMockingRule<>(DefaultXWikiRenderingConfiguration.class, XWikiRenderingConfiguration.class);

    @Test
    public void getLinkLabelFormat() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.linkLabelFormat", "%np")).thenReturn("%np");

        assertEquals("%np", this.mocker.getComponentUnderTest().getLinkLabelFormat());
    }

    @Test
    public void getImageWidthLimit() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageWidthLimit", -1)).thenReturn(100);

        assertEquals(100, this.mocker.getComponentUnderTest().getImageWidthLimit());
    }

    @Test
    public void getImageHeightLimit() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageHeightLimit", -1)).thenReturn(150);

        assertEquals(150, this.mocker.getComponentUnderTest().getImageHeightLimit());
    }

    @Test
    public void isImageDimensionsIncludedInImageURL() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.imageDimensionsIncludedInImageURL", true)).thenReturn(false);

        assertFalse(this.mocker.getComponentUnderTest().isImageDimensionsIncludedInImageURL());
    }

    @Test
    public void getTransformationNames() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.transformations", Arrays.asList("macro", "icon"))).thenReturn(
            Arrays.asList("mytransformation"));

        List<String> txs = this.mocker.getComponentUnderTest().getTransformationNames();
        assertEquals(1, txs.size());
        assertEquals("mytransformation", txs.get(0));
    }
}
