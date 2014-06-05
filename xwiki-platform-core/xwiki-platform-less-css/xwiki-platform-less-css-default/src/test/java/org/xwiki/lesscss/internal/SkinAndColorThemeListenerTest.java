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
package org.xwiki.lesscss.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.lesscss.LESSSkinFileCache;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link SkinAndColorThemeListener}.
 *
 * @since 6.1M2
 * @version $Id$
 */
public class SkinAndColorThemeListenerTest
{
    @Rule
    public MockitoComponentMockingRule<SkinAndColorThemeListener> mocker =
            new MockitoComponentMockingRule<>(SkinAndColorThemeListener.class);

    private LESSSkinFileCache lessSkinFileCache;
    @Before
    public void setUp() throws Exception
    {
        lessSkinFileCache = mocker.getInstance(LESSSkinFileCache.class);
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals("LESS Color Theme Listener", mocker.getComponentUnderTest().getName());
    }

    @Test
    public void getEvents() throws Exception
    {
        List<Event> eventsToObserve = Arrays.<Event>asList(
                new DocumentCreatedEvent(),
                new DocumentUpdatedEvent(),
                new DocumentDeletedEvent());

        assertEquals(eventsToObserve, mocker.getComponentUnderTest().getEvents());
    }

    @Test
    public void onEventWhenColorThemeChanged() throws Exception
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

        // Test
        mocker.getComponentUnderTest().onEvent(event, doc, data);

        // Verify
        verify(lessSkinFileCache).clear("wiki");
    }

    @Test
    public void onEventWhenSkinChanged() throws Exception
    {
        // Mocks
        Event event = mock(Event.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        Object data = new Object();

        EntityReference classReference = new LocalDocumentReference("XWiki", "XWikiSkins");
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object = mock(BaseObject.class);
        objects.add(object);
        when(doc.getXObjects(classReference)).thenReturn(objects);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(doc.getDocumentReference()).thenReturn(documentReference);

        // Test
        mocker.getComponentUnderTest().onEvent(event, doc, data);

        // Verify
        verify(lessSkinFileCache).clear("wiki");
    }

    @Test
    public void onEventWhenNoObject() throws Exception
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
        mocker.getComponentUnderTest().onEvent(event, doc, data);

        // Verify
        verify(lessSkinFileCache, never()).clear("wikiId");
    }
}
