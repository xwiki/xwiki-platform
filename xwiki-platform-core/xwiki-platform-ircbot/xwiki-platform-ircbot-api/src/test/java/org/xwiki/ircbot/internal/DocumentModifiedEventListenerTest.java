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
package org.xwiki.ircbot.internal;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.DocumentModifiedEventListenerConfiguration;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link DocumentModifiedEventListener}.
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class DocumentModifiedEventListenerTest
{
    @Rule
    public MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        DocumentModifiedEventListener.class);

    private IRCBot bot;

    private DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

    private DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");

    private XWikiDocument document;

    private XWikiContext xcontext;

    private DocumentModifiedEventListenerConfiguration configuration;

    @Before
    public void configure() throws Exception
    {
        bot = mocker.getInstance(IRCBot.class);
        when(bot.isConnected()).thenReturn(true);
        when(bot.getChannelsNames()).thenReturn(Collections.singleton("channel"));

        configuration = mocker.getInstance(DocumentModifiedEventListenerConfiguration.class);
        when(configuration.getExclusionPatterns()).thenReturn(Collections.<Pattern> emptyList());

        Execution execution = mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(new ExecutionContext());

        EntityReferenceSerializer<String> serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(documentReference)).thenReturn("wiki:space.page");
        when(serializer.serialize(userReference)).thenReturn("userwiki:userspace.userpage");

        xcontext = mock(XWikiContext.class);
        when(xcontext.getUserReference()).thenReturn(userReference);

        document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getComment()).thenReturn("comment");
        when(document.getExternalURL("view", null, xcontext)).thenReturn("http://someurl");
    }

    @Test
    public void onEventWhenBotNotStarted() throws Exception
    {
        when(bot.isConnected()).thenReturn(false);

        mocker.getComponentUnderTest().onEvent(null, null, null);

        // Verify that no message is sent to the IRC channel
        verify(bot, never()).sendMessage(anyString(), anyString());
    }

    @Test
    public void onEventWhenSourceIsNotAXWikiDocument() throws Exception
    {
        mocker.getComponentUnderTest().onEvent(null, "not a XWiki Document instance, it's a String.class", null);

        // Verify that no message is sent to the IRC channel
        verify(bot, never()).sendMessage(anyString(), anyString());
    }

    @Test
    public void onEventWhenDocumentCreatedAndNotExcluded() throws Exception
    {
        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(documentReference), document, xcontext);

        verify(bot, times(1)).sendMessage("channel",
            "wiki:space.page was created by userwiki:userspace.userpage (comment) - " + "http://someurl");
    }

    @Test
    public void onEventWhenDocumentCreatedAndGuestUser() throws Exception
    {
        when(xcontext.getUserReference()).thenReturn(null);

        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(documentReference), document, xcontext);

        verify(bot, times(1)).sendMessage("channel",
            "wiki:space.page was created by Guest (comment) - " + "http://someurl");
    }

    @Test
    public void onEventWhenDocumentCreatedButExcluded() throws Exception
    {
        when(configuration.getExclusionPatterns()).thenReturn(Arrays.asList(Pattern.compile(".*:space\\..*")));

        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(documentReference), document, xcontext);

        // Verify that no message is sent to the IRC channel
        verify(bot, never()).sendMessage(anyString(), anyString());
    }

    @Test
    public void onEventWhenXARImportStarted() throws Exception
    {
        // We simulate an EC with XAR started information
        Execution execution = mocker.getInstance(Execution.class);
        execution.getContext().setProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY, 0L);

        mocker.getComponentUnderTest().onEvent(new DocumentCreatedEvent(documentReference), document, xcontext);

        // Verify that no message is sent to the IRC channel
        verify(bot, never()).sendMessage(anyString(), anyString());

        // We also verify that the XAR import counter is increased by one
        assertEquals(1L, execution.getContext().getProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY));
    }
}
