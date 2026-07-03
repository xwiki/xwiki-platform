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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.messagestream.MessageStream;
import org.xwiki.messagestream.MessageStreamConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MessageStreamScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class MessageStreamScriptServiceTest
{
    private final DocumentReference targetUser = new DocumentReference("wiki", "XWiki", "JaneBuck");

    private final DocumentReference targetGroup = new DocumentReference("wiki", "XWiki", "MyFriends");

    @InjectMockComponents
    private MessageStreamScriptService streamService;

    @MockComponent
    private Execution execution;

    @MockComponent
    private MessageStream stream;

    @MockComponent
    private MessageStreamConfiguration messageStreamConfiguration;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @BeforeEach
    void setUp()
    {
        when(this.execution.getContext()).thenReturn(new ExecutionContext());
    }

    @Test
    void postPublicMessage()
    {
        assertTrue(this.streamService.postPublicMessage("Hello World!"));
        verify(this.stream).postPublicMessage("Hello World!");
    }

    @Test
    void postPublicMessageWithFailure()
    {
        doThrow(new RuntimeException("error")).when(this.stream).postPublicMessage("Hello World!");
        assertFalse(this.streamService.postPublicMessage("Hello World!"));
    }

    @Test
    void postPersonalMessage()
    {
        assertTrue(this.streamService.postPersonalMessage("Hello World!"));
        verify(this.stream).postPersonalMessage("Hello World!");
    }

    @Test
    void postPersonalMessageWithFailure()
    {
        doThrow(new RuntimeException("error")).when(this.stream).postPersonalMessage("Hello World!");
        assertFalse(this.streamService.postPersonalMessage("Hello World!"));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    void postDirectMessage()
    {
        assertTrue(this.streamService.postDirectMessageToUser("Hello World!", this.targetUser));
        verify(this.stream).postDirectMessageToUser("Hello World!", this.targetUser);
    }

    @Test
    void postDirectMessageWithFailure()
    {
        doThrow(new RuntimeException("error")).when(this.stream)
            .postDirectMessageToUser("Hello World!", this.targetUser);

        assertFalse(this.streamService.postDirectMessageToUser("Hello World!", this.targetUser));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    void postGroupMessage()
    {
        assertTrue(this.streamService.postMessageToGroup("Hello World!", this.targetGroup));
        verify(this.stream).postMessageToGroup("Hello World!", this.targetGroup);
    }

    @Test
    void postGroupMessageWithFailure()
    {
        doThrow(new RuntimeException("error")).when(this.stream).postMessageToGroup("Hello World!", this.targetGroup);

        assertFalse(this.streamService.postMessageToGroup("Hello World!", this.targetGroup));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    void deleteMessage()
    {
        assertTrue(this.streamService.deleteMessage("abc123"));
        verify(this.stream).deleteMessage("abc123");
    }

    @Test
    void deleteMessageWithFailure()
    {
        doThrow(new IllegalArgumentException("error")).when(this.stream).deleteMessage("abc123");

        assertFalse(this.streamService.deleteMessage("abc123"));
        assertEquals("error", this.streamService.getLastError().getMessage());
    }

    @Test
    void isActive()
    {
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(
            new DocumentReference("wikiA", "Space", "Page"),
            new DocumentReference("wikiB", "Space", "Page"));
        when(this.messageStreamConfiguration.isActive("wikiA")).thenReturn(true);
        when(this.messageStreamConfiguration.isActive("wikiB")).thenReturn(false);

        // Test
        assertTrue(this.streamService.isActive());
        assertFalse(this.streamService.isActive());
    }
}
