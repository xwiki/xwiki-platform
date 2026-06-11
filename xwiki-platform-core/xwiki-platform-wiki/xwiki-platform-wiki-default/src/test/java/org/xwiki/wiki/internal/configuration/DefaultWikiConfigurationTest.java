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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.configuration.DefaultWikiConfiguration}.
 *
 * @version $Id$
 * @since 5.4.4
 */
@ComponentTest
class DefaultWikiConfigurationTest
{
    @InjectMockComponents
    private DefaultWikiConfiguration defaultWikiConfiguration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Test
    void getAliasSuffix()
    {
        when(this.configuration.getProperty("wiki.alias.suffix", "")).thenReturn("xwiki.org");
        assertEquals("xwiki.org", this.defaultWikiConfiguration.getAliasSuffix());

        when(this.configuration.getProperty("wiki.alias.suffix", "")).thenReturn("blabla.org");
        assertEquals("blabla.org", this.defaultWikiConfiguration.getAliasSuffix());
    }
}
