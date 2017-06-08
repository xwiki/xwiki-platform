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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationCreatePage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomeEditPage;
import org.xwiki.appwithinminutes.test.po.ApplicationHomePage;
import org.xwiki.appwithinminutes.test.po.ClassFieldEditPane;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.appwithinminutes.test.po.EntryNamePane;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.test.ui.po.editor.wysiwyg.RichTextAreaElement;

/**
 * Tests the special document fields available in the class editor, such as Title and Content.
 * 
 * @version $Id$
 * @since 4.5RC1
 */
public class DocumentFieldsTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    @Test
    public void titleAndContent()
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
        RichTextAreaElement contentTextArea = entryEditPage.getContentEditor().waitToLoad().getRichTextArea();
        Assert.assertEquals("13", entryEditPage.getValue("number1"));
        // The page name is used as the default value for the title field.
        Assert.assertEquals("Test", entryEditPage.getDocumentTitle());
        Assert.assertEquals("Test", entryEditPage.getTitle());
        entryEditPage.setTitle("Foo");
        Assert.assertEquals(defaultContent, contentTextArea.getText());
        contentTextArea.setContent("Bar");

        // Check that the title and the content of the entry have been updated.
        ViewPage entryViewPage = entryEditPage.clickSaveAndView();
        Assert.assertEquals("Foo", entryViewPage.getDocumentTitle());
        Assert.assertTrue(entryViewPage.getContent().contains("Bar"));
        entryViewPage.clickBreadcrumbLink(appName);

        // Check the entries live table.
        LiveTableElement liveTable = new ApplicationHomePage().getEntriesLiveTable();
        liveTable.waitUntilReady();
        Assert.assertEquals(1, liveTable.getRowCount());
        Assert.assertTrue(liveTable.hasRow("My Title", "Foo"));
        Assert.assertTrue(liveTable.hasRow("My Content", "Bar"));

        // Check that the title and the content of the class have not been changed.
        getUtil().gotoPage(new LocalDocumentReference(Arrays.asList(appName, "Code"), appName + "Class"), "edit",
            "editor=wiki");
        WikiEditPage editPage = new WikiEditPage();
        Assert.assertEquals(appName + " Class", editPage.getTitle());
        Assert.assertEquals("", editPage.getContent());

        // Now edit the class and check if the default values for title and content are taken from the template.
        editPage.editInline();
        Assert.assertEquals(defaultTitle, new ClassFieldEditPane("title1").getDefaultValue());
        Assert.assertEquals(defaultContent, new ClassFieldEditPane("content1").getDefaultValue());
    }
}
