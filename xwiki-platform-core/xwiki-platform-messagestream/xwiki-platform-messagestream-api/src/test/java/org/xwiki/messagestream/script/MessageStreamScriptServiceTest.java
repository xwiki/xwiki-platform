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
package org.xwiki.messagestream.script;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.messagestream.MessageStream;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link MessageStreamScriptService}.
 * 
 * @version $Id$
 */
public class MessageStreamScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<MessageStreamScriptService> mocker =
        new MockitoComponentMockingRule<>(MessageStreamScriptService.class);

    private MessageStreamScriptService streamService;

    private final DocumentReference targetUser = new DocumentReference("wiki", "XWiki", "JaneBuck");

    private final DocumentReference targetGroup = new DocumentReference("wiki", "XWiki", "MyFriends");

    @Before
    public void configure() throws Exception
    {
        this.streamService = this.mocker.getComponentUnderTest();

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void postPublicMessage() throws Exception
    {
        assertTrue(this.streamService.postPublicMessage("Hello World!"));

        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        verify(stream).postPublicMessage("Hello World!");
    }

    @Test
    public void postPublicMessageWithFailure() throws Exception
    {
        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        doThrow(new RuntimeException("error")).when(stream).postPublicMessage("Hello World!");

        assertFalse(this.streamService.postPublicMessage("Hello World!"));
    }

    @Test
    public void postPersonalMessage() throws Exception
    {
        assertTrue(this.streamService.postPersonalMessage("Hello World!"));

        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        verify(stream).postPersonalMessage("Hello World!");
    }

    @Test
    public void postPersonalMessageWithFailure() throws Exception
    {
        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        doThrow(new RuntimeException("error")).when(stream).postPersonalMessage("Hello World!");

        assertFalse(this.streamService.postPersonalMessage("Hello World!"));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    public void postDirectMessage() throws Exception
    {
        assertTrue(this.streamService.postDirectMessageToUser("Hello World!", this.targetUser));

        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        verify(stream).postDirectMessageToUser("Hello World!", this.targetUser);
    }

    @Test
    public void postDirectMessageWithFailure() throws Exception
    {
        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        doThrow(new RuntimeException("error")).when(stream).postDirectMessageToUser("Hello World!", this.targetUser);

        assertFalse(this.streamService.postDirectMessageToUser("Hello World!", this.targetUser));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    public void postGroupMessage() throws Exception
    {
        assertTrue(this.streamService.postMessageToGroup("Hello World!", this.targetGroup));

        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        verify(stream).postMessageToGroup("Hello World!", this.targetGroup);
    }

    @Test
    public void postGroupMessageWithFailure() throws Exception
    {
        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        doThrow(new RuntimeException("error")).when(stream).postMessageToGroup("Hello World!", this.targetGroup);

        assertFalse(this.streamService.postMessageToGroup("Hello World!", this.targetGroup));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    public void deleteMessage() throws Exception
    {
        assertTrue(this.streamService.deleteMessage("abc123"));

        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        verify(stream).deleteMessage("abc123");
    }

    @Test
    public void deleteMessageWithFailure() throws Exception
    {
        MessageStream stream = this.mocker.getInstance(MessageStream.class);
        doThrow(new IllegalArgumentException("error")).when(stream).deleteMessage("abc123");

        assertFalse(this.streamService.deleteMessage("abc123"));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }
}
