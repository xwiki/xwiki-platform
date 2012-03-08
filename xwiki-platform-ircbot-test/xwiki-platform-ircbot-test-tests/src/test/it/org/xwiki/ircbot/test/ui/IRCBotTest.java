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
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
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

        // Verify that the Bot is stopped, if not, stop it
        // We do this as the first thing since otherwise events could be sent to the bot which would make our
        // assertions below false.
        IRCBotPage page = IRCBotPage.gotoPage();
        if (page.isBotStarted()) {
            page.clickActionButton();
        }
        Assert.assertFalse(page.isBotStarted());

        // Remove the Log listener archive page to start from a clean slate.
        getUtil().deletePage("IRC", "testarchive");

        // Configure the Logging Bot Listener to log into a fixed name page so that we can easily delete that page at
        // the test start and find it easily to assert its content below.
        IRCBotConfigurationPage configPage = IRCBotConfigurationPage.gotoPage();
        configPage.setLoggingPage("IRC.testarchive");

        // Add a Bot Listener
        getUtil().addObject(getTestClassName(), getTestMethodName(), "IRC.IRCBotListenerClass",
            "description", "bot listener test",
            "name", "Test");
        getUtil().addObject(getTestClassName(), getTestMethodName(), "IRC.IRCBotListenerEventClass",
            "event", "onMessage",
            "script", "gotcha!");

        // Start the Bot
        page = IRCBotPage.gotoPage();
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

        // Create a new page to verify that a message is sent to the IRC channel.
        // We thus test the IRC Event Listener.
        getUtil().createPage(getTestClassName(), triggerPageName, "whatever", "title");

        // Simulate typing some content in the IRC Channel. This will have two effects:
        // - the message will be logged
        // - our test listener will send "gotcha!" to the IRC channel
        //TODO: Send "hello"

        // Verify the Archive page (this tests the Log Bot Listener).
        // It may take some time for the IRC Server to send back the Join event thus we wait for the archive page to
        // be created.
        final ViewPage archivePage = getUtil().gotoPage("IRC", "testarchive");
        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try {
                    driver.navigate().refresh();
                    return archivePage.getContent().contains("<XWikiBotTest> has joined #xwikitest")
                        && archivePage.getContent().contains("hello");
                } catch (NotFoundException e) {
                    return false;
                } catch (StaleElementReferenceException e) {
                    // The element was removed from DOM in the meantime
                    return false;
                }
            }
        });

        // Verify the messages sent to the IRC Server. There should be 2:
        // - one sent by the Document Event Listener when we have created the new page
        // - one sent by the Test Listener in response to "hello".
        // TODO: verify the messages

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
        getUtil().deletePage(getTestClassName(), getTestMethodName());

        // Verify that our Bot is no longer listed
        //TODO
    }
}
