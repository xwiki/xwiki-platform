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

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link XWikiRenderingConfiguration}.
 *
 * @version $Id$
 *  @since 2.0M1
 */
@ComponentTest
class XWikiRenderingConfigurationTest
{
    @InjectMockComponents
    private XWikiRenderingConfiguration configuration;

    @MockComponent
    private ConfigurationSource source;

    @Test
    void getLinkLabelFormat()
    {
        when(this.source.getProperty("rendering.linkLabelFormat", "%np")).thenReturn("%np");

        assertEquals("%np", this.configuration.getLinkLabelFormat());
    }

    @Test
    void getTransformationNames()
    {
        when(this.source.getProperty("rendering.transformations", Arrays.asList("macro", "icon"))).thenReturn(
            Arrays.asList("mytransformation"));

        List<String> txs = this.configuration.getTransformationNames();
        assertEquals(1, txs.size());
        assertEquals("mytransformation", txs.get(0));
    }
}
