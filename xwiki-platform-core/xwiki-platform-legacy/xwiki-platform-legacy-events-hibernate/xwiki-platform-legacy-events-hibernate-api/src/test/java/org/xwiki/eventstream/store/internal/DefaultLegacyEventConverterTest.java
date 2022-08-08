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

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultLegacyEventConverterTest
{
    @InjectMockComponents
    private DefaultLegacyEventConverter legacyEventConverter;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private EventFactory eventFactory;

    @MockComponent
    private EntityReferenceResolver<String> resolver;

    @MockComponent
    @Named("explicit")
    private EntityReferenceResolver<String> explicitResolver;

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
        LegacyEvent result = legacyEventConverter.convertEventToLegacyActivity(event);

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
        assertTrue(result.isHidden());
    }

    @Test
    public void convertEventToLegacyActivityNoDocument()
    {
        DocumentReference useReference = new DocumentReference("mywiki", "XWiki", "Foo");
        Set<String> target = Collections.singleton("mywiki:XWiki.Bar");
        WikiReference wikiReference = new WikiReference("mywiki");
        Event event = mock(Event.class);
        when(event.getImportance()).thenReturn(Event.Importance.MINOR);
        when(event.getApplication()).thenReturn("app");
        when(event.getBody()).thenReturn("body");
        when(event.getDate()).thenReturn(new Date(10));
        when(event.getId()).thenReturn("eventId");
        when(event.getWiki()).thenReturn(wikiReference);
        when(event.getGroupId()).thenReturn("groupId");
        when(event.getStream()).thenReturn("stream");
        when(event.getTitle()).thenReturn("title");
        when(event.getType()).thenReturn("type");
        when(event.getUser()).thenReturn(useReference);
        when(event.getTarget()).thenReturn(target);
        when(event.getHidden()).thenReturn(false);

        when(serializer.serialize(useReference)).thenReturn("mywiki:XWiki.Foo");
        when(serializer.serialize(wikiReference)).thenReturn("mywiki");

        // Test
        LegacyEvent result = legacyEventConverter.convertEventToLegacyActivity(event);

        // Verify
        assertNotNull(result);
        assertEquals(20, result.getPriority());
        assertEquals("app", result.getApplication());
        assertEquals("body", result.getBody());
        assertEquals(new Date(10), result.getDate());
        assertEquals("eventId", result.getEventId());
        assertNull(result.getPage());
        assertEquals("", result.getParam1());
        assertEquals("", result.getParam2());
        assertEquals("groupId", result.getRequestId());
        assertNull(result.getSpace());
        assertEquals("stream", result.getStream());
        assertEquals("title", result.getTitle());
        assertEquals("type", result.getType());
        assertNull(result.getUrl());
        assertEquals("mywiki:XWiki.Foo", result.getUser());
        assertEquals("", result.getVersion());
        assertEquals("mywiki", result.getWiki());
        assertEquals(target, result.getTarget());
        assertFalse(result.isHidden());

        verify(serializer, times(2)).serialize(any());
        verify(compactSerializer, never()).serialize(any());
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
        DocumentReference eventDocReference = new DocumentReference("xwiki", "Some", "Page");
        when(resolver.resolve("Some.Page", EntityType.DOCUMENT, new WikiReference("xwiki")))
                .thenReturn(eventDocReference);
        DocumentReference userDocReference = new DocumentReference("xwiki", "XWiki", "User");
        when(resolver.resolve("xwiki:XWiki.User", EntityType.DOCUMENT)).thenReturn(userDocReference);

        // Test
        Event result = legacyEventConverter.convertLegacyActivityToEvent(event);

        // Verify
        assertNotNull(result);
        assertEquals("app", result.getApplication());
        assertEquals("body", result.getBody());
        assertEquals("param1", result.getDocumentTitle());
        assertEquals(eventDocReference, result.getDocument());
        assertEquals("eventId", result.getId());
        assertEquals(Event.Importance.MAJOR, result.getImportance());
        assertEquals("requestId", result.getGroupId());
        assertEquals("stream", result.getStream());
        assertEquals("title", result.getTitle());
        assertEquals("type", result.getType());
        assertEquals(new URL("https://www.xwiki.org"), result.getUrl());
        assertEquals(userDocReference, result.getUser());
        assertEquals("10.3", result.getDocumentVersion());
        assertEquals(target, result.getTarget());
        assertTrue(result.getHidden());
    }

    @Test
    public void convertLegacyEventToEventWithoutDocument()
    {
        Set<String> target = new HashSet<>();
        target.add("mywiki:XWiki.Bar");

        LegacyEvent event = new LegacyEvent();
        event.setApplication("app");
        event.setBody("body");
        event.setDate(new Date(10));
        event.setWiki("mywiki");
        event.setEventId("eventId");
        event.setPriority(20);
        event.setRequestId("requestId");
        event.setStream("stream");
        event.setTitle("title");
        event.setType("type");
        event.setUser("mywiki:XWiki.Foo");
        event.setVersion("10.3");
        event.setTarget(target);
        event.setHidden(false);

        // Mockers
        when(eventFactory.createRawEvent()).thenReturn(new DefaultEvent());
        DocumentReference userDocReference = new DocumentReference("mywiki", "XWiki", "User");
        when(resolver.resolve("mywiki:XWiki.Foo", EntityType.DOCUMENT)).thenReturn(userDocReference);

        // Test
        Event result = legacyEventConverter.convertLegacyActivityToEvent(event);

        // Verify
        assertNotNull(result);
        assertEquals("app", result.getApplication());
        assertEquals("body", result.getBody());
        assertEquals("eventId", result.getId());
        assertEquals(Event.Importance.MINOR, result.getImportance());
        assertEquals("requestId", result.getGroupId());
        assertEquals("stream", result.getStream());
        assertEquals("title", result.getTitle());
        assertEquals("type", result.getType());
        assertEquals(userDocReference, result.getUser());
        assertEquals("10.3", result.getDocumentVersion());
        assertEquals(target, result.getTarget());
        assertFalse(result.getHidden());

        verify(resolver, times(1)).resolve(any(), any());
    }
}
