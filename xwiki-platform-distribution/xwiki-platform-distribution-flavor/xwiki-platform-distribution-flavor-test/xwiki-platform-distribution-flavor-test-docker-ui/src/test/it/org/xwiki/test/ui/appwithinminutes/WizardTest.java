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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.PagesLiveTableElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the App Within Minutes wizard.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class WizardTest
{
    /**
     * The first step of the wizard.
     */
    private ApplicationCreatePage appCreatePage;

    @BeforeEach
    void setUp(TestUtils testUtils, TestReference testReference)
    {
        // Register a simple user, login and go to the App Within Minutes home page.
        String userName = "SimpleUser";
        String password = "SimplePassword";
        testUtils.createUserAndLogin(userName, password);
        // Make sure the application location exists so that we can select it with the location picker.
        testUtils.createPage(Arrays.asList(getClass().getSimpleName(), testReference.getName()), "WebHome", null,
            null);
        AppWithinMinutesHomePage appWithinMinutesHomePage = AppWithinMinutesHomePage.gotoPage();

        // Click the Create Application button.
        this.appCreatePage = appWithinMinutesHomePage.clickCreateApplication();
    }

    /**
     * Tests the application creation process from start to end.
     */
    @Test
    @Order(1)
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    void testCreateApplication(TestUtils testUtils, TestReference testReference)
    {
        // Step 1
        // Set the application location.
        this.appCreatePage.getLocationPicker().browseDocuments();
        new DocumentPickerModal().selectDocument(getClass().getSimpleName(), testReference.getName(), "WebHome");
        this.appCreatePage.getLocationPicker().waitForLocation(
            Arrays.asList("", getClass().getSimpleName(), testReference.getName(), ""));

        // Enter the application name, making sure we also use some special chars.
        // See XWIKI-11747: Impossible to create new entry with an application having UTF8 chars in its name
        String appName = "Cities âé";
        String[] appPath = new String[] {getClass().getSimpleName(), testReference.getName(), appName};
        this.appCreatePage.setApplicationName(appName);

        // Wait for the preview.
        this.appCreatePage.waitForApplicationNamePreview();

        // Move to the next step.
        ApplicationClassEditPage classEditPage = this.appCreatePage.clickNextStep();

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
        assertTrue(homePage.getContent().contains(appDescription));

        // Add a new entry.
        String firstEntryName = "City 1 âé";
        EntryNamePane entryNamePane = homePage.clickAddNewEntry();
        entryNamePane.setName(firstEntryName);
        EntryEditPage entryEditPage = entryNamePane.clickAdd();

        // Assert the pretty name and the default value of the Short Text field.
        // Apparently WebElement#getText() takes into account the text-transform CSS property.
        assertEquals("CITY NAME", entryEditPage.getLabel("cityName"));
        assertEquals("Paris", entryEditPage.getValue("cityName"));

        // Change the field value.
        entryEditPage.setValue("cityName", "London");

        // Save and go back to the application home page.
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);
        homePage = new ApplicationHomePage();

        // Assert the entry we have just created is listed in the live table.
        LiveTableElement entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        assertTrue(entriesLiveTable.hasRow("City Name", "London"));

        // Assert that only the entry we have just created is listed as child of the application home page. The rest of
        // the documents (class, template, sheet, preferences) should be marked as hidden.
        PagesLiveTableElement childrenLiveTable = homePage.viewChildren().getLiveTable();
        childrenLiveTable.waitUntilReady();
        assertEquals(1, childrenLiveTable.getRowCount());
        assertTrue(childrenLiveTable.hasPageWithTitle(firstEntryName));

        // Go back to the application home edit page.
        testUtils.gotoPage(
            Arrays.asList(getClass().getSimpleName(), testReference.getName(), appName)
            , "WebHome", "edit", "");
        homeEditPage = new ApplicationHomeEditPage();

        // Change the application description.
        appDescription = "The best app!";
        homeEditPage.setDescription(appDescription);

        // Remove one of the live table columns.
        homeEditPage.removeLiveTableColumn("Actions");

        // Save
        homePage = homeEditPage.clickSaveAndView();

        // Assert that the application description has changed and that the column has been removed.
        assertTrue(homePage.getContent().contains(appDescription));
        entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        assertFalse(entriesLiveTable.hasColumn("Actions"));

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
        assertEquals("POPULATION SIZE", entryEditPage.getLabel("number1"));

        // Save and go back to the application home page.
        entryEditPage.clickSaveAndView().clickBreadcrumbLink(appName);
        homePage = new ApplicationHomePage();

        // Assert both entries are displayed in the live table.
        entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        assertTrue(entriesLiveTable.hasRow("Page Title", firstEntryName));
        assertTrue(entriesLiveTable.hasRow("Page Title", secondEntryName));

        // Go to the App Within Minutes home page.
        AppWithinMinutesHomePage appWithinMinutesHomePage = AppWithinMinutesHomePage.gotoPage();

        // Assert that the created application is listed in the live table.
        ApplicationsLiveTableElement appsLiveTable = appWithinMinutesHomePage.getAppsLiveTable();
        assertTrue(appsLiveTable.isApplicationListed(appName));

        // Delete the application entries.
        homePage = appsLiveTable.viewApplication(appName);
        assertEquals('/' + StringUtils.join(appPath, '/'), homePage.getBreadcrumbContent());
        homePage.clickDeleteAllEntries().clickYes();
        // Verify that the entries live table is empty.
        entriesLiveTable = homePage.getEntriesLiveTable();
        entriesLiveTable.waitUntilReady();
        assertEquals(0, entriesLiveTable.getRowCount());

        // Delete the application.
        homePage.clickDeleteApplication().clickYes();
        // Verify that the application is not listed anymore.
        appsLiveTable = AppWithinMinutesHomePage.gotoPage().getAppsLiveTable();
        assertFalse(appsLiveTable.isApplicationListed(appName));


        // TODO: how to do that with docker tests?
        // this.validateConsole.getLogCaptureConfiguration().registerExcludes("WikiComponentException: Registering UI "
        //     + "extensions at wiki level requires wiki administration rights");
    }

    /**
     * @see XWIKI-7380: Cannot go back from step 2 to step 1
     */
    @Test
    @Order(2)
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    void testGoBackToFirstStep()
    {
        // Step 1
        String appName = "Empty App âé";
        this.appCreatePage.setApplicationName(appName);
        this.appCreatePage.waitForApplicationNamePreview();

        // Step 2
        ApplicationClassEditPage classEditPage = this.appCreatePage.clickNextStep();
        classEditPage.addField("Short Text");

        // Back to Step 1
        this.appCreatePage = classEditPage.clickPreviousStep();
        this.appCreatePage.setApplicationName(appName);
        this.appCreatePage.waitForApplicationNamePreview();
        // Test that the application wasn't created.
        assertFalse(this.appCreatePage.getContent().contains(ApplicationNameTest.APP_NAME_USED_WARNING_MESSAGE));

        // Step 2 again
        classEditPage = this.appCreatePage.clickNextStep();
        assertTrue(classEditPage.getContent().contains(ClassEditorTest.EMPTY_CANVAS_HINT));
        classEditPage.addField("Number");

        // Step 4 and back to Step 2
        classEditPage = classEditPage.clickNextStep().clickNextStep().clickPreviousStep().clickPreviousStep();
        assertFalse(classEditPage.getContent().contains(ClassEditorTest.EMPTY_CANVAS_HINT));
        assertFalse(classEditPage.hasPreviousStep());
    }
}
