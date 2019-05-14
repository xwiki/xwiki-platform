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
package org.xwiki.test.ui.appwithinminutes;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.AppWithinMinutesHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationsLiveTableElement;
import org.xwiki.appwithinminutes.test.po.ClassFieldEditPane;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;

/**
 * Tests the live table that lists the existing applications on the AppWithinMinutes home page.
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class AppsLiveTableTest extends AbstractTest
{
    /**
     * The page being tested.
     */
    private AppWithinMinutesHomePage homePage;

    @Before
    public void setUp()
    {
        // Register a simple user, login and go to the AppWithinMinutes home page.
        String userName = RandomStringUtils.randomAlphanumeric(5);
        String password = RandomStringUtils.randomAlphanumeric(6);
        getUtil().createUserAndLogin(userName, password);
        homePage = AppWithinMinutesHomePage.gotoPage();
    }

    /**
     * Creates an application and deletes it using the Actions column from the applications live table.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDeleteApplication()
    {
        // Create the application.
        String appName = RandomStringUtils.randomAlphabetic(6);
        createApplication(appName);

        // Check the the applications live table lists the created application.
        ApplicationsLiveTableElement appsLiveTable = homePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        Assert.assertTrue(appsLiveTable.hasColumn("Actions"));
        appsLiveTable.filterApplicationName(appName.substring(0, 3));
        Assert.assertTrue(appsLiveTable.isApplicationListed(appName));

        // Click the delete icon then cancel the confirmation.
        appsLiveTable.clickDeleteApplication(appName).clickNo();
        // We should be taken back to the AppWithinMinutes home page.
        homePage = new AppWithinMinutesHomePage();
        appsLiveTable = homePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        // The application name filter should've been preserved.
        Assert.assertEquals(appName.substring(0, 3), appsLiveTable.getApplicationNameFilter());

        // Click the delete icon again and this confirm the action.
        appsLiveTable.clickDeleteApplication(appName).clickYes();
        // We should be taken back to the AppWithinMinutes home page.
        homePage = new AppWithinMinutesHomePage();
        appsLiveTable = homePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        // The application name filter should've been preserved.
        Assert.assertEquals(appName.substring(0, 3), appsLiveTable.getApplicationNameFilter());
        // And the deleted application shouldn't be listed anymore.
        Assert.assertFalse(appsLiveTable.isApplicationListed(appName));
    }

    /**
     * Creates an application and edits it using the Actions column from the applications live table.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testEditApplication()
    {
        // Create the application.
        String appName = RandomStringUtils.randomAlphabetic(6);
        createApplication(appName);

        // Edit the application.
        ApplicationsLiveTableElement appsLiveTable = homePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        ApplicationClassEditPage classEditor = appsLiveTable.clickEditApplication(appName);

        // Edit the existing class field.
        ClassFieldEditPane fieldEditPane = new ClassFieldEditPane("shortText1");
        fieldEditPane.setPrettyName("City Name");
        fieldEditPane.openConfigPanel();
        fieldEditPane.setName("cityName");

        // Move to the next step.
        ApplicationHomeEditPage homeEditPage = classEditor.clickNextStep().clickNextStep();
        homeEditPage.setDescription("demo");

        // Finish editing.
        ApplicationHomePage homePage = homeEditPage.clickFinish();
        Assert.assertTrue(homePage.getContent().contains("demo"));
    }

    /**
     * Tests that the actions are displayed only when the current user has the right to perform them.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testActionRights()
    {
        // Create the application.
        String appName = RandomStringUtils.randomAlphabetic(6);
        createApplication(appName);

        // The application author should be able to edit and delete the application.
        ApplicationsLiveTableElement appsLiveTable = homePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        appsLiveTable.filterApplicationName(appName);
        Assert.assertTrue(appsLiveTable.canEditApplication(appName));
        Assert.assertTrue(appsLiveTable.canDeleteApplication(appName));

        // Logout. Guests shouldn't be able to edit nor delete the application.
        homePage.logout();
        getUtil().recacheSecretToken();
        homePage = new AppWithinMinutesHomePage();
        appsLiveTable = homePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        appsLiveTable.filterApplicationName(appName);
        Assert.assertFalse(appsLiveTable.canEditApplication(appName));
        Assert.assertFalse(appsLiveTable.canDeleteApplication(appName));

        // Login with a different user. The new user shouldn't be able to delete the application.
        getUtil().createUserAndLogin("someOtherUser", "somePassword");
        appsLiveTable = AppWithinMinutesHomePage.gotoPage().getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        appsLiveTable.filterApplicationName(appName);
        Assert.assertTrue(appsLiveTable.canEditApplication(appName));
        Assert.assertFalse(appsLiveTable.canDeleteApplication(appName));

        this.validateConsole.getLogCaptureConfiguration().registerExpected("WikiComponentException: Registering UI "
            + "extensions at wiki level requires wiki administration rights");
    }

    /**
     * Creates an application with the specified name. The application class will have just one field.
     * 
     * @param appName the name of the application to create
     */
    private void createApplication(String appName)
    {
        ApplicationCreatePage appCreatePage = homePage.clickCreateApplication();
        appCreatePage.setApplicationName(appName);
        appCreatePage.waitForApplicationNamePreview();
        ApplicationClassEditPage classEditPage = appCreatePage.clickNextStep();
        classEditPage.addField("Short Text");
        classEditPage.clickNextStep().clickNextStep().clickFinish();
        homePage = AppWithinMinutesHomePage.gotoPage();
    }
}
