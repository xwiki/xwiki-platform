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

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
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
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link org.xwiki.eventstream.internal.DefaultEvent default event} and
 * {@link org.xwiki.eventstream.internal.DefaultEventFactory default event factory}.
 * 
 * @version $Id$
 */
public class EventAndFactoryTest extends AbstractMockingComponentTestCase
{
    Event defaultEvent;

    Event rawEvent;

    @MockingRequirement
    private DefaultEventFactory factory;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        final DocumentAccessBridge mockDocumentAccessBridge =
            getComponentManager().getInstance(DocumentAccessBridge.class);
        getMockery().checking(new Expectations()
                    {
            {
                allowing(mockDocumentAccessBridge).getCurrentUser();
                will(returnValue("XWiki.Admin"));
            }
        });

        final ExecutionContext context = new ExecutionContext();
        final Execution mockExecution = getComponentManager().getInstance(Execution.class);
        getMockery().checking(new Expectations()
                    {
            {
                allowing(mockExecution).getContext();
                will(returnValue(context));
            }
        });

        final EntityReferenceResolver<String> mockResolver =
            getComponentManager().getInstance(EntityReferenceResolver.TYPE_STRING);
        getMockery().checking(new Expectations()
                    {
            {
                allowing(mockResolver).resolve("XWiki.Admin", EntityType.DOCUMENT);
                will(returnValue(new DocumentReference("xwiki", "XWiki", "Admin")));
            }
        });
        this.defaultEvent = this.factory.createEvent();
        this.rawEvent = this.factory.createRawEvent();
    }

    @Test
    public void testId()
    {
        Assert.assertNotNull("Event ID not set on new event", this.defaultEvent.getId());
        String id = UUID.randomUUID().toString();
        this.defaultEvent.setId(id);
        Assert.assertEquals("Event ID was not persisted on new event", id, this.defaultEvent.getId());

        Assert.assertNull(this.rawEvent.getId());
        this.rawEvent.setId(id);
        Assert.assertEquals("Event ID was not persisted on raw event", id, this.rawEvent.getId());

        this.rawEvent.setId(null);
        Assert.assertNull(this.rawEvent.getId());
    }

    @Test
    public void testGroupId()
    {
        Assert.assertNotNull("Group ID not set on new event", this.defaultEvent.getGroupId());
        Assert.assertEquals("Consecutive events have different group identifiers", this.defaultEvent.getGroupId(),
            this.factory.createEvent().getGroupId());
        String id = UUID.randomUUID().toString();
        this.defaultEvent.setGroupId(id);
        Assert.assertEquals("Group ID was not persisted", id, this.defaultEvent.getGroupId());

        Assert.assertNull(this.rawEvent.getGroupId());
        this.rawEvent.setGroupId(id);
        Assert.assertEquals(id, this.rawEvent.getGroupId());

        this.rawEvent.setGroupId(null);
        Assert.assertNull(this.rawEvent.getGroupId());
    }

    @Test
    public void testType()
    {
        Assert.assertNull(this.defaultEvent.getType());
        String type = "CommentAdded";
        this.defaultEvent.setType(type);
        Assert.assertEquals(type, this.defaultEvent.getType());

        Assert.assertNull(this.rawEvent.getType());
        this.rawEvent.setType(type);
        Assert.assertEquals(type, this.rawEvent.getType());

        this.rawEvent.setType(null);
        Assert.assertNull(this.rawEvent.getType());
    }

    @Test
    public void testDate()
    {
        Assert.assertNotNull(this.defaultEvent.getDate());
        Assert.assertTrue(new Date().getTime() - this.defaultEvent.getDate().getTime() < 1000);
        Date date = new Date();
        this.defaultEvent.setDate(date);
        Assert.assertEquals(date, this.defaultEvent.getDate());

        Assert.assertNull(this.rawEvent.getDate());
        this.rawEvent.setDate(date);
        Assert.assertEquals(date, this.rawEvent.getDate());

        this.rawEvent.setDate(null);
        Assert.assertNull(this.rawEvent.getDate());
    }

    @Test
    public void testImportance()
    {
        Assert.assertEquals(Importance.MEDIUM, this.defaultEvent.getImportance());
        this.defaultEvent.setImportance(Importance.CRITICAL);
        Assert.assertEquals(Importance.CRITICAL, this.defaultEvent.getImportance());

        Assert.assertEquals(Importance.MEDIUM, this.rawEvent.getImportance());
        this.rawEvent.setImportance(Importance.BACKGROUND);
        Assert.assertEquals(Importance.BACKGROUND, this.rawEvent.getImportance());

        this.rawEvent.setImportance(null);
        Assert.assertEquals(Importance.MEDIUM, this.rawEvent.getImportance());
    }

    @Test
    public void testApplication()
    {
        Assert.assertNull(this.defaultEvent.getApplication());
        String app = "Comments";
        this.defaultEvent.setApplication(app);
        Assert.assertEquals(app, this.defaultEvent.getApplication());

        Assert.assertNull(this.rawEvent.getApplication());
        this.rawEvent.setApplication(app);
        Assert.assertEquals(app, this.rawEvent.getApplication());

        this.rawEvent.setApplication(null);
        Assert.assertNull(this.rawEvent.getApplication());
    }

    @Test
    public void testStream()
    {
        Assert.assertNull(this.defaultEvent.getStream());
        String stream = "xwiki:XWiki.AdminGroup";
        this.defaultEvent.setStream(stream);
        Assert.assertEquals(stream, this.defaultEvent.getStream());

        Assert.assertNull(this.rawEvent.getStream());
        this.rawEvent.setStream(stream);
        Assert.assertEquals(stream, this.rawEvent.getStream());

        this.rawEvent.setStream(null);
        Assert.assertNull(this.rawEvent.getStream());
    }

    @Test
    public void testDocument()
    {
        Assert.assertNull(this.defaultEvent.getDocument());
        DocumentReference ref = new DocumentReference("wiki", "Space", "Page");
        this.defaultEvent.setDocument(ref);
        Assert.assertEquals(ref, this.defaultEvent.getDocument());

        Assert.assertNull(this.rawEvent.getDocument());
        this.rawEvent.setDocument(ref);
        Assert.assertEquals(ref, this.rawEvent.getDocument());

        this.defaultEvent.setDocument(null);
        Assert.assertNull(this.defaultEvent.getDocument());
    }

    @Test
    public void testSpace()
    {
        Assert.assertNull(this.defaultEvent.getSpace());
        DocumentReference doc = new DocumentReference("wiki1", "Space1", "Page");
        SpaceReference space = new SpaceReference("Space2", new WikiReference("wiki2"));

        this.defaultEvent.setDocument(doc);
        Assert.assertEquals(doc.getLastSpaceReference(), this.defaultEvent.getSpace());
        Assert.assertEquals("Space1", this.defaultEvent.getSpace().getName());

        this.defaultEvent.setSpace(space);
        Assert.assertEquals(space, this.defaultEvent.getSpace());
        Assert.assertEquals("Space2", this.defaultEvent.getSpace().getName());

        this.defaultEvent.setSpace(null);
        Assert.assertEquals(doc.getLastSpaceReference(), this.defaultEvent.getSpace());
        this.defaultEvent.setDocument(null);
        Assert.assertNull(this.defaultEvent.getSpace());

        Assert.assertNull(this.rawEvent.getSpace());
    }

    @Test
    public void testWiki()
    {
        Assert.assertNull(this.defaultEvent.getWiki());
        DocumentReference doc = new DocumentReference("wiki1", "Space1", "Page");
        SpaceReference space = new SpaceReference("Space2", new WikiReference("wiki2"));
        WikiReference wiki = new WikiReference("wiki3");

        this.defaultEvent.setDocument(doc);
        Assert.assertEquals(doc.getWikiReference(), this.defaultEvent.getWiki());
        Assert.assertEquals("wiki1", this.defaultEvent.getWiki().getName());

        this.defaultEvent.setSpace(space);
        Assert.assertEquals(space.getRoot(), this.defaultEvent.getWiki());
        Assert.assertEquals("wiki2", this.defaultEvent.getWiki().getName());

        this.defaultEvent.setWiki(wiki);
        Assert.assertEquals(wiki, this.defaultEvent.getWiki());
        Assert.assertEquals("wiki3", this.defaultEvent.getWiki().getName());

        this.defaultEvent.setWiki(null);
        Assert.assertEquals(space.getRoot(), this.defaultEvent.getWiki());
        this.defaultEvent.setSpace(null);
        Assert.assertEquals(doc.getWikiReference(), this.defaultEvent.getWiki());
        this.defaultEvent.setDocument(null);
        Assert.assertNull(this.defaultEvent.getWiki());

        Assert.assertNull(this.rawEvent.getWiki());
    }

    @Test
    public void testRelatedEntity()
    {
        Assert.assertNull(this.defaultEvent.getRelatedEntity());
        DocumentReference ref = new DocumentReference("wiki", "Space", "Page");
        this.defaultEvent.setRelatedEntity(ref);
        Assert.assertEquals(ref, this.defaultEvent.getRelatedEntity());

        this.defaultEvent.setRelatedEntity(null);
        Assert.assertNull(this.defaultEvent.getRelatedEntity());

        Assert.assertNull(this.rawEvent.getRelatedEntity());
    }

    @Test
    public void testDocumentVersion()
    {
        Assert.assertNull(this.defaultEvent.getDocumentVersion());
        String version = "4.2";
        this.defaultEvent.setDocumentVersion(version);
        Assert.assertEquals(version, this.defaultEvent.getDocumentVersion());

        this.defaultEvent.setDocumentVersion(null);
        Assert.assertNull(this.defaultEvent.getDocumentVersion());

        Assert.assertNull(this.rawEvent.getDocumentVersion());
    }

    @Test
    public void testDocumentTitle()
    {
        Assert.assertNull(this.defaultEvent.getDocumentTitle());
        String title = "Welcome to your wiki";
        this.defaultEvent.setDocumentTitle(title);
        Assert.assertEquals(title, this.defaultEvent.getDocumentTitle());

        this.defaultEvent.setDocumentTitle(null);
        Assert.assertNull(this.defaultEvent.getDocumentTitle());

        Assert.assertNull(this.rawEvent.getDocumentTitle());
    }

    @Test
    public void testUser()
    {
        Assert.assertNotNull(this.defaultEvent.getUser());
        DocumentReference user = new DocumentReference("xwiki", "XWiki", "Admin");
        Assert.assertEquals(user, this.defaultEvent.getUser());

        user = new DocumentReference("wiki2", "XWiki", "jdoe");
        this.defaultEvent.setUser(user);
        Assert.assertEquals(user, this.defaultEvent.getUser());

        this.defaultEvent.setUser(null);
        Assert.assertNull(this.defaultEvent.getUser());

        Assert.assertNull(this.rawEvent.getUser());
        this.rawEvent.setUser(user);
        Assert.assertEquals(user, this.rawEvent.getUser());
    }

    @Test
    public void testURL() throws MalformedURLException
    {
        Assert.assertNull(this.defaultEvent.getUrl());
        URL url = new URL("http://xwiki.org/xwiki/bin/Some/Page");
        this.defaultEvent.setUrl(url);
        Assert.assertEquals(url, this.defaultEvent.getUrl());

        this.defaultEvent.setUrl(null);
        Assert.assertNull(this.defaultEvent.getUrl());

        Assert.assertNull(this.rawEvent.getUrl());
    }

    @Test
    public void testTitle()
    {
        Assert.assertNull(this.defaultEvent.getTitle());
        String title = "Deleted attachment file.png";
        this.defaultEvent.setTitle(title);
        Assert.assertEquals(title, this.defaultEvent.getTitle());

        this.defaultEvent.setTitle(null);
        Assert.assertNull(this.defaultEvent.getTitle());

        Assert.assertNull(this.rawEvent.getTitle());
    }

    @Test
    public void testBody()
    {
        Assert.assertNull(this.defaultEvent.getBody());
        String body = "I **do** believe in fairies!";
        this.defaultEvent.setBody(body);
        Assert.assertEquals(body, this.defaultEvent.getBody());

        this.defaultEvent.setBody(null);
        Assert.assertNull(this.defaultEvent.getBody());

        Assert.assertNull(this.rawEvent.getBody());
    }
}
