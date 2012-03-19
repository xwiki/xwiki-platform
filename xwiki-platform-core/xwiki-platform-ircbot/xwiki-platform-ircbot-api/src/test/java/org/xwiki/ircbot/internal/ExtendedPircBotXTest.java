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

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.pircbotx.Channel;
import org.pircbotx.OutputThread;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link HelpIRCBotListener}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class ExtendedPircBotXTest extends AbstractComponentTestCase
{
    private class TestableExtendedPircBotX extends ExtendedPircBotX
    {
        public TestableExtendedPircBotX()
        {
            _outputThread = getMockery().mock(OutputThread.class);
        }

        @Override
        public synchronized void disconnect()
        {
            // Don't do anything
        }

        @Override
        public boolean isConnected()
        {
            return true;
        }

        @Override
        public synchronized void dispose()
        {
            // Don't do anything
        }

        public OutputThread getOutputThread()
        {
            return _outputThread;
        }
    }

    private class TestableChannel extends Channel
    {
        public TestableChannel(PircBotX bot, String name)
        {
            super(bot, name);
        }
    }

    private class TestableUser extends User
    {
        protected TestableUser(PircBotX bot, String nick)
        {
            super(bot, nick);
        }
    }

    @Override
    public void setUp() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);
        super.setUp();
    }

    @Test
    public void sendMessageLongerThanFiveLines()
    {
        final TestableExtendedPircBotX bot = new TestableExtendedPircBotX();

        getMockery().checking(new Expectations()
        {
            {
                // Test is here
                oneOf(bot.getOutputThread()).send("PRIVMSG channel :nick: line1");
                oneOf(bot.getOutputThread()).send("PRIVMSG channel :nick: line2");
                oneOf(bot.getOutputThread()).send("PRIVMSG channel :nick: line3");
                oneOf(bot.getOutputThread()).send("PRIVMSG channel :nick: line4");
                oneOf(bot.getOutputThread()).send("PRIVMSG channel :nick: line5");
                oneOf(bot.getOutputThread()).send("PRIVMSG channel :nick: ... and 2 more lines...");
            }
        });

        bot.sendMessage(new TestableChannel(bot, "channel"), new TestableUser(bot, "nick"),
            "line1\nline2\nline3\nline4\nline5\nline6\nline7");
    }
}
