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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.lesscss.cache.ColorThemeCache;
import org.xwiki.lesscss.cache.LESSResourcesCache;
import org.xwiki.lesscss.resources.LESSObjectPropertyResourceReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.web.Utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 6.4RC1
 * @version $Id$
 */
public class SSXListenerTest
{
    @Rule
    public MockitoComponentMockingRule<SSXListener> mocker =
            new MockitoComponentMockingRule<>(SSXListener.class);

    private LESSResourcesCache lessResourcesCache;

    private ColorThemeCache colorThemeCache;

    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        lessResourcesCache = mocker.getInstance(LESSResourcesCache.class);
        colorThemeCache = mocker.getInstance(ColorThemeCache.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals("LESS SSX objects listener", mocker.getComponentUnderTest().getName());
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
    public void onEvent() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        XWikiDocument doc = mock(XWikiDocument.class);
        BaseObject obj1 = mock(BaseObject.class);
        BaseObject obj2 = mock(BaseObject.class);
        List<BaseObject> objList = new ArrayList<>();
        DocumentReference ssxDocRef = new DocumentReference("wiki", "XWiki", "StyleSheetExtension");
        when(doc.getXObjects(eq(ssxDocRef))).thenReturn(objList);
        objList.add(obj1);
        objList.add(null);
        objList.add(obj2);
        when(obj1.getStringValue("contentType")).thenReturn("CSS");
        when(obj2.getStringValue("contentType")).thenReturn("LESS");
        when(obj2.getNumber()).thenReturn(2);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Doc");
        when(doc.getDocumentReference()).thenReturn(documentReference);

        // Because BaseObjectReference uses components from the Utils class, we need to set up the component manager
        ComponentManager rootComponentManager = mock(ComponentManager.class);
        Utils.setComponentManager(rootComponentManager);
        ComponentManager contextComponentManager = mock(ComponentManager.class);
        when(rootComponentManager.getInstance(ComponentManager.class, "context")).thenReturn(contextComponentManager);

        // Mock to serialize the object
        EntityReferenceSerializer entityReferenceSerializer = mock(EntityReferenceSerializer.class);
        when(contextComponentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, "default"))
            .thenReturn(entityReferenceSerializer);
        when(entityReferenceSerializer.serialize(any(EntityReference.class))).thenReturn("objName");

        // Mock to resolve the object
        DocumentReferenceResolver documentReferenceResolver = mock(DocumentReferenceResolver.class);
        when(contextComponentManager.getInstance(DocumentReferenceResolver.TYPE_STRING, "default"))
            .thenReturn(documentReferenceResolver);
        when(documentReferenceResolver.resolve(anyString())).thenReturn(new DocumentReference("a", "b", "c"));

        // Test
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), doc, new Object());

        // Verify
        LESSObjectPropertyResourceReference resourceRef = new LESSObjectPropertyResourceReference(
            new ObjectPropertyReference("code", new BaseObjectReference(ssxDocRef, 2, documentReference)));
        verify(lessResourcesCache, atLeastOnce()).clearFromLESSResource(eq(resourceRef));
        verify(colorThemeCache, atLeastOnce()).clearFromLESSResource(eq(resourceRef));
    }
}
