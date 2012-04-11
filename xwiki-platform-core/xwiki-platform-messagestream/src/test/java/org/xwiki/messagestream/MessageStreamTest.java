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
package org.xwiki.messagestream;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.messagestream.internal.DefaultMessageStream;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link org.xwiki.userstatus.internal.DefaultEvent default event} and
 * {@link org.xwiki.messagestream.internal.DefaultMessageStream default event factory}.
 * 
 * @version $Id$
 */
public class MessageStreamTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private DefaultMessageStream stream;

    private final DocumentReference currentUser = new DocumentReference("wiki", "XWiki", "JohnDoe");

    private final DocumentReference targetUser = new DocumentReference("wiki", "XWiki", "JaneBuck");

    private final DocumentReference targetGroup = new DocumentReference("wiki", "XWiki", "MyFriends");

    @Test
    public void testPostPublicMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();
        this.stream.postPublicMessage("Hello World!");
        Assert.assertEquals("Hello World!", postedMessage.getBody());
        Assert.assertEquals(Importance.MINOR, postedMessage.getImportance());
        Assert.assertEquals("publicMessage", postedMessage.getType());
        Assert.assertEquals(this.currentUser, postedMessage.getRelatedEntity());
    }

    @Test
    public void testPostPublicMessageWithNullMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();
        this.stream.postPublicMessage(null);
        Assert.assertEquals(null, postedMessage.getBody());
    }

    @Test
    public void testPostPublicMessageWithEmptyMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();
        this.stream.postPublicMessage("");
        Assert.assertEquals("", postedMessage.getBody());
    }

    @Test
    public void testPostPublicMessageWithLongMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();
        this.stream.postPublicMessage(StringUtils.repeat('a', 10000));
        Assert.assertEquals(StringUtils.repeat('a', 2000), postedMessage.getBody());
    }

    @Test
    public void testPostPersonalMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();
        this.stream.postPersonalMessage("Hello World!");
        Assert.assertEquals("Hello World!", postedMessage.getBody());
        Assert.assertEquals(Importance.MEDIUM, postedMessage.getImportance());
        Assert.assertEquals("personalMessage", postedMessage.getType());
        Assert.assertEquals(this.currentUser, postedMessage.getRelatedEntity());
    }

    @Test
    public void testPostPersonalMessageWithNullMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();
        this.stream.postPersonalMessage(null);
        Assert.assertEquals(null, postedMessage.getBody());
    }

    @Test
    public void testPostPersonalMessageWithEmptyMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();
        this.stream.postPersonalMessage("");
        Assert.assertEquals("", postedMessage.getBody());
    }

    @Test
    public void testPostPersonalMessageWithLongMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();
        this.stream.postPersonalMessage(StringUtils.repeat('a', 10000));
        Assert.assertEquals(StringUtils.repeat('a', 2000), postedMessage.getBody());
    }

    @Test
    public void testPostDirectMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();
        this.stream.postDirectMessageToUser("Hello World!", this.targetUser);
        Assert.assertEquals("Hello World!", postedMessage.getBody());
        Assert.assertEquals(Importance.CRITICAL, postedMessage.getImportance());
        Assert.assertEquals("directMessage", postedMessage.getType());
        Assert.assertEquals("wiki:XWiki.JaneBuck", postedMessage.getStream());
        Assert.assertEquals(new ObjectReference("XWiki.XWikiUsers", this.targetUser), postedMessage.getRelatedEntity());
    }

    @Test
    public void testPostDirectMessageWithNullMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();
        this.stream.postDirectMessageToUser(null, this.targetUser);
        Assert.assertEquals(null, postedMessage.getBody());
    }

    @Test
    public void testPostDirectMessageWithEmptyMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();
        this.stream.postDirectMessageToUser("", this.targetUser);
        Assert.assertEquals("", postedMessage.getBody());
    }

    @Test
    public void testPostDirectMessageWithLongMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();
        this.stream.postDirectMessageToUser(StringUtils.repeat('a', 10000), this.targetUser);
        Assert.assertEquals(StringUtils.repeat('a', 2000), postedMessage.getBody());
    }

    @Test
    public void testPostGroupMessage() throws Exception
    {
        Event postedMessage = setupForGroupMessage();
        this.stream.postMessageToGroup("Hello Friends!", this.targetGroup);
        Assert.assertEquals("Hello Friends!", postedMessage.getBody());
        Assert.assertEquals(Importance.MAJOR, postedMessage.getImportance());
        Assert.assertEquals("groupMessage", postedMessage.getType());
        Assert.assertEquals("wiki:XWiki.MyFriends", postedMessage.getStream());
        Assert.assertEquals(new ObjectReference("XWiki.XWikiGroups", this.targetGroup), postedMessage
            .getRelatedEntity());
    }

    @Test
    public void testGetRecentPersonalMessagesWithNegativeLimit() throws Exception
    {
        setupForLimitQueries(30, 0);
        this.stream.getRecentPersonalMessages(-4, 0);
    }

    @Test
    public void testGetRecentPersonalMessagesWithZeroLimit() throws Exception
    {
        setupForLimitQueries(30, 0);
        this.stream.getRecentPersonalMessages(0, 0);
    }

    @Test
    public void testGetRecentPersonalMessagesWithLimit1() throws Exception
    {
        setupForLimitQueries(1, 0);
        this.stream.getRecentPersonalMessages(1, 0);
    }

    @Test
    public void testGetRecentPersonalMessagesWithLimit30() throws Exception
    {
        setupForLimitQueries(30, 0);
        this.stream.getRecentPersonalMessages(30, 0);
    }

    @Test
    public void testGetRecentPersonalMessagesWithLimit100() throws Exception
    {
        setupForLimitQueries(100, 0);
        this.stream.getRecentPersonalMessages(100, 0);
    }

    @Test
    public void testGetRecentPersonalMessagesWithNegativeOffset() throws Exception
    {
        setupForLimitQueries(30, 0);
        this.stream.getRecentPersonalMessages(30, -4);
    }

    @Test
    public void testGetRecentPersonalMessagesWithNegativeLimitAndOffset() throws Exception
    {
        setupForLimitQueries(30, 0);
        this.stream.getRecentPersonalMessages(-1, -1);
    }

    @Test
    public void testGetRecentPersonalMessagesWithZeroOffset() throws Exception
    {
        setupForLimitQueries(100, 0);
        this.stream.getRecentPersonalMessages(100, 0);
    }

    @Test
    public void testGetRecentPersonalMessagesWithOffset1() throws Exception
    {
        setupForLimitQueries(20, 1);
        this.stream.getRecentPersonalMessages(20, 1);
    }

    @Test
    public void testGetRecentPersonalMessagesWithOffset100() throws Exception
    {
        setupForLimitQueries(50, 100);
        this.stream.getRecentPersonalMessages(50, 100);
    }

    @Test
    public void testGetRecentPersonalMessagesWhenQueryFails() throws Exception
    {
        final Query mockQuery = getMockQuery();
        final QueryManager mockQueryManager = getComponentManager().getInstance(QueryManager.class);
        final EventStream mockEventStream = getComponentManager().getInstance(EventStream.class);
        final DocumentAccessBridge mockBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        @SuppressWarnings("unchecked")
        final EntityReferenceResolver<String> mockResolver =
            getComponentManager().getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        @SuppressWarnings("unchecked")
        final EntityReferenceSerializer<String> mockSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockBridge).getCurrentUser();
                will(returnValue("XWiki.JohnDoe"));
                allowing(mockResolver).resolve("XWiki.JohnDoe", EntityType.DOCUMENT);
                will(returnValue(MessageStreamTest.this.currentUser));
                allowing(mockSerializer).serialize(MessageStreamTest.this.currentUser);
                will(returnValue("wiki:XWiki.JohnDoe"));
                exactly(1).of(mockQuery).setLimit(30);
                will(returnValue(mockQuery));
                exactly(1).of(mockQuery).setOffset(0);
                will(returnValue(mockQuery));
                allowing(mockQuery).bindValue(with(any(String.class)), with("wiki:XWiki.JohnDoe"));
                allowing(mockQueryManager).createQuery(with(aNonNull(String.class)), with(aNonNull(String.class)));
                will(returnValue(mockQuery));
                exactly(1).of(mockEventStream).searchEvents(with(mockQuery));
                will(throwException(new QueryException("", null, null)));
            }
        });
        List<Event> result = this.stream.getRecentPersonalMessages();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    private Event setupForNewMessage() throws ComponentLookupException, Exception
    {
        final EventFactory mockEventFactory = getComponentManager().getInstance(EventFactory.class);
        final Event e = new DefaultEvent();
        e.setId(UUID.randomUUID().toString());
        final ModelContext mockContext = getComponentManager().getInstance(ModelContext.class);
        final EventStream mockEventStream = getComponentManager().getInstance(EventStream.class);
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockEventFactory).createEvent();
                will(returnValue(e));
                exactly(1).of(mockContext).getCurrentEntityReference();
                will(returnValue(new DocumentReference("wiki", "Space", "Page")));
                exactly(1).of(mockEventStream).addEvent(e);
            }
        });
        return e;
    }

    private Event setupForPublicMessage() throws Exception
    {
        final Event e = setupForNewMessage();
        final DocumentAccessBridge mockBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        @SuppressWarnings("unchecked")
        final EntityReferenceResolver<String> mockResolver =
            getComponentManager().getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        @SuppressWarnings("unchecked")
        final EntityReferenceSerializer<String> mockSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockBridge).getCurrentUser();
                will(returnValue("XWiki.JohnDoe"));
                atLeast(1).of(mockResolver).resolve("XWiki.JohnDoe", EntityType.DOCUMENT);
                will(returnValue(MessageStreamTest.this.currentUser));
                exactly(1).of(mockSerializer).serialize(MessageStreamTest.this.currentUser);
                will(returnValue("wiki:XWiki.JohnDoe"));
            }
        });
        return e;
    }

    private Event setupForPersonalMessage() throws Exception
    {
        final Event e = setupForNewMessage();
        final DocumentAccessBridge mockBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        @SuppressWarnings("unchecked")
        final EntityReferenceResolver<String> mockResolver =
            getComponentManager().getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        @SuppressWarnings("unchecked")
        final EntityReferenceSerializer<String> mockSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockBridge).getCurrentUser();
                will(returnValue("XWiki.JohnDoe"));
                atLeast(1).of(mockResolver).resolve("XWiki.JohnDoe", EntityType.DOCUMENT);
                will(returnValue(MessageStreamTest.this.currentUser));
                exactly(1).of(mockSerializer).serialize(MessageStreamTest.this.currentUser);
                will(returnValue("wiki:XWiki.JohnDoe"));
            }
        });
        return e;
    }

    private Event setupForDirectMessage() throws ComponentLookupException, Exception
    {
        final Event e = setupForNewMessage();
        @SuppressWarnings("unchecked")
        final EntityReferenceSerializer<String> mockSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockSerializer).serialize(MessageStreamTest.this.targetUser);
                will(returnValue("wiki:XWiki.JaneBuck"));
            }
        });
        return e;
    }

    private Event setupForGroupMessage() throws ComponentLookupException, Exception
    {
        final Event e = setupForNewMessage();
        @SuppressWarnings("unchecked")
        final EntityReferenceSerializer<String> mockSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        getMockery().checking(new Expectations()
        {
            {
                exactly(1).of(mockSerializer).serialize(MessageStreamTest.this.targetGroup);
                will(returnValue("wiki:XWiki.MyFriends"));
            }
        });
        return e;
    }

    private void setupForLimitQueries(final int expectedLimit, final int expectedOffset)
        throws ComponentLookupException, Exception
    {
        final Query mockQuery = getMockQuery();
        final QueryManager mockQueryManager = getComponentManager().getInstance(QueryManager.class);
        final EventStream mockEventStream = getComponentManager().getInstance(EventStream.class);
        final DocumentAccessBridge mockBridge = getComponentManager().getInstance(DocumentAccessBridge.class);
        final EntityReferenceResolver<String> mockResolver =
            getComponentManager().getInstance(EntityReferenceResolver.TYPE_STRING, "current");
        @SuppressWarnings("unchecked")
        final EntityReferenceSerializer<String> mockSerializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockBridge).getCurrentUser();
                will(returnValue("XWiki.JohnDoe"));
                allowing(mockResolver).resolve("XWiki.JohnDoe", EntityType.DOCUMENT);
                will(returnValue(MessageStreamTest.this.currentUser));
                allowing(mockSerializer).serialize(MessageStreamTest.this.currentUser);
                will(returnValue("wiki:XWiki.JohnDoe"));
                exactly(1).of(mockQuery).setLimit(expectedLimit);
                will(returnValue(mockQuery));
                exactly(1).of(mockQuery).setOffset(expectedOffset);
                will(returnValue(mockQuery));
                allowing(mockQuery).bindValue(with(any(String.class)), with("wiki:XWiki.JohnDoe"));
                allowing(mockQueryManager).createQuery(with(aNonNull(String.class)), with(aNonNull(String.class)));
                will(returnValue(mockQuery));
                exactly(1).of(mockEventStream).searchEvents(with(mockQuery));
                will(returnValue(null));
            }
        });
    }

    private Query getMockQuery()
    {
        return getMockery().mock(Query.class);
    }
}
