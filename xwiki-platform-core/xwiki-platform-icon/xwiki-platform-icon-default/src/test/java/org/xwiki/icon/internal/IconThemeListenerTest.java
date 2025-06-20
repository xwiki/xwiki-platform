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
package org.xwiki.icon.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.icon.IconSetCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.IconThemeListener}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@ComponentTest
class IconThemeListenerTest
{
    @InjectMockComponents
    private IconThemeListener listener;

    @MockComponent
    private IconSetCache iconSetCache;

    @Test
    void onEvent()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        List<BaseObject> list = new ArrayList<>();
        BaseObject obj = mock(BaseObject.class);
        list.add(obj);
        when(obj.getStringValue("name")).thenReturn("icontheme1");

        DocumentReference docRef = new DocumentReference("wikiA", "b", "c");
        when(doc.getDocumentReference()).thenReturn(docRef);

        LocalDocumentReference iconThemeClassRef = new LocalDocumentReference("IconThemesCode", "IconThemeClass");
        when(doc.getXObjects(eq(iconThemeClassRef))).thenReturn(list);

        // Tests
        this.listener.onEvent(null, doc, null);

        // Verify
        verify(iconSetCache, atLeastOnce()).clear(docRef);
        verify(iconSetCache, atLeastOnce()).clear("icontheme1", "wikiA");
    }

    @Test
    void onEventWhenNoObjects()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getXObject(any(DocumentReference.class))).thenReturn(null);

        // Tests
        this.listener.onEvent(null, doc, null);

        // Verify
        verify(iconSetCache, never()).clear(any(DocumentReference.class));
    }

    @Test
    void onEventWhenEmptyListObjects()
    {
        // Mocks
        XWikiDocument doc = mock(XWikiDocument.class);
        List<BaseObject> list = new ArrayList<>();

        LocalDocumentReference iconThemeClassRef = new LocalDocumentReference("IconThemesCode", "IconThemeClass");
        when(doc.getXObjects(eq(iconThemeClassRef))).thenReturn(list);

        // Tests
        this.listener.onEvent(null, doc, null);

        // Verify
        verify(iconSetCache, never()).clear(any(DocumentReference.class));
    }

    @Test
    void getEvents()
    {
        List<Event> results = this.listener.getEvents();

        // Verify
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof DocumentUpdatedEvent);
        assertTrue(results.get(1) instanceof DocumentDeletedEvent);
    }

    @Test
    void getName()
    {
        assertEquals("Icon Theme listener.", this.listener.getName());
    }
}
