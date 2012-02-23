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
package org.xwiki.ircbot.test.ui;

import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;

/**
 * UI tests for the IRC Bot feature.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class IRCBotTest extends AbstractTest
{
    @Test
    public void testAddWikiBotListener()
    {
        // TODO: Login a superadmin for now, remove when http://jira.xwiki.org/jira/browse/XWIKI-7581 is fixed
        getDriver().get(getUtil().getURLToLoginAs("superadmin", "pass"));
        getUtil().recacheSecretToken();

        getUtil().deletePage(getTestClassName(), getTestMethodName());

        getUtil().addObject(getTestClassName(), getTestMethodName(), "IRC.IRCBotListenerClass",
            "description", "bot listener test");
        getUtil().addObject(getTestClassName(), getTestMethodName(), "IRC.IRCBotListenerEventClass",
            "event", "onMessage",
            "script", "{{velocity}}$services.ircbot.bot.sendMessage('gotcha!'){{/velocity}}");

        // TODO: Go to the page listing all Bot Listeners and verify that our new wiki bot listener is listed!
    }
}
