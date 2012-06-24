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
import org.xwiki.ircbot.test.po.IRCConfigurationPage;
import org.xwiki.ircbot.test.po.IRCBotPage;
import org.xwiki.ircbot.test.po.WebHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;

import junit.framework.Assert;

/**
 * UI tests for the IRC Bot feature.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class IRCBotTest extends AbstractTest
{
    private static final String ARCHIVE_PAGE = "IRCArchive";

    private static final String SERVER_PAGE = "IRCServer";

    private static final String LISTENER_PAGE = "TestListener";

    @Before
    public void setUp()
    {
        // Login as superadmin to have delete rights.
        getDriver().get(getUtil().getURLToLoginAs("superadmin", "pass"));
        getUtil().recacheSecretToken();

        // Verify that the Bot is stopped, if not, stop it
        // We do this as the first thing since otherwise events could be sent to the bot which would make our
        // assertions below false.
        IRCBotPage page = IRCBotPage.gotoPage();
        if (page.isBotStarted()) {
            page.clickActionButton();
        }
        Assert.assertFalse(page.isBotStarted());

        // Delete pages that we create in the test
        getUtil().deletePage(getTestClassName(), LISTENER_PAGE);
        getUtil().deletePage(getTestClassName(), SERVER_PAGE);
        getUtil().deletePage(getTestClassName(), ARCHIVE_PAGE);

        // Configure the Logging Bot Listener to log into a fixed name page so that we can easily delete that page at
        // the test start and find it easily to assert its content below.
        IRCConfigurationPage configPage = IRCConfigurationPage.gotoPage();
        configPage.setLoggingPage(getTestClassName() + "." + ARCHIVE_PAGE);
    }

    @Test
    public void testBot()
    {
        // Add a Test Bot Listener
        getUtil().addObject(getTestClassName(), LISTENER_PAGE, "IRC.IRCBotListenerClass",
            "description", "bot listener test",
            "name", "Test");
        getUtil().addObject(getTestClassName(), LISTENER_PAGE, "IRC.IRCBotListenerEventClass",
            "event", "onMessage",
            "script", "gotcha!");

        // Start the Bot
        IRCBotPage page = IRCBotPage.gotoPage();
        // Note that starting the Bot will generate a Join Event and thanks to the Log Bot Listener, the following
        // will be logged in the archive page: "<XWikiBotTest> has joined #xwikitest"
        page.clickActionButton();

        // Verify that the Bot is started
        Assert.assertTrue(page.isBotStarted());

        // Verify that our Bot listener is listed and started
        Assert.assertTrue(page.isBotListenerStarted("Test"));

        // Verify that the other default Bot Listeners are listed and started too
        Assert.assertTrue(page.isBotListenerStarted("Log"));
        Assert.assertTrue(page.isBotListenerStarted("Help"));
        Assert.assertTrue(page.isBotListenerStarted("AutoReconnect"));

        // Create a new page for 2 reasons:
        // - It allows us testing the Document Modified Event Listener since a message will be sent to the IRC Channel
        // - In the content of this page we get access to the list of messages sent to the IRC channel and we can
        //    verify that the Document Modified Event Listener has sent the correct message to the channel
        final ViewPage messagesPage = getUtil().createPage(getTestClassName(), SERVER_PAGE,
              "{{velocity}}\n"
            + "#if (\"$!request.message\" != \"\")\n"
            + "  #set ($discard = $services.ircserver.typeInChannel($request.message))\n"
            + "#else\n"
            + "  #foreach ($message in $services.ircserver.messages)\n"
            + "    $message\n"
            + "  #end\n"
            + "#end\n"
            + "{{/velocity}}", "title");
        messagesPage.waitUntilContent("xwiki:" + getTestClassName() + "\\." + SERVER_PAGE + " was modified by "
                    + "xwiki:XWiki\\.superadmin \\(created\\) - .*" + getTestClassName() + "/" + SERVER_PAGE);

        // Simulate typing some content in the IRC Channel. This will have two effects:
        // - the message will be logged
        // - our test listener will send "gotcha!" to the IRC channel
        getUtil().gotoPage(getTestClassName(), SERVER_PAGE, "view", "message", "hello");
        getUtil().gotoPage(getTestClassName(), SERVER_PAGE);
        // Note: We don't assume any order for the 3 messages we're asserting.
        messagesPage.waitUntilContent(".*nick: gotcha!.*");
        Assert.assertTrue(messagesPage.getContent().contains(
            "xwiki:IRCBotTest.IRCServer was modified by xwiki:XWiki.superadmin (created)"));
        Assert.assertTrue(messagesPage.getContent().contains(
            "xwiki:IRCBotTest.IRCArchive was modified by xwiki:XWiki.superadmin (created)"));

        // Verify the Archive page (this tests the Log Bot Listener).
        // It may take some time for the IRC Server to send back the events thus we wait for the archive page to
        // be created.
        final ViewPage archivePage = getUtil().gotoPage(getTestClassName(), ARCHIVE_PAGE);
        archivePage.waitUntilContent("<nick> hello");

        // Stop the Bot
        page = IRCBotPage.gotoPage();
        page.clickActionButton();

        // Verify that the Bot is stopped again
        Assert.assertFalse(page.isBotStarted());

        // Verify that our Bot is listed and stopped. Same for the Help listener. However the other listeners are
        // component-based and cannot be disabled.
        Assert.assertFalse(page.isBotListenerStarted("Test"));
        Assert.assertFalse(page.isBotListenerStarted("Log"));
        Assert.assertTrue(page.isBotListenerStarted("Help"));
        Assert.assertTrue(page.isBotListenerStarted("AutoReconnect"));

        // Remove our Bot Listener
        getUtil().deletePage(getTestClassName(), LISTENER_PAGE);

        // Verify that our Bot is no longer listed
        page = IRCBotPage.gotoPage();
        Assert.assertFalse(page.containsListener("Test"));

        // Go to the IRC Home Page and verify that the Archive Livetable contains our Log Archive document
        WebHomePage homePage = WebHomePage.gotoPage();
        Assert.assertTrue(homePage.getArchiveLiveTable().hasRow("irc.doc.name", ARCHIVE_PAGE));
    }
}
