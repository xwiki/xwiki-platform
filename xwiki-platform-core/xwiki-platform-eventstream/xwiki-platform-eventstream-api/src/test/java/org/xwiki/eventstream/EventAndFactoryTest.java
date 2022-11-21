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
package org.xwiki.eventstream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.internal.DefaultEventFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link org.xwiki.eventstream.internal.DefaultEvent default event} and
 * {@link org.xwiki.eventstream.internal.DefaultEventFactory default event factory}.
 * 
 * @version $Id$
 */
@ComponentTest
class EventAndFactoryTest
{
    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private Execution execution;

    @MockComponent
    private EntityReferenceResolver<String> resolver;

    @InjectMockComponents
    private DefaultEventFactory factory;

    private Event defaultEvent;

    private Event rawEvent;

    @BeforeEach
    void configure() throws Exception
    {
        when(this.documentAccessBridge.getCurrentUser()).thenReturn("XWiki.Admin");

        ExecutionContext context = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(context);

        when(resolver.resolve("XWiki.Admin", EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("xwiki", "XWiki", "Admin"));

        this.defaultEvent = this.factory.createEvent();
        this.rawEvent = this.factory.createRawEvent();
    }

    @Test
    void testId()
    {
        assertNotNull("Event ID not set on new event", this.defaultEvent.getId());
        String id = UUID.randomUUID().toString();
        this.defaultEvent.setId(id);
        assertEquals(id, this.defaultEvent.getId(), "Event ID was not persisted on new event");

        assertNull(this.rawEvent.getId());
        this.rawEvent.setId(id);
        assertEquals(id, this.rawEvent.getId(), "Event ID was not persisted on raw event");

        this.rawEvent.setId(null);
        assertNull(this.rawEvent.getId());
    }

    @Test
    void testGroupId()
    {
        assertNotNull("Group ID not set on new event", this.defaultEvent.getGroupId());
        assertEquals(this.defaultEvent.getGroupId(), this.factory.createEvent().getGroupId(),
            "Consecutive events have different group identifiers");
        String id = UUID.randomUUID().toString();
        this.defaultEvent.setGroupId(id);
        assertEquals(id, this.defaultEvent.getGroupId(), "Group ID was not persisted");

        assertNull(this.rawEvent.getGroupId());
        this.rawEvent.setGroupId(id);
        assertEquals(id, this.rawEvent.getGroupId());

        this.rawEvent.setGroupId(null);
        assertNull(this.rawEvent.getGroupId());
    }

    @Test
    void testType()
    {
        assertNull(this.defaultEvent.getType());
        String type = "CommentAdded";
        this.defaultEvent.setType(type);
        assertEquals(type, this.defaultEvent.getType());

        assertNull(this.rawEvent.getType());
        this.rawEvent.setType(type);
        assertEquals(type, this.rawEvent.getType());

        this.rawEvent.setType(null);
        assertNull(this.rawEvent.getType());
    }

    @Test
    void testDate()
    {
        assertNotNull(this.defaultEvent.getDate());
        assertTrue(new Date().getTime() - this.defaultEvent.getDate().getTime() < 1000);
        Date date = new Date();
        this.defaultEvent.setDate(date);
        assertEquals(date, this.defaultEvent.getDate());

        assertNull(this.rawEvent.getDate());
        this.rawEvent.setDate(date);
        assertEquals(date, this.rawEvent.getDate());

        this.rawEvent.setDate(null);
        assertNull(this.rawEvent.getDate());
    }

    @Test
    void testImportance()
    {
        assertEquals(Importance.MEDIUM, this.defaultEvent.getImportance());
        this.defaultEvent.setImportance(Importance.CRITICAL);
        assertEquals(Importance.CRITICAL, this.defaultEvent.getImportance());

        assertEquals(Importance.MEDIUM, this.rawEvent.getImportance());
        this.rawEvent.setImportance(Importance.BACKGROUND);
        assertEquals(Importance.BACKGROUND, this.rawEvent.getImportance());

        this.rawEvent.setImportance(null);
        assertEquals(Importance.MEDIUM, this.rawEvent.getImportance());
    }

    @Test
    void testApplication()
    {
        assertNull(this.defaultEvent.getApplication());
        String app = "Comments";
        this.defaultEvent.setApplication(app);
        assertEquals(app, this.defaultEvent.getApplication());

        assertNull(this.rawEvent.getApplication());
        this.rawEvent.setApplication(app);
        assertEquals(app, this.rawEvent.getApplication());

        this.rawEvent.setApplication(null);
        assertNull(this.rawEvent.getApplication());
    }

    @Test
    void testStream()
    {
        assertNull(this.defaultEvent.getStream());
        String stream = "xwiki:XWiki.AdminGroup";
        this.defaultEvent.setStream(stream);
        assertEquals(stream, this.defaultEvent.getStream());

        assertNull(this.rawEvent.getStream());
        this.rawEvent.setStream(stream);
        assertEquals(stream, this.rawEvent.getStream());

        this.rawEvent.setStream(null);
        assertNull(this.rawEvent.getStream());
    }

    @Test
    void testDocument()
    {
        assertNull(this.defaultEvent.getDocument());
        DocumentReference ref = new DocumentReference("wiki", "Space", "Page");
        this.defaultEvent.setDocument(ref);
        assertEquals(ref, this.defaultEvent.getDocument());

        assertNull(this.rawEvent.getDocument());
        this.rawEvent.setDocument(ref);
        assertEquals(ref, this.rawEvent.getDocument());

        this.defaultEvent.setDocument(null);
        assertNull(this.defaultEvent.getDocument());
    }

    @Test
    void testSpace()
    {
        assertNull(this.defaultEvent.getSpace());
        DocumentReference doc = new DocumentReference("wiki1", "Space1", "Page");
        SpaceReference space = new SpaceReference("Space2", new WikiReference("wiki2"));

        this.defaultEvent.setDocument(doc);
        assertEquals(doc.getLastSpaceReference(), this.defaultEvent.getSpace());
        assertEquals("Space1", this.defaultEvent.getSpace().getName());

        this.defaultEvent.setSpace(space);
        assertEquals(space, this.defaultEvent.getSpace());
        assertEquals("Space2", this.defaultEvent.getSpace().getName());

        this.defaultEvent.setSpace(null);
        assertEquals(doc.getLastSpaceReference(), this.defaultEvent.getSpace());
        this.defaultEvent.setDocument(null);
        assertNull(this.defaultEvent.getSpace());

        assertNull(this.rawEvent.getSpace());
    }

    @Test
    void testWiki()
    {
        assertNull(this.defaultEvent.getWiki());
        DocumentReference doc = new DocumentReference("wiki1", "Space1", "Page");
        SpaceReference space = new SpaceReference("Space2", new WikiReference("wiki2"));
        WikiReference wiki = new WikiReference("wiki3");

        this.defaultEvent.setDocument(doc);
        assertEquals(doc.getWikiReference(), this.defaultEvent.getWiki());
        assertEquals("wiki1", this.defaultEvent.getWiki().getName());

        this.defaultEvent.setSpace(space);
        assertEquals(space.getRoot(), this.defaultEvent.getWiki());
        assertEquals("wiki2", this.defaultEvent.getWiki().getName());

        this.defaultEvent.setWiki(wiki);
        assertEquals(wiki, this.defaultEvent.getWiki());
        assertEquals("wiki3", this.defaultEvent.getWiki().getName());

        this.defaultEvent.setWiki(null);
        assertEquals(space.getRoot(), this.defaultEvent.getWiki());
        this.defaultEvent.setSpace(null);
        assertEquals(doc.getWikiReference(), this.defaultEvent.getWiki());
        this.defaultEvent.setDocument(null);
        assertNull(this.defaultEvent.getWiki());

        assertNull(this.rawEvent.getWiki());
    }

    @Test
    void testRelatedEntity()
    {
        assertNull(this.defaultEvent.getRelatedEntity());
        DocumentReference ref = new DocumentReference("wiki", "Space", "Page");
        this.defaultEvent.setRelatedEntity(ref);
        assertEquals(ref, this.defaultEvent.getRelatedEntity());

        this.defaultEvent.setRelatedEntity(null);
        assertNull(this.defaultEvent.getRelatedEntity());

        assertNull(this.rawEvent.getRelatedEntity());
    }

    @Test
    void testDocumentVersion()
    {
        assertNull(this.defaultEvent.getDocumentVersion());
        String version = "4.2";
        this.defaultEvent.setDocumentVersion(version);
        assertEquals(version, this.defaultEvent.getDocumentVersion());

        this.defaultEvent.setDocumentVersion(null);
        assertNull(this.defaultEvent.getDocumentVersion());

        assertNull(this.rawEvent.getDocumentVersion());
    }

    @Test
    void testDocumentTitle()
    {
        assertNull(this.defaultEvent.getDocumentTitle());
        String title = "Welcome to your wiki";
        this.defaultEvent.setDocumentTitle(title);
        assertEquals(title, this.defaultEvent.getDocumentTitle());

        this.defaultEvent.setDocumentTitle(null);
        assertNull(this.defaultEvent.getDocumentTitle());

        assertNull(this.rawEvent.getDocumentTitle());
    }

    @Test
    void testUser()
    {
        assertNotNull(this.defaultEvent.getUser());
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "Admin");
        assertEquals(user, this.defaultEvent.getUser());

        user = new DocumentReference("wiki2", "XWiki", "jdoe");
        this.defaultEvent.setUser(user);
        assertEquals(user, this.defaultEvent.getUser());

        this.defaultEvent.setUser(null);
        assertNull(this.defaultEvent.getUser());

        assertNull(this.rawEvent.getUser());
        this.rawEvent.setUser(user);
        assertEquals(user, this.rawEvent.getUser());
    }

    @Test
    void testURL() throws MalformedURLException
    {
        assertNull(this.defaultEvent.getUrl());
        URL url = new URL("http://xwiki.org/xwiki/bin/Some/Page");
        this.defaultEvent.setUrl(url);
        assertEquals(url, this.defaultEvent.getUrl());

        this.defaultEvent.setUrl(null);
        assertNull(this.defaultEvent.getUrl());

        assertNull(this.rawEvent.getUrl());
    }

    @Test
    void testTitle()
    {
        assertNull(this.defaultEvent.getTitle());
        String title = "Deleted attachment file.png";
        this.defaultEvent.setTitle(title);
        assertEquals(title, this.defaultEvent.getTitle());

        this.defaultEvent.setTitle(null);
        assertNull(this.defaultEvent.getTitle());

        assertNull(this.rawEvent.getTitle());
    }

    @Test
    void testBody()
    {
        assertNull(this.defaultEvent.getBody());
        String body = "I **do** believe in fairies!";
        this.defaultEvent.setBody(body);
        assertEquals(body, this.defaultEvent.getBody());

        this.defaultEvent.setBody(null);
        assertNull(this.defaultEvent.getBody());

        assertNull(this.rawEvent.getBody());
    }
}
