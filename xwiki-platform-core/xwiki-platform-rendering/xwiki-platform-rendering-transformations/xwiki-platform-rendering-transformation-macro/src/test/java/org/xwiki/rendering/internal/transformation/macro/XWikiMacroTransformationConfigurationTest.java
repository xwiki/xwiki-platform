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

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.macro.AbstractMacro.DEFAULT_CATEGORY_DEPRECATED;
import static org.xwiki.rendering.macro.AbstractMacro.DEFAULT_CATEGORY_INTERNAL;

/**
 * Unit tests for {@link XWikiMacroTransformationConfiguration}.
 * 
 * @version $Id$
 * @since 2.6RC1
 */
@ComponentTest
class XWikiMacroTransformationConfigurationTest
{
    @InjectMockComponents
    private XWikiMacroTransformationConfiguration configuration;

    @MockComponent
    private ConfigurationSource source;

    @Test
    void getCategories()
    {
        when(this.source.getProperty("rendering.transformation.macro.categories", Properties.class))
            .thenReturn(new Properties());

        Properties categories = this.configuration.getCategories();
        assertNotNull(categories);
        assertEquals(0, categories.size());
    }

    @Test
    void getHiddenCategories()
    {
        when(this.source.getProperty("rendering.transformation.macro." + "hiddenCategories", List.class,
            List.of(DEFAULT_CATEGORY_INTERNAL, DEFAULT_CATEGORY_DEPRECATED)))
            .thenReturn(List.of("C1", "C2", "C1", "C3"));
        Set<String> hiddenCategories = this.configuration.getHiddenCategories();
        assertEquals(Set.of("C1", "C2", "C3"), hiddenCategories);
    }
}
