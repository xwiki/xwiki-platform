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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.ircbot.DocumentModifiedEventListenerConfiguration;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Unit tests for {@link DocumentModifiedEventListener}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class DocumentModifiedEventListenerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private DocumentModifiedEventListener listener;

    @Test
    public void onEventWhenBotNotStarted() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(false));

            // The test is here, we verify that sendMessage is never called, i.e. that no message is sent to the IRC
            // channel. Note that this statement is not needed, it's just here to make the test more explicit.
            never(bot).sendMessage(with(any(String.class)), with(any(String.class)));
        }});

        this.listener.onEvent(null, null, null);
    }

    @Test
    public void onEventWhenSourceIsNotAXWikiDocument() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(true));

            // The test is here, we verify that sendMessage is never called, i.e. that no message is sent to the IRC
            // channel. Note that this statement is not needed, it's just here to make the test more explicit.
            never(bot).sendMessage(with(any(String.class)), with(any(String.class)));
        }});

        this.listener.onEvent(null, "not a XWiki Document instance, it's a String.class", null);
    }

    @Test
    public void onEventWhenDocumentCreatedAndNotExcluded() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        final DocumentReference reference = new DocumentReference("wiki", "space", "page");
        final DocumentModifiedEventListenerConfiguration configuration =
            getComponentManager().getInstance(DocumentModifiedEventListenerConfiguration.class);
        final DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");
        final WikiIRCModel ircModel = getComponentManager().getInstance(WikiIRCModel.class);

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();
        final XWikiURLFactory factory = getMockery().mock(XWikiURLFactory.class);
        xwikiContext.setURLFactory(factory);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
                will(returnValue(true));
            oneOf(bot).getChannelsNames();
                will(returnValue(Collections.singleton("channel")));
            oneOf(serializer).serialize(reference);
                will(returnValue("wiki:space.page"));
            oneOf(configuration).getExclusionPatterns();
                will(returnValue(Collections.emptyList()));
            oneOf(serializer).serialize(userReference);
                will(returnValue("userwiki:userspace.userpage"));
            oneOf(ircModel).getXWikiContext();
                will(returnValue(xwikiContext));
            oneOf(factory).createExternalURL("space", "page", "view", null, null, "wiki", xwikiContext);
                will(returnValue(new URL("http://someurl")));

            // The test is here!
            oneOf(bot).sendMessage("channel", "wiki:space.page was modified by userwiki:userspace.userpage (created) - "
                + "http://someurl");
        }});

        XWikiDocument document = new XWikiDocument(reference);
        document.setAuthorReference(userReference);

        this.listener.onEvent(new DocumentCreatedEvent(reference), document, null);
    }

    @Test
    public void onEventWhenDocumentCreatedButExcluded() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        final DocumentReference reference = new DocumentReference("wiki", "space", "page");
        final DocumentModifiedEventListenerConfiguration configuration =
            getComponentManager().getInstance(DocumentModifiedEventListenerConfiguration.class);
        final DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
                will(returnValue(true));
            oneOf(serializer).serialize(reference);
                will(returnValue("wiki:space.page"));
            oneOf(configuration).getExclusionPatterns();
                will(returnValue(Arrays.asList(Pattern.compile(".*:space\\..*"))));
            }});

        Utils.setComponentManager(getComponentManager());
        XWikiDocument document = new XWikiDocument(reference);
        document.setAuthorReference(userReference);

        this.listener.onEvent(new DocumentCreatedEvent(reference), document, null);
    }
}
