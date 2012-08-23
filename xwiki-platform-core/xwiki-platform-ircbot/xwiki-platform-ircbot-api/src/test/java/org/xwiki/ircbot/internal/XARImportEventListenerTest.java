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

import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.annotation.MockingRequirement;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

import junit.framework.Assert;

/**
 * Unit tests for {@link org.xwiki.ircbot.internal.XARImportEventListener}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@AllComponents
public class XARImportEventListenerTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private XARImportEventListener listener;

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
    public void onEventWhenXARImportStarted() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final WikiIRCModel ircModel = getComponentManager().getInstance(WikiIRCModel.class);
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        final DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");

        // We simulate an EC without any XAR started information
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext ec = new ExecutionContext();

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();
        xwikiContext.setUserReference(userReference);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
                will(returnValue(true));
            oneOf(bot).getChannelsNames();
                will(returnValue(Collections.singleton("channel")));
            oneOf(execution).getContext();
                will(returnValue(ec));
            oneOf(ircModel).getXWikiContext();
                will(returnValue(xwikiContext));
            oneOf(serializer).serialize(userReference);
                will(returnValue("userwiki:userspace.userpage"));

            // The test is here!
            oneOf(bot).sendMessage("channel", "A XAR import has been started by userwiki:userspace.userpage");
        }});

        this.listener.onEvent(new XARImportingEvent(), null, null);

        // Also verify that the XAR import counter has been set to 0 in the EC
        Assert.assertEquals(0L, ec.getProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY));
    }

    @Test
    public void onEventWhenXARImportFinished() throws Exception
    {
        final IRCBot bot = getComponentManager().getInstance(IRCBot.class);
        final WikiIRCModel ircModel = getComponentManager().getInstance(WikiIRCModel.class);
        final EntityReferenceSerializer<String> serializer =
            getComponentManager().getInstance(EntityReferenceSerializer.TYPE_STRING);
        final DocumentReference userReference = new DocumentReference("userwiki", "userspace", "userpage");

        // We simulate an EC with some XAR started information (100 documents imported)
        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext ec = new ExecutionContext();
        ec.setProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY, 100L);

        Utils.setComponentManager(getComponentManager());
        final XWikiContext xwikiContext = new XWikiContext();
        xwikiContext.setUserReference(userReference);

        getMockery().checking(new Expectations()
        {{
            oneOf(bot).isConnected();
                will(returnValue(true));
            oneOf(bot).getChannelsNames();
                will(returnValue(Collections.singleton("channel")));
            oneOf(execution).getContext();
                will(returnValue(ec));
            oneOf(ircModel).getXWikiContext();
                will(returnValue(xwikiContext));
            oneOf(serializer).serialize(userReference);
                will(returnValue("userwiki:userspace.userpage"));

            // The test is here!
            oneOf(bot).sendMessage("channel", "The XAR import started by userwiki:userspace.userpage is now finished, "
                + "100 documents have been imported");
        }});

        this.listener.onEvent(new XARImportedEvent(), null, null);

        // Also verify that the XAR import counter has been removed
        Assert.assertNull(ec.getProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY));
    }
}
