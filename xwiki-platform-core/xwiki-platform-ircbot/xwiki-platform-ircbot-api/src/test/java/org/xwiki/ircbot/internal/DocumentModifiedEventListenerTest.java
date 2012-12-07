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
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.DocumentModifiedEventListenerConfiguration;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.annotation.MockingRequirement;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

import junit.framework.Assert;

/**
 * Unit tests for {@link DocumentModifiedEventListener}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@AllComponents
@MockingRequirement(DocumentModifiedEventListener.class)
public class DocumentModifiedEventListenerTest extends AbstractMockingComponentTestCase
{
    private EventListener listener;

    @Before
    public void configure() throws Exception
    {
        Utils.setComponentManager(getComponentManager());

        this.listener = getComponentManager().getInstance(EventListener.class, "ircdocumentmodified");
    }

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

        // We simulate an EC without any XAR started information
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext ec = new ExecutionContext();

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();
        final XWikiURLFactory factory = getMockery().mock(XWikiURLFactory.class);
        xwikiContext.setURLFactory(factory);
        xwikiContext.setUserReference(userReference);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(true));
            oneOf(serializer).serialize(reference);
            will(returnValue("wiki:space.page"));
            oneOf(execution).getContext();
            will(returnValue(ec));
            oneOf(configuration).getExclusionPatterns();
            will(returnValue(Collections.emptyList()));
            oneOf(serializer).serialize(userReference);
            will(returnValue("userwiki:userspace.userpage"));
            oneOf(factory).createExternalURL("space", "page", "view", null, null, "wiki", xwikiContext);
            will(returnValue(new URL("http://someurl")));
            oneOf(bot).getChannelsNames();
            will(returnValue(Collections.singleton("channel")));

            // The test is here!
            oneOf(bot).sendMessage("channel", "wiki:space.page was created by userwiki:userspace.userpage (comment) - "
                + "http://someurl");
        }});

        XWikiDocument document = new XWikiDocument(reference);
        document.setComment("comment");

        this.listener.onEvent(new DocumentCreatedEvent(reference), document, xwikiContext);
    }

    @Test
    public void onEventWhenDocumentCreatedAndGuestUser() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final EntityReferenceSerializer<String> serializer =
                getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        final DocumentReference reference = new DocumentReference("wiki", "space", "page");
        final DocumentModifiedEventListenerConfiguration configuration =
                getComponentManager().getInstance(DocumentModifiedEventListenerConfiguration.class);

        // We simulate an EC without any XAR started information
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext ec = new ExecutionContext();

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();
        final XWikiURLFactory factory = getMockery().mock(XWikiURLFactory.class);
        xwikiContext.setURLFactory(factory);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
            will(returnValue(true));
            oneOf(serializer).serialize(reference);
            will(returnValue("wiki:space.page"));
            oneOf(execution).getContext();
            will(returnValue(ec));
            oneOf(configuration).getExclusionPatterns();
            will(returnValue(Collections.emptyList()));
            oneOf(factory).createExternalURL("space", "page", "view", null, null, "wiki", xwikiContext);
            will(returnValue(new URL("http://someurl")));
            oneOf(bot).getChannelsNames();
            will(returnValue(Collections.singleton("channel")));

            // The test is here!
            oneOf(bot).sendMessage("channel", "wiki:space.page was created by Guest (comment) - "
                    + "http://someurl");
        }});

        XWikiDocument document = new XWikiDocument(reference);
        document.setComment("comment");

        this.listener.onEvent(new DocumentCreatedEvent(reference), document, xwikiContext);
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

        // We simulate an EC without any XAR started information
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext ec = new ExecutionContext();

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
                will(returnValue(true));
            oneOf(serializer).serialize(reference);
                will(returnValue("wiki:space.page"));
            oneOf(configuration).getExclusionPatterns();
                will(returnValue(Arrays.asList(Pattern.compile(".*:space\\..*"))));
            oneOf(execution).getContext();
                will(returnValue(ec));
        }});

        Utils.setComponentManager(getComponentManager());
        XWikiDocument document = new XWikiDocument(reference);

        this.listener.onEvent(new DocumentCreatedEvent(reference), document, xwikiContext);
    }

    @Test
    public void onEventWhenXARImportStarted() throws Exception
    {
        final DocumentReference reference = new DocumentReference("wiki", "space", "page");

        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);

        // We simulate an EC with XAR started information
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext ec = new ExecutionContext();
        ec.setProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY, 0L);

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
                will(returnValue(true));
            oneOf(serializer).serialize(reference);
                will(returnValue("wiki:space.page"));
            oneOf(execution).getContext();
                will(returnValue(ec));

            // The test is here, we verify that no message is sent to the IRC channel
            never(bot).sendMessage(with(any(String.class)), with(any(String.class)));
        }});

        XWikiDocument document = new XWikiDocument(reference);

        this.listener.onEvent(new DocumentCreatedEvent(reference), document, xwikiContext);

        // We also verify that the XAR import counter is increased by one
        Assert.assertEquals(1L, ec.getProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY));
    }
}
