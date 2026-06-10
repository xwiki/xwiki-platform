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
package org.xwiki.lesscss.internal.listeners;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReference;
import org.xwiki.lesscss.internal.colortheme.ColorThemeReferenceFactory;
import org.xwiki.lesscss.internal.colortheme.DocumentColorThemeReference;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.lesscss.internal.listeners.ColorThemeListener}.
 *
 * @version $Id$
 * @since 6.3M2
 */
@ComponentTest
class ColorThemeListenerTest
{
    @InjectMockComponents
    private ColorThemeListener colorThemeListener;

    @MockComponent
    private LESSResourcesCache lessResourcesCache;

    @MockComponent
    private ColorThemeCache colorThemeCache;

    @MockComponent
    private ColorThemeReferenceFactory colorThemeReferenceFactory;

    @Test
    void getName()
    {
        assertEquals("LESS Color Theme Listener", this.colorThemeListener.getName());
    }

    @Test
    void getEvents()
    {
        List<Event> eventsToObserve = List.of(
            new DocumentCreatedEvent(),
            new DocumentUpdatedEvent(),
            new DocumentDeletedEvent());

        assertEquals(eventsToObserve, this.colorThemeListener.getEvents());
    }

    @Test
    void onEventWhenFlamingoThemeChanged()
    {
        // Mocks
        Event event = mock(Event.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        Object data = new Object();

        EntityReference classReference = new LocalDocumentReference("FlamingoThemesCode", "ThemeClass");
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object = mock(BaseObject.class);
        objects.add(object);
        when(doc.getXObjects(classReference)).thenReturn(objects);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(doc.getDocumentReference()).thenReturn(documentReference);

        ColorThemeReference colorThemeReference = new DocumentColorThemeReference(documentReference, null);
        when(this.colorThemeReferenceFactory.createReference(documentReference)).thenReturn(colorThemeReference);

        // Test
        this.colorThemeListener.onEvent(event, doc, data);

        // Verify
        verify(this.lessResourcesCache).clearFromColorTheme(colorThemeReference);
        verify(this.colorThemeCache).clearFromColorTheme(colorThemeReference);
    }

    @Test
    void onEventWhenColorThemeChanged()
    {
        // Mocks
        Event event = mock(Event.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        Object data = new Object();

        EntityReference classReference = new LocalDocumentReference("ColorThemes", "ColorThemeClass");
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object = mock(BaseObject.class);
        objects.add(object);
        when(doc.getXObjects(classReference)).thenReturn(objects);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(doc.getDocumentReference()).thenReturn(documentReference);

        ColorThemeReference colorThemeReference = new DocumentColorThemeReference(documentReference, null);
        when(this.colorThemeReferenceFactory.createReference(documentReference)).thenReturn(colorThemeReference);

        // Test
        this.colorThemeListener.onEvent(event, doc, data);

        // Verify
        verify(this.lessResourcesCache).clearFromColorTheme(colorThemeReference);
        verify(this.colorThemeCache).clearFromColorTheme(colorThemeReference);
    }

    @Test
    void onEventWhenNoObject()
    {
        // Mocks
        Event event = mock(Event.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        Object data = new Object();

        EntityReference classReference = new EntityReference("ColorThemeClass", EntityType.DOCUMENT,
            new EntityReference("ColorThemes", EntityType.SPACE));
        List<BaseObject> objects = new ArrayList<>();
        when(doc.getXObjects(classReference)).thenReturn(objects);

        // Test
        this.colorThemeListener.onEvent(event, doc, data);

        // Verify
        verifyNoInteractions(this.lessResourcesCache);
        verifyNoInteractions(this.colorThemeCache);
    }
}
