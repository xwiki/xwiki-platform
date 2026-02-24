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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultStandardURLConfiguration}.
 *
 * @version $Id$
 * @since 5.3M1
 */
@ComponentTest
class DefaultStandardURLConfigurationTest
{
    @InjectMockComponents
    private DefaultStandardURLConfiguration configuration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource source;

    @Test
    void isPathBasedMultiWiki() throws Exception
    {
        when(this.source.getProperty("url.standard.multiwiki.isPathBased", Boolean.TRUE)).thenReturn(Boolean.TRUE);

        assertTrue(this.configuration.isPathBasedMultiWiki());
    }

    @Test
    void getWikiPathPrefix() throws Exception
    {
        when(this.source.getProperty("url.standard.multiwiki.wikiPathPrefix", "wiki")).thenReturn("wiki");

        assertEquals("wiki", this.configuration.getWikiPathPrefix());
    }

    @Test
    void getEntityPathPrefix() throws Exception
    {
        when(this.source.getProperty("url.standard.entityPathPrefix", "bin")).thenReturn("bin");

        assertEquals("bin", this.configuration.getEntityPathPrefix());
    }

    @Test
    void isViewActionHidden() throws Exception
    {
        when(source.getProperty("url.standard.hideViewAction", false)).thenReturn(false);

        assertFalse(this.configuration.isViewActionHidden());
    }
}
