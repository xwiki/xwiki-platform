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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WindowType;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeCKEditor;
import org.xwiki.realtime.wysiwyg.test.po.RealtimeWYSIWYGEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Multi User functional tests for the Realtime WYSIWYG Editor.
 * 
 * @version $Id$
 * @since 16.2.0
 * @since 16.3.0RC1
 */
@UITest(properties = {"xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",}, extraJARs = {
    // The WebSocket end-point implementation based on XWiki components needs to be installed as core extension.
    "org.xwiki.platform:xwiki-platform-websocket",

    // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
    // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
    "org.xwiki.platform:xwiki-platform-notifications-filters-default",})
class RealtimeWYSIWYGMultiUserIT extends AbstractRealtimeWYSIWYGMultiUserIT
{
    // Note: There is currently no solution to have multiple browser sessions running at the same time with Selenium.
    // The following tests take advantage of the fact that the user doesn't get disconnected of a realtime session
    // when logging out on another tab.

    @Test
    @Order(1)
    void noLockWarning(TestUtils setup, TestReference testReference)
    {
        //
        // First Tab
        //

        // Edit the page as alice, effectively locking it.
        setup.createUserAndLogin("alice", "pass", "editor", "Wysiwyg");
        RealtimeWYSIWYGEditPage firstEditPage = RealtimeWYSIWYGEditPage.gotoPage(testReference);
        RealtimeCKEditor firstEditor = firstEditPage.getContenEditor();

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
        secondEditPage.getContenEditor();
    }

    @Test
    @Order(2)
    void lockWarning(TestUtils setup, TestReference testReference)
    {
        //
        // First Tab
        //

        // Edit the page as alice, effectively locking it.
        setup.createUserAndLogin("alice", "pass", "editor", "Wysiwyg");
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
        RealtimeWYSIWYGEditPage.gotoPage(testReference);

        // Check that we did not get to the edit page.
        assertFalse(setup.isInWYSIWYGEditMode());
    }

}
