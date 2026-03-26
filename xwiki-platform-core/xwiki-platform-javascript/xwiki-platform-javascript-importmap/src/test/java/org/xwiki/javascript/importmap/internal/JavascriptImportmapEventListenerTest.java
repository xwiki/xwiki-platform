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
package org.xwiki.javascript.importmap.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.AbstractExtensionEvent;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link JavascriptImportmapEventListener}.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
@ComponentTest
class JavascriptImportmapEventListenerTest
{
    @InjectMockComponents
    private JavascriptImportmapEventListener listener;

    @MockComponent
    private JavascriptImportmapResolver javascriptImportmapResolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    void onEventNoNamespace()
    {
        var mock = mock(AbstractExtensionEvent.class);
        when(mock.hasNamespace()).thenReturn(false);
        this.listener.onEvent(mock, null, null);
        verify(this.javascriptImportmapResolver).clearCache();
    }

    @Test
    void onEventMatchingNamespace()
    {
        var extensionInstalledEvent =
            new ExtensionInstalledEvent(mock(ExtensionId.class), new WikiNamespace("wiki1").serialize());
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki1");
        this.listener.onEvent(extensionInstalledEvent, null, null);
        verify(this.javascriptImportmapResolver).clearCache();
    }

    @Test
    void onEventDifferentNamespaces()
    {
        var extensionInstalledEvent =
            new ExtensionInstalledEvent(mock(ExtensionId.class), new WikiNamespace("wiki1").serialize());
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki2");
        this.listener.onEvent(extensionInstalledEvent, null, null);
        verify(this.javascriptImportmapResolver, never()).clearCache();
    }

    @Test
    void onEventWrongEvent()
    {
        this.listener.onEvent(mock(Event.class), null, null);
        verify(this.javascriptImportmapResolver, never()).clearCache();
    }
}
