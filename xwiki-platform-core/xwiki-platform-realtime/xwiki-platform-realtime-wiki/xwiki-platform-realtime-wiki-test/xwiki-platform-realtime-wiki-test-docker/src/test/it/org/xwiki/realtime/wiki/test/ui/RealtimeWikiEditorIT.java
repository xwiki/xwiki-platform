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
package org.xwiki.realtime.wiki.test.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WindowType;
import org.xwiki.realtime.test.RealtimeTestUtils;
import org.xwiki.realtime.test.po.RealtimeEditToolbar;
import org.xwiki.realtime.wiki.test.po.RealtimeWikiEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Functional tests for the real-time Wiki editor.
 * 
 * @version $Id$
 * @since 15.5.4
 * @since 15.10
 */
@UITest(extraJARs = {
    // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
    "org.xwiki.platform:xwiki-platform-websocket",})
class RealtimeWikiEditorIT
{
    protected static String firstTabHandle;

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        firstTabHandle = setup.getDriver().getWindowHandle();

        setup.loginAsSuperAdmin();

        // We need to set the realtime wiki editor as the default wiki editor.
        setup.addObject("XWiki", "XWikiPreferences", "XWiki.EditorBindingClass", "dataType",
            "org.xwiki.rendering.syntax.SyntaxContent#text", "roleHint", "realtime-wiki");

        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wiki", "usertype", "Advanced");
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        // Close all tabs except the first one.
        setup.getDriver().getWindowHandles().stream().filter(handle -> !handle.equals(firstTabHandle))
            .forEach(handle -> setup.getDriver().switchTo().window(handle).close());

        // Switch back to the first tab.
        setup.getDriver().switchTo().window(firstTabHandle);
    }

    @Test
    @Order(1)
    void toggleRealtimeWithSelf(TestReference testReference, TestUtils setup)
    {
        // First tab
        RealtimeWikiEditPage firstRtWikiEditor = RealtimeWikiEditPage.gotoPage(testReference);
        String firstUserId = firstRtWikiEditor.getToolbar().getUserId();
        firstRtWikiEditor.sendKeys("one");

        // Second tab
        String secondTabHandle = setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();
        RealtimeWikiEditPage secondRtWikiEditor = RealtimeWikiEditPage.gotoPage(testReference);
        String secondUserId = secondRtWikiEditor.getToolbar().getUserId();

        // We need to wait until the two tabs are connected.
        secondRtWikiEditor.getToolbar().waitForCoeditor(firstUserId);
        secondRtWikiEditor.waitUntilContentContains("one");
        secondRtWikiEditor.sendKeys(Keys.END, Keys.ENTER, "two");

        // First tab
        setup.getDriver().switchTo().window(firstTabHandle);
        firstRtWikiEditor.getToolbar().waitForCoeditor(secondUserId);
        firstRtWikiEditor.waitUntilContentContains("two");

        // Leave realtime editing on both tabs and then join again.
        firstRtWikiEditor.getToolbar().leaveCollaboration();
        firstRtWikiEditor.sendKeys(" 1");

        // Second tab
        setup.getDriver().switchTo().window(secondTabHandle);
        secondRtWikiEditor.getToolbar().leaveCollaboration();
        secondRtWikiEditor.sendKeys(" 2");

        // First tab
        setup.getDriver().switchTo().window(firstTabHandle);
        try {
            firstRtWikiEditor.waitUntilContentContains("2");
            fail("The user left the realtime session but remote changes were still applied.");
        } catch (Exception e) {
            assertEquals("one 1\ntwo", firstRtWikiEditor.getExactContent());
        }

        // Joining back the realtime session reloads the page currently.
        firstUserId = firstRtWikiEditor.getToolbar().joinCollaboration().getUserId();
        firstRtWikiEditor = new RealtimeWikiEditPage();
        firstRtWikiEditor.sendKeys("end");

        // Second tab
        setup.getDriver().switchTo().window(secondTabHandle);
        try {
            secondRtWikiEditor.waitUntilContentContains("1");
            fail("The user left the realtime session but remote changes were still applied.");
        } catch (Exception e) {
            assertEquals("one\ntwo 2", secondRtWikiEditor.getExactContent());
        }

        // Joining back the realtime session reloads the page currently.
        secondUserId = secondRtWikiEditor.getToolbar().joinCollaboration().getUserId();
        secondRtWikiEditor = new RealtimeWikiEditPage();
        secondRtWikiEditor.waitUntilContentContains("end");
        secondRtWikiEditor.sendKeys(Keys.END, Keys.ENTER, "finish");

        // The coeditors list should appear again.
        secondRtWikiEditor.getToolbar().waitForCoeditor(firstUserId);

        // First tab
        setup.getDriver().switchTo().window(firstTabHandle);
        firstRtWikiEditor.getToolbar().waitForCoeditor(secondUserId);
        firstRtWikiEditor.waitUntilContentContains("finish");

        assertEquals("end\nfinish", firstRtWikiEditor.getExactContent());
    }

    @Test
    @Order(2)
    void failGracefullyIfWeCannotConnect(TestUtils setup, TestReference testReference) throws Exception
    {
        RealtimeTestUtils.simulateFailedWebSocketConnection(setup, () -> {
            // Start fresh.
            setup.deletePage(testReference);

            loginAsAlice(setup);

            WikiEditPage editPage = WikiEditPage.gotoPage(testReference);
            editPage.waitForNotificationErrorMessage("Failed to join the realtime collaboration.");

            RealtimeEditToolbar realtimeToolbar = new RealtimeEditToolbar();
            assertFalse(realtimeToolbar.isCollaborating());
            assertFalse(realtimeToolbar.canJoinCollaboration());

            editPage.setContent("edited alone");
            ViewPage viewPage = editPage.clickSaveAndView();
            assertEquals("edited alone", viewPage.getContent());

            return null;
        });
    }

    private void loginAsAlice(TestUtils setup)
    {
        setup.login("alice", "pa$$word");
    }
}
