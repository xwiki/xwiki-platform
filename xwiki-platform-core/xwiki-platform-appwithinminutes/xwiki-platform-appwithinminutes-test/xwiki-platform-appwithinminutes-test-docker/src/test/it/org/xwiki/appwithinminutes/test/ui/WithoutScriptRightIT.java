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
package org.xwiki.appwithinminutes.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.AppWithinMinutesHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationsLiveTableElement;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests AWM without script right.
 *
 * @version $Id$
 * @since 13.10.11
 * @since 14.4.8
 * @since 14.10.1
 * @since 15.0RC1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since 
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class WithoutScriptRightIT
{
    private static final String USER_NAME = "NoScript";

    private static final String PASSWORD = "password";

    private static final String NO_SCRIPT_ERROR = "You don't have the script right which is necessary to create or "
        + "edit an application. The application might not work correctly when you continue.";

    @BeforeAll
    static void beforeAll(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createUserAndLogin(USER_NAME, PASSWORD, "");
    }

    @Order(1)
    @Test
    void createApplicationWithoutScriptRight(TestReference testReference)
    {
        ApplicationCreatePage appCreatePage = AppWithinMinutesHomePage.gotoPage().clickCreateApplication();
        String appName = testReference.getLastSpaceReference().getName();
        appCreatePage.setApplicationName(appName);
        appCreatePage.waitForApplicationNamePreview();
        assertTrue(appCreatePage.getContent().contains(NO_SCRIPT_ERROR));

        ApplicationClassEditPage classEditPage = appCreatePage.clickNextStep();
        classEditPage.addField("Short Text");
        ApplicationHomeEditPage applicationHomeEditPage = classEditPage.clickNextStep().clickNextStep();
        assertTrue(applicationHomeEditPage.getContent().contains(NO_SCRIPT_ERROR));

        ApplicationHomePage applicationHomePage = applicationHomeEditPage.clickFinish();
        assertFalse(applicationHomePage.hasEntriesLiveTable());

        EntryNamePane entryNamePane = applicationHomePage.clickAddNewEntry();
        entryNamePane.setName("Test entry");
        EntryEditPage editPage = entryNamePane.clickAdd();
        // The edit form doesn't work without script right.
        assertTrue(editPage.hasRenderingError());

        // Without script right, no delete is available.
        ApplicationsLiveTableElement liveTable = AppWithinMinutesHomePage.gotoPage().getAppsLiveTable();
        assertTrue(liveTable.isApplicationListed(appName));
        assertFalse(liveTable.canDeleteApplication(appName));
    }
}
