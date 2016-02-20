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
package org.xwiki.rendering.internal.transformation.macro;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link XWikiMacroTransformationConfiguration}.
 * 
 * @version $Id$
 * @since 2.6RC1
 */
public class XWikiMacroTransformationConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<XWikiMacroTransformationConfiguration> mocker =
        new MockitoComponentMockingRule<>(XWikiMacroTransformationConfiguration.class);

    @Test
    public void getCategories() throws Exception
    {
        ConfigurationSource source = this.mocker.getInstance(ConfigurationSource.class);
        when(source.getProperty("rendering.transformation.macro.categories", Properties.class)).thenReturn(
            new Properties());

        Properties categories = this.mocker.getComponentUnderTest().getCategories();
        assertNotNull(categories);
        assertEquals(0, categories.size());
    }
}
