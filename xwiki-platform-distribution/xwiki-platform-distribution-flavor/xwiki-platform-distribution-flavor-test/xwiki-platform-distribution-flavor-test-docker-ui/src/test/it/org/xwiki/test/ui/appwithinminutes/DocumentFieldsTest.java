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

import org.apache.commons.lang3.RandomStringUtils;
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
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the special document fields available in the class editor, such as Title and Content.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class DocumentFieldsTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    @Test
    @Order(1)
    void titleAndContent(TestUtils testUtils)
    {
        // Create a new application.
        String appName = RandomStringUtils.randomAlphabetic(6);
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        appCreatePage.setApplicationName(appName);
        appCreatePage.waitForApplicationNamePreview();
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
        ApplicationHomeEditPage homeEditPage = classEditPage.clickNextStep().clickNextStep().waitUntilPageIsLoaded();
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
        entryViewPage.clickBreadcrumbLink(appName);

        // Check the entries live table.
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        assertEquals(1, liveTable.getRowCount());
        assertTrue(liveTable.hasRow("My Title", "Foo"));
        assertTrue(liveTable.hasRow("My Content", "Bar"));

        // Check that the title and the content of the class have not been changed.
        testUtils.gotoPage(new LocalDocumentReference(Arrays.asList(appName, "Code"), appName + "Class"), "edit",
            "editor=wiki");
        WikiEditPage editPage = new WikiEditPage();
        assertEquals(appName + " Class", editPage.getTitle());
        assertEquals("", editPage.getContent());

        // Now edit the class and check if the default values for title and content are taken from the template.
        editPage.editInline();
        assertEquals(defaultTitle, new ClassFieldEditPane("title1").getDefaultValue());
        assertEquals(defaultContent, new ClassFieldEditPane("content1").getDefaultValue());
    }
}
