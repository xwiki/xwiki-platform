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

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.AppWithinMinutesHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.ApplicationTemplateProviderEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationsLiveTableElement;
import org.xwiki.appwithinminutes.test.po.ClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.index.tree.test.po.DocumentPickerModal;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.PagesLiveTableElement;

/**
 * Tests the App Within Minutes wizard.
 *
 * @version $Id$
 * @since 3.3
 */
public class WizardTest extends AbstractTest
{
    /**
     * The first step of the wizard.
     */
    private ApplicationCreatePage appCreatePage;

    @Before
    public void setUp()
    {
        // Register a simple user, login and go to the App Within Minutes home page.
        String userName = "SimpleUser";
        String password = "SimplePassword";
        getUtil().createUserAndLogin(userName, password);
        // Make sure the application location exists so that we can select it with the location picker.
        getUtil().createPage(Arrays.asList(getClass().getSimpleName(), this.testName.getMethodName()), "WebHome", null,
            null);
        AppWithinMinutesHomePage appWithinMinutesHomePage = AppWithinMinutesHomePage.gotoPage();

        // Click the Create Application button.
        appCreatePage = appWithinMinutesHomePage.clickCreateApplication();
    }

    /**
     * Tests the application creation process from start to end.
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testCreateApplication()
    {
        // Step 1
        // Set the application location.
        appCreatePage.getLocationPicker().browseDocuments();
        new DocumentPickerModal().selectDocument(getClass().getSimpleName(), this.testName.getMethodName(), "WebHome");
        appCreatePage.getLocationPicker().waitForLocation(
            Arrays.asList("", getClass().getSimpleName(), this.testName.getMethodName(), ""));

        // Enter the application name, making sure we also use some special chars.
        // See XWIKI-11747: Impossible to create new entry with an application having UTF8 chars in its name
        String appName = "Cities âé";
        String[] appPath = new String[] {getClass().getSimpleName(), this.testName.getMethodName(), appName};
        appCreatePage.setApplicationName(appName);

        // Wait for the preview.
        appCreatePage.waitForApplicationNamePreview();

        // Move to the next step.
        ApplicationClassEditPage classEditPage = appCreatePage.clickNextStep();

        // Step 2
        // Add a 'Short Text' field.
        ClassFieldEditPane fieldEditPane = classEditPage.addField("Short Text");

        // Set the field pretty name and default value
        fieldEditPane.setPrettyName("City Name");
        fieldEditPane.setDefaultValue("Paris");

        // Move to the next step.
        ApplicationTemplateProviderEditPage templateProviderEditPage = classEditPage.clickNextStep();

        // Move back to the second step.
        classEditPage = templateProviderEditPage.clickPreviousStep();

        // Open the configuration panel and set the field name
        fieldEditPane = new ClassFieldEditPane("shortText1");
        fieldEditPane.openConfigPanel();
        fieldEditPane.setName("cityName");

        // Move to the next step.
        templateProviderEditPage = classEditPage.clickNextStep();

        // Step 3
        templateProviderEditPage.setIcon("worl");
        templateProviderEditPage.setDescription("A city page");

        // Move to the next step.
        ApplicationHomeEditPage homeEditPage = templateProviderEditPage.clickNextStep().waitUntilPageIsLoaded();

        // Step 4
        // Enter the application description.
        String appDescription = "Simple application to manage data about various cities";
        homeEditPage.setDescription(appDescription);

        // Add the Short Text field from the previous step to the list of columns.
        homeEditPage.addLiveTableColumn("City Name");

        // Click the finish button which should lead us to the application home page.
        ApplicationHomePage homePage = homeEditPage.clickFinish();

        // Assert the application description is present.
        Assert.assertTrue(homePage.getContent().contains(appDescription));

        // Add a new entry.
        String firstEntryName = "City 1 âé";
        EntryNamePane entryNamePane = homePage.clickAddNewEntry();
        entryNamePane.setName(firstEntryName);
        EntryEditPage entryEditPage = entryNamePane.clickAdd();

        // Assert the pretty name and the default value of the Short Text field.
        // Apparently WebElement#getText() takes into account the text-transform CSS property.
        Assert.assertEquals("CITY NAME", entryEditPage.getLabel("cityName"));
        Assert.assertEquals("Paris", entryEditPage.getValue("cityName"));

        // Change the field value.
        entryEditPage.setValue("cityName", "London");

        // Save and go back to the application home page.
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);
        homePage = new ApplicationHomePage();

        // Assert the entry we have just created is listed in the live table.
        LiveTableElement entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        Assert.assertTrue(entriesLiveTable.hasRow("City Name", "London"));

        // Assert that only the entry we have just created is listed as child of the application home page. The rest of
        // the documents (class, template, sheet, preferences) should be marked as hidden.
        PagesLiveTableElement childrenLiveTable = homePage.viewChildren().getLiveTable();
        childrenLiveTable.waitUntilReady();
        Assert.assertEquals(1, childrenLiveTable.getRowCount());
        Assert.assertTrue(childrenLiveTable.hasPageWithTitle(firstEntryName));

        // Go back to the application home page.
        getDriver().navigate().back();

        // Click the edit button.
        homePage.edit();
        homeEditPage = new ApplicationHomeEditPage();

        // Change the application description.
        appDescription = "The best app!";
        homeEditPage.setDescription(appDescription);

        // Remove one of the live table columns.
        homeEditPage.removeLiveTableColumn("Actions");

        // Save
        homePage = homeEditPage.clickSaveAndView();

        // Assert that the application description has changed and that the column has been removed.
        Assert.assertTrue(homePage.getContent().contains(appDescription));
        entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        Assert.assertFalse(entriesLiveTable.hasColumn("Actions"));

        // Click the link to edit the application.
        classEditPage = homePage.clickEditApplication();

        // Drag a Number field.
        fieldEditPane = classEditPage.addField("Number");

        // Set the field pretty name.
        fieldEditPane.setPrettyName("Population Size");

        // Fast forward.
        homeEditPage = classEditPage.clickNextStep().clickNextStep();
        // Just wait for the WYSIWYG editor (which is used for setting the application description) to load so that the
        // page layout is stable before we click on the Finish button.
        homeEditPage.setDescription(appDescription);
        homePage = homeEditPage.clickFinish();

        // Add a new entry.
        String secondEntryName = "City 2 âé";
        entryNamePane = homePage.clickAddNewEntry();
        entryNamePane.setName(secondEntryName);
        entryEditPage = entryNamePane.clickAdd();

        // Assert the new field is displayed in the edit sheet (field name was auto-generated).
        // Apparently WebElement#getText() takes into account the text-transform CSS property.
        Assert.assertEquals("POPULATION SIZE", entryEditPage.getLabel("number1"));

        // Save and go back to the application home page.
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);
        homePage = new ApplicationHomePage();

        // Assert both entries are displayed in the live table.
        entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        Assert.assertTrue(entriesLiveTable.hasRow("Page Title", firstEntryName));
        Assert.assertTrue(entriesLiveTable.hasRow("Page Title", secondEntryName));

        // Go to the App Within Minutes home page.
        AppWithinMinutesHomePage appWithinMinutesHomePage = AppWithinMinutesHomePage.gotoPage();

        // Assert that the created application is listed in the live table.
        ApplicationsLiveTableElement appsLiveTable = appWithinMinutesHomePage.getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        Assert.assertTrue(appsLiveTable.isApplicationListed(appName));

        // Delete the application entries.
        homePage = appsLiveTable.viewApplication(appName);
        Assert.assertEquals('/' + StringUtils.join(appPath, '/'), homePage.getBreadcrumbContent());
        homePage.clickDeleteAllEntries().clickYes();
        // Verify that the entries live table is empty.
        entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        Assert.assertEquals(0, entriesLiveTable.getRowCount());

        // Delete the application.
        homePage.clickDeleteApplication().clickYes();
        // Verify that the application is not listed anymore.
        appsLiveTable = AppWithinMinutesHomePage.gotoPage().getAppsLiveTable();
        appsLiveTable.waitUntilReady();
        Assert.assertFalse(appsLiveTable.isApplicationListed(appName));
    }

    /**
     * @see XWIKI-7380: Cannot go back from step 2 to step 1
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testGoBackToFirstStep()
    {
        // Step 1
        String appName = "Empty App âé";
        appCreatePage.setApplicationName(appName);
        appCreatePage.waitForApplicationNamePreview();

        // Step 2
        ApplicationClassEditPage classEditPage = appCreatePage.clickNextStep();
        classEditPage.addField("Short Text");

        // Back to Step 1
        appCreatePage = classEditPage.clickPreviousStep();
        appCreatePage.setApplicationName(appName);
        appCreatePage.waitForApplicationNamePreview();
        // Test that the application wasn't created.
        Assert.assertFalse(appCreatePage.getContent().contains(ApplicationNameTest.APP_NAME_USED_WARNING_MESSAGE));

        // Step 2 again
        classEditPage = appCreatePage.clickNextStep();
        Assert.assertTrue(classEditPage.getContent().contains(ClassEditorTest.EMPTY_CANVAS_HINT));
        classEditPage.addField("Number");

        // Step 3 and back to Step 2
        classEditPage = classEditPage.clickNextStep().clickPreviousStep();
        Assert.assertFalse(classEditPage.getContent().contains(ClassEditorTest.EMPTY_CANVAS_HINT));
        Assert.assertFalse(classEditPage.hasPreviousStep());
    }
}
