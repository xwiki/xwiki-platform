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
package org.xwiki.realtime.wysiwyg.test.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WindowType;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeRichTextAreaElement;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeWYSIWYGEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Multi User functional tests for the Realtime WYSIWYG Editor.
 * 
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0RC1
 */
@UITest(properties = {"xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",}, extraJARs = {
    // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
    "org.xwiki.platform:xwiki-platform-websocket",

    // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
    // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
    "org.xwiki.platform:xwiki-platform-notifications-filters-default",})
class RealtimeWYSIWYGMultiUserIT extends AbstractRealtimeWYSIWYGEditorIT
{
    private static final String TEST_STRING = "Hello from Bob!";

    // Note: There is currently no solution to have multiple browser sessions running at the same time with Selenium.
    // The following tests take advantage of the fact that the user doesn't get disconnected of a realtime session
    // when logging out on another tab.

    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        // When working with different users during tests, there is a risk to end up logged in any account.
        // We make sure that we are logged-in as alice when starting each test.
        setup.createUserAndLogin("alice", "pass", "editor", "Wysiwyg");
    }

    @Test
    @Order(1)
    void noLockWarningSameEditor(TestUtils setup, TestReference testReference)
    {
        //
        // First Tab
        //

        // We are already logged-in as alice, edit the page as alice, effectively locking it.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();
        RealtimeRichTextAreaElement firstTextArea = firstEditor.getRichTextArea();

        //
        // Second Tab
        //
        setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Log in as bob. This will log alice out but will not release the lock nor terminate her realtime session.
        setup.createUserAndLogin("bob", "pass", "editor", "Wysiwyg");

        // Edit the same page as bob, alice still has the lock, but because she is active in the realtime session
        // no warning message should be handled.
        RealtimeWYSIWYGEditPage secondEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);

        // If we get the editor, that means there was no warning.
        RealtimeCKEditor secondEditor = secondEditPage.getContenEditor();

        // Write some text to check that we joined the same session.
        RealtimeRichTextAreaElement secondTextArea = secondEditor.getRichTextArea();
        secondTextArea.sendKeys(TEST_STRING);

        //
        // First Tab
        //

        setup.getDriver().switchTo().window(firstTabHandle);
        firstTextArea.waitUntilTextContains(TEST_STRING);

    }

    @Test
    @Order(2)
    void lockWarningSameEditor(TestUtils setup, TestReference testReference)
    {
        //
        // First Tab
        //

        // We are already logged-in as alice, edit the page as alice, effectively locking it.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        firstEditPage.getContenEditor();

        // Leaving the realtime session should not release the lock.
        firstEditPage.leaveRealtimeEditing();

        //
        // Second Tab
        //
        setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Log in as bob. This will log alice out but will not release the lock.
        setup.createUserAndLogin("bob", "pass", "editor", "Wysiwyg");

        // Edit the same page as bob, alice still has the lock, but because she is not active in the realtime session
        // a warning message should appear.
        setup.gotoPage(testReference, "edit", "editor=wysiwyg");

        // Check that we did not get to the edit page.
        assertFalse(setup.isInWYSIWYGEditMode());
    }

    @Test
    @Order(3)
    void lockWarningWysiwygAndWikiEditors(TestUtils setup, TestReference testReference)
    {
        //
        // First Tab
        //

        // We are already logged-in as alice, edit the page as alice, effectively locking it.
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();

        //
        // Second Tab
        //
        setup.getDriver().switchTo().newWindow(WindowType.TAB).getWindowHandle();

        // Log in as bob. This will log alice out but will not release the lock nor terminate her realtime session.
        setup.createUserAndLogin("bob", "pass", "editor", "Wysiwyg");

        // Edit the same page as bob, alice still has the lock, but because she is using a different editor
        // a warning message should appear.
        setup.gotoPage(testReference, "edit", "editor=wiki");

        // Check that we did not get to the edit page.
        assertFalse(setup.isInWYSIWYGEditMode());
    }

}
