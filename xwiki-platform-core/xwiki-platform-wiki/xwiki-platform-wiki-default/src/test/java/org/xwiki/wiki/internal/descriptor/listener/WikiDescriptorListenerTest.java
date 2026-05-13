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
package org.xwiki.wiki.internal.descriptor.listener;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link WikiDescriptorListenerTest}.
 *
 * @since 6.0M1
 * @version $Id$
 */
@ComponentTest
class WikiDescriptorListenerTest
{
    @InjectMockComponents
    private WikiDescriptorListener wikiDescriptorListener;

    @MockComponent
    private WikiDescriptorBuilder builder;

    @MockComponent
    private WikiDescriptorCache cache;

    @MockComponent
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @Test
    void onDocumentDeletedEvent()
    {
        XWikiDocument document = mock(XWikiDocument.class);
        XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(document.getOriginalDocument()).thenReturn(originalDocument);

        Event event = new DocumentDeletedEvent();

        List<BaseObject> objects = new ArrayList<>();
        BaseObject object = mock(BaseObject.class);
        objects.add(object);
        when(originalDocument.getXObjects(WikiDescriptorListener.SERVER_CLASS)).thenReturn(objects);

        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwikiA");
        when(originalDocument.getDocumentReference()).thenReturn(documentReference);

        when(this.wikiDescriptorDocumentHelper.getWikiIdFromDocumentReference(documentReference))
            .thenReturn("subwikia");

        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("subwikia", "alias");
        when(this.cache.getFromId("subwikia")).thenReturn(descriptor);

        // Test
        this.wikiDescriptorListener.onEvent(event, document, null);

        // Verify
        verify(this.cache).remove(descriptor.getId(), descriptor.getAliases());
        verify(this.cache, never()).add(any(DefaultWikiDescriptor.class));
    }

    @Test
    void onDocumentUpdatedEvent()
    {
        XWikiDocument document = mock(XWikiDocument.class);
        XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(document.getOriginalDocument()).thenReturn(originalDocument);

        Event event = new DocumentUpdatedEvent();

        List<BaseObject> objects = new ArrayList<>();
        BaseObject object = mock(BaseObject.class);
        objects.add(object);
        when(originalDocument.getXObjects(WikiDescriptorListener.SERVER_CLASS)).thenReturn(objects);

        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwikiA");
        when(originalDocument.getDocumentReference()).thenReturn(documentReference);

        when(this.wikiDescriptorDocumentHelper.getWikiIdFromDocumentReference(documentReference))
            .thenReturn("subwikia");

        DefaultWikiDescriptor descriptor = new DefaultWikiDescriptor("subwikia", "alias");
        when(this.cache.getFromId("subwikia")).thenReturn(descriptor);

        // New objects
        List<BaseObject> newObjects = new ArrayList<>();
        BaseObject newObject = mock(BaseObject.class);
        newObjects.add(newObject);

        when(document.getXObjects(WikiDescriptorListener.SERVER_CLASS)).thenReturn(newObjects);
        DefaultWikiDescriptor newDescriptor = new DefaultWikiDescriptor("subwikia", "newAlias");
        when(this.builder.buildDescriptorObject(newObjects, document)).thenReturn(newDescriptor);

        // Test
        this.wikiDescriptorListener.onEvent(event, document, null);

        // Verify
        verify(this.cache).remove(descriptor.getId(), descriptor.getAliases());
        verify(this.cache).add(newDescriptor);
    }
}
