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
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.internal.skin.DocumentSkinReference;
import org.xwiki.lesscss.internal.skin.SkinReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link SkinListener}.
 *
 * @since 7.0RC1
 * @version $Id $
 */
public class SkinListenerTest
{
    @Rule
    public MockitoComponentMockingRule<SkinListener> mocker =
            new MockitoComponentMockingRule<>(SkinListener.class);

    private LESSResourcesCache lessResourcesCache;

    private ColorThemeCache colorThemeCache;

    private SkinReferenceFactory skinReferenceFactory;

    @Before
    public void setUp() throws Exception
    {
        lessResourcesCache = mocker.getInstance(LESSResourcesCache.class);
        colorThemeCache = mocker.getInstance(ColorThemeCache.class);
        skinReferenceFactory = mocker.getInstance(SkinReferenceFactory.class);
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals("LESS Skin Listener", mocker.getComponentUnderTest().getName());
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
    public void onEventWhenSkinIsChanged() throws Exception
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
        
        DocumentSkinReference skinReference = new DocumentSkinReference(documentReference, null);
        when(skinReferenceFactory.createReference(documentReference)).thenReturn(skinReference);

        // Test
        mocker.getComponentUnderTest().onEvent(event, doc, data);

        // Verify
        verify(lessResourcesCache).clearFromSkin(skinReference);
        verify(colorThemeCache).clearFromSkin(skinReference);
    }

    @Test
    public void onEventWhenNoObject() throws Exception
    {
        // Mocks
        Event event = mock(Event.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        Object data = new Object();

        EntityReference classReference = new LocalDocumentReference("XWiki", "XWikiSkins");
        List<BaseObject> objects = new ArrayList<>();
        when(doc.getXObjects(classReference)).thenReturn(objects);

        // Test
        mocker.getComponentUnderTest().onEvent(event, doc, data);

        // Verify
        verifyNoInteractions(lessResourcesCache);
        verifyNoInteractions(colorThemeCache);
    }
}
