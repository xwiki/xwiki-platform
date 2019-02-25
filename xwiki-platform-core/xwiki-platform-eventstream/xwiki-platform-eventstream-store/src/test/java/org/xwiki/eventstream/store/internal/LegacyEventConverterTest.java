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
package org.xwiki.eventstream.store.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyEventConverterTest {
    @Rule
    public final MockitoComponentMockingRule<LegacyEventConverter> mocker =
            new MockitoComponentMockingRule<>(LegacyEventConverter.class);

    private EventFactory eventFactory;
    private EntityReferenceSerializer<String> serializer;
    private EntityReferenceSerializer<String> compactSerializer;
    private EntityReferenceResolver<String> resolver;
    private EntityReferenceResolver<String> explicitResolver;

    @Before
    public void setUp() throws Exception {
        eventFactory = mocker.getInstance(EventFactory.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);

        compactSerializer = mock(EntityReferenceSerializer.class);
        mocker.registerComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki", compactSerializer);

        resolver = mocker.getInstance(EntityReferenceResolver.TYPE_STRING);
        explicitResolver = mock(EntityReferenceResolver.class);
        mocker.registerComponent(EntityReferenceResolver.TYPE_STRING, "explicit", explicitResolver);
    }

    @Test
    public void convertEventToLegacyActivity() throws Exception {
        WikiReference wikiReference = new WikiReference("xwiki");
        DocumentReference documentReference = new DocumentReference("xwiki", "Some", "Page");
        DocumentReference useReference = new DocumentReference("xwiki", "XWiki", "User");
        Set<String> target = new HashSet<>();
        target.add("xwiki:XWiki.Target");

        Event event = mock(Event.class);
        when(event.getImportance()).thenReturn(Event.Importance.MAJOR);
        when(event.getApplication()).thenReturn("app");
        when(event.getBody()).thenReturn("body");
        when(event.getDate()).thenReturn(new Date(10));
        when(event.getId()).thenReturn("eventId");
        when(event.getWiki()).thenReturn(wikiReference);
        when(event.getDocument()).thenReturn(documentReference);
        when(compactSerializer.serialize(documentReference, wikiReference)).thenReturn("Some.Page");
        when(event.getDocumentTitle()).thenReturn("docTitle");
        when(event.getGroupId()).thenReturn("groupId");
        when(event.getSpace()).thenReturn(documentReference.getLastSpaceReference());
        when(event.getStream()).thenReturn("stream");
        when(event.getTitle()).thenReturn("title");
        when(event.getType()).thenReturn("type");
        when(event.getUrl()).thenReturn(new URL("http://www.xwiki.org"));
        when(event.getUser()).thenReturn(useReference);
        when(event.getDocumentVersion()).thenReturn("10.2");
        when(event.getTarget()).thenReturn(target);
        when(event.getHidden()).thenReturn(true);

        DocumentReference relatedEntity = new DocumentReference("xwiki", "Related", "Entity");
        when(event.getRelatedEntity()).thenReturn(relatedEntity);
        when(serializer.serialize(relatedEntity)).thenReturn("xwiki:Related.Entity");

        when(compactSerializer.serialize(documentReference.getLastSpaceReference(), wikiReference)).thenReturn("Some");
        when(serializer.serialize(useReference)).thenReturn("xwiki:XWiki.User");
        when(serializer.serialize(wikiReference)).thenReturn("xwiki");

        // Test
        LegacyEvent result = mocker.getComponentUnderTest().convertEventToLegacyActivity(event);

        // Verify
        assertNotNull(result);
        assertEquals(40, result.getPriority());
        assertEquals("app", result.getApplication());
        assertEquals("body", result.getBody());
        assertEquals(new Date(10), result.getDate());
        assertEquals("eventId", result.getEventId());
        assertEquals("Some.Page", result.getPage());
        assertEquals("docTitle", result.getParam1());
        assertEquals("xwiki:Related.Entity", result.getParam2());
        assertEquals("groupId", result.getRequestId());
        assertEquals("Some", result.getSpace());
        assertEquals("stream", result.getStream());
        assertEquals("title", result.getTitle());
        assertEquals("type", result.getType());
        assertEquals("http://www.xwiki.org", result.getUrl());
        assertEquals("xwiki:XWiki.User", result.getUser());
        assertEquals("10.2", result.getVersion());
        assertEquals("xwiki", result.getWiki());
        assertEquals(target, result.getTarget());
        assertEquals(true, result.isHidden());
    }

    @Test
    public void convertLegacyActivityToEvent() throws Exception
    {
        Set<String> target = new HashSet<>();
        target.add("xwiki:XWiki.Target");

        LegacyEvent event = new LegacyEvent();
        event.setApplication("app");
        event.setBody("body");
        event.setDate(new Date(10));
        event.setPage("Some.Page");
        event.setWiki("xwiki");
        event.setEventId("eventId");
        event.setParam1("param1");
        event.setPriority(40);
        event.setRequestId("requestId");
        event.setStream("stream");
        event.setTitle("title");
        event.setType("type");
        event.setUrl("https://www.xwiki.org");
        event.setUser("xwiki:XWiki.User");
        event.setVersion("10.3");
        event.setTarget(target);
        event.setHidden(true);

        // Mockers
        when(eventFactory.createRawEvent()).thenReturn(new DefaultEvent());
        when(resolver.resolve("Some.Page", EntityType.DOCUMENT, new WikiReference("xwiki")))
                .thenReturn(new DocumentReference("xwiki", "Some", "Page"));
        when(resolver.resolve("xwiki:XWiki.User", EntityType.DOCUMENT)).thenReturn(
                new DocumentReference("xwiki", "XWiki", "User"));

        // Test
        Event result = mocker.getComponentUnderTest().convertLegacyActivityToEvent(event);

        // Verify
        assertNotNull(result);
        assertEquals("app", result.getApplication());
        assertEquals("body", result.getBody());
        assertEquals("param1", result.getDocumentTitle());
        assertEquals(new DocumentReference("xwiki", "Some", "Page"), result.getDocument());
        assertEquals("eventId", result.getId());
        assertEquals(Event.Importance.MAJOR, result.getImportance());
        assertEquals("requestId", result.getGroupId());
        assertEquals("stream", result.getStream());
        assertEquals("title", result.getTitle());
        assertEquals("type", result.getType());
        assertEquals(new URL("https://www.xwiki.org"), result.getUrl());
        assertEquals(new DocumentReference("xwiki", "XWiki", "User"), result.getUser());
        assertEquals("10.3", result.getDocumentVersion());
        assertEquals(target, result.getTarget());
        assertEquals(true, result.getHidden());
    }

}
