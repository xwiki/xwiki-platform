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

import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.ClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.EditablePropertyPane;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the special document fields available in the class editor, such as Title and Content.
 * 
 * @version $Id$
 * @since 12.10.11
 * @since 13.4.6
 * @since 13.10RC1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"})
class DocumentFieldsIT
{
    private String appName = RandomStringUtils.randomAlphabetic(6);

    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        // By default the minimal distribution used for the tests doesn't have any rights setup. Let's create an Admin
        // user part of the Admin Group and make sure that this Admin Group has admin rights in the wiki. We could also
        // have given that Admin user the admin right directly but the solution we chose is closer to the XS
        // distribution.
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createAdminUser();
        setup.loginAsAdmin();
    }

    @Test
    @Order(1)
    void titleAndContent(TestUtils setup)
    {
        // Create a new application.
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        appCreatePage.setApplicationName(this.appName);
        ApplicationClassEditPage classEditPage = appCreatePage.clickNextStep();

        // Add a standard field.
        ClassFieldEditPane numberField = classEditPage.addField("Number");

        // Add the Title and Content fields.
        ClassFieldEditPane titleField = classEditPage.addField("Title");
        ClassFieldEditPane contentField = classEditPage.addField("Content");

        // Change the default field pretty names.
        // See XWIKI-9154: The application live table uses the standard 'Page title' heading instead of the pretty name
        // set for the Title field
        titleField.setPrettyName("My Title");
        contentField.setPrettyName("My Content");

        // Set the default values that will be saved in the template.
        numberField.setDefaultValue("13");
        String defaultTitle = "Enter title here";
        titleField.setDefaultValue(defaultTitle);
        String defaultContent = "Enter content here";
        contentField.setDefaultValue(defaultContent);

        // Add live table columns for Title and Content.
        ApplicationHomeEditPage homeEditPage = classEditPage.clickNextStep().clickNextStep();
        homeEditPage.addLiveTableColumn("My Title");
        homeEditPage.addLiveTableColumn("My Content");

        // Add an application entry.
        EntryNamePane entryNamePane = homeEditPage.clickFinish().clickAddNewEntry();
        entryNamePane.setName("Test");
        EntryEditPage entryEditPage = entryNamePane.clickAdd();
        assertEquals("13", entryEditPage.getValue("number1"));
        // The page name is used as the default value for the title field.
        assertEquals("Test", entryEditPage.getDocumentTitle());
        assertEquals("Test", entryEditPage.getTitle());
        entryEditPage.setTitle("Foo");
        assertEquals(defaultContent, entryEditPage.getContent());
        entryEditPage.setContent("Bar");

        // Check that the title and the content of the entry have been updated.
        ViewPage entryViewPage = entryEditPage.clickSaveAndView();
        assertEquals("Foo", entryViewPage.getDocumentTitle());
        assertTrue(entryViewPage.getContent().contains("Bar"));

        // Verify that we can edit the document fields in-place.
        String propertyReference = String.format("%s.Code.%1$sClass[0].title1", this.appName);
        EditablePropertyPane<String> titleProperty = new EditablePropertyPane<>(propertyReference);
        assertEquals("Foo", titleProperty.clickEdit().getValue());
        titleProperty.setValue("Book").clickSave();
        assertEquals("Book", titleProperty.getDisplayValue());

        // Check the entries live table.
        entryViewPage.clickBreadcrumbLink(this.appName);
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("My Title", "Book"));
        assertTrue(liveTable.hasRow("My Content", "Bar"));

        // Check that the title and the content of the class have not been changed.
        setup.gotoPage(new LocalDocumentReference(Arrays.asList(this.appName, "Code"), this.appName + "Class"), "edit",
            "editor=wiki");
        WikiEditPage editPage = new WikiEditPage();
        assertEquals(this.appName + " Class", editPage.getTitle());
        assertEquals("", editPage.getContent());

        // Now edit the class and check if the default values for title and content are taken from the template.
        editPage.edit();
        assertEquals(defaultTitle, new ClassFieldEditPane("title1").getDefaultValue());
        assertEquals(defaultContent, new ClassFieldEditPane("content1").getDefaultValue());
    }

    @Test
    @Order(2)
    void contentFromSimpleUser(TestUtils setup)
    {
        // Create an application entry with a simple user that doesn't have script rights.
        setup.createUserAndLogin("Alice", "pass");

        ApplicationHomePage appHomePage = ApplicationHomePage.gotoPage(this.appName);
        appHomePage.getEntriesLiveTable().waitUntilReady();

        EntryNamePane entryNamePage = appHomePage.clickAddNewEntry();
        entryNamePage.setName("ByAlice");

        EntryEditPage entryEditPage = entryNamePage.clickAdd();
        entryEditPage.setTitle("Title by $services.localization.render('Alice')");
        entryEditPage.setContent("Content by {{velocity}}$services.localization.render('Alice'){{/velocity}}");

        ViewPage entryViewPage = entryEditPage.clickSaveAndView();
        assertEquals("Title by $services.localization.render('Alice')", entryViewPage.getDocumentTitle());
        assertTrue(entryViewPage.getContent().contains("Content by"));
        assertTrue(entryViewPage.getContent().contains("The execution of the [velocity] script macro is not allowed"));
    }
}
