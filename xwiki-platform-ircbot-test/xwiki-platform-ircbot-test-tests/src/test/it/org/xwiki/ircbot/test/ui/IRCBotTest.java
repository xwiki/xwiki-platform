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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.ircbot.test.po.IRCBotConfigurationPage;
import org.xwiki.ircbot.test.po.IRCBotPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;

import junit.framework.Assert;

/**
 * UI tests for the IRC Bot feature.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class IRCBotTest extends AbstractTest
{
    @Before
    public void setUp()
    {
        // Login as superadmin to have delete rights.
        getDriver().get(getUtil().getURLToLoginAs("superadmin", "pass"));
        getUtil().recacheSecretToken();
    }

    @Test
    public void testBot()
    {
        getUtil().deletePage(getTestClassName(), getTestMethodName());
        String triggerPageName = getTestMethodName() + "NewPage";
        getUtil().deletePage(getTestClassName(), triggerPageName);
        getUtil().deletePage("IRC", "testarchive");

        // Configure the Logging Bot Listener to log into a fixed name page so that we can easily delete that page at
        // the test start and find it easily to assert its content below.
        IRCBotConfigurationPage configPage = IRCBotConfigurationPage.gotoPage();
        configPage.setLoggingPage("IRC.testarchive");

        // Go to the main Bot home page
        IRCBotPage page = IRCBotPage.gotoPage();

        // Verify that the Bot is stopped
        Assert.assertFalse(page.isBotStarted());

        // Add a Bot Listener
        getUtil().addObject(getTestClassName(), getTestMethodName(), "IRC.IRCBotListenerClass",
            "description", "bot listener test");
        getUtil().addObject(getTestClassName(), getTestMethodName(), "IRC.IRCBotListenerEventClass",
            "event", "onMessage",
            "script", "gotcha!");

        // Start the Bot
        page = IRCBotPage.gotoPage();
        page.clickActionButton();

        // Verify that the Bot is started
        Assert.assertTrue(page.isBotStarted());

        // Verify that our Bot listener is listed and started
        //TODO

        // Create a new page to verify that a message is sent to the IRC channel.
        // We thus test the IRC Event Listener.
        getUtil().createPage(getTestClassName(), triggerPageName, "whatever", "title");

        // We verify indirectly that the message was sent to the IRC channel by verifying that the Log Bot Listener
        // has archived the message. This also allows testing the Log Bot Listener.
        ViewPage archivePage = getUtil().gotoPage("IRC", "testarchive");

        // TODO: Need to wait till the content we expect is there
        String content = archivePage.getContent();

        // TODO: add the wait and assert the content of the archive

        // Stop the Bot
        page = IRCBotPage.gotoPage();
        page.clickActionButton();

        // Verify that the Bot is stopped again
        Assert.assertFalse(page.isBotStarted());

        // Verify that our Bot is listed and stopped
        //TODO

        // Remove our Bot Listener
        getUtil().deletePage(getTestClassName(), getTestMethodName());

        // Verify that our Bot is no longer listed
        //TODO
    }
}
