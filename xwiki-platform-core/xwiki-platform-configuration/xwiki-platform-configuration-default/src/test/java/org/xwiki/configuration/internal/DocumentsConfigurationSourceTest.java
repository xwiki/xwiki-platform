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
package org.xwiki.configuration.internal;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.DocumentsConfigurationSource}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class DocumentsConfigurationSourceTest
{
    @InjectMockComponents
    private DocumentsConfigurationSource source;

    @MockComponent
    @Named("wiki")
    private ConfigurationSource wikiSource;

    @MockComponent
    @Named("spaces")
    private ConfigurationSource spaceSource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @BeforeEach
    void before()
    {
        when(this.xcontextProvider.get()).thenReturn(mock(XWikiContext.class));
    }

    @Test
    void containsKey()
    {
        when(this.wikiSource.containsKey("key")).thenReturn(true);
        when(this.spaceSource.containsKey("key")).thenReturn(false);

        assertTrue(this.source.containsKey("key"));

        // Verify that the order call is correct
        InOrder inOrder = inOrder(wikiSource, spaceSource);
        // First call
        inOrder.verify(spaceSource).containsKey("key");
        // Second call
        inOrder.verify(wikiSource).containsKey("key");
    }
}
