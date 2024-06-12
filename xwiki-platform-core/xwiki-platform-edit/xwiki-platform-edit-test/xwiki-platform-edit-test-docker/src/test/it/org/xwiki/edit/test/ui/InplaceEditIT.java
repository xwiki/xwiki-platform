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
package org.xwiki.edit.test.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Tests in-place page editing.
 * 
 * @version $Id$
 * @since 12.10.6
 * @since 13.2RC1
 */
@UITest
class InplaceEditIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg");
    }

    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, "before\n\n== Section ==\n\nafter", "test title");
    }

    @Test
    @Order(1)
    void editInplace(TestUtils setup, TestReference testReference)
    {
        InplaceEditablePage viewPage = new InplaceEditablePage();
        assertTrue(viewPage.getPageURL().endsWith("/editInplace/"), viewPage.getPageURL());

        //
        // Test action buttons (Cancel, Save, Save & View).
        //

        // Cancel
        viewPage.editInplace();
        assertEquals("test title", viewPage.getDocumentTitle());
        assertTrue(viewPage.getPageURL().endsWith("/editInplace/#edit"), viewPage.getPageURL());

        viewPage.setDocumentTitle("updated title");
        viewPage.cancel();
        assertEquals("test title", viewPage.getDocumentTitle());
        assertTrue(viewPage.getPageURL().endsWith("/editInplace/#"), viewPage.getPageURL());

        // Save + Cancel
        viewPage.editInplace();
        assertEquals("test title", viewPage.getDocumentTitle());
        assertTrue(viewPage.getPageURL().endsWith("/editInplace/#edit"), viewPage.getPageURL());

        viewPage.setDocumentTitle("updated title");
        viewPage.save();
        viewPage.setDocumentTitle("new title");
        viewPage.cancel();
        assertEquals("updated title", viewPage.getDocumentTitle());

        // Save & View
        viewPage.editInplace();
        assertEquals("updated title", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("new title");
        viewPage.saveAndView();
        assertEquals("new title", viewPage.getDocumentTitle());

        // Save + Save & View
        viewPage.editInplace();
        assertEquals("new title", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("another title");
        viewPage.save();
        viewPage.setDocumentTitle("last title");
        viewPage.saveAndView();
        assertEquals("last title", viewPage.getDocumentTitle());

        //
        // Test section editing
        //

        viewPage.editSectionInplace(1);
        assertEquals("last title", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("Last Title");
        viewPage.save().cancel();
        assertEquals("Last Title", viewPage.getDocumentTitle());

        //
        // Test in-place editor reload.
        //

        viewPage.editInplace();
        viewPage.setDocumentTitle("some title");
        setup.getDriver().navigate().refresh();
        viewPage = new InplaceEditablePage().waitForInplaceEditor();
        // Changes are currently lost on page reload.
        assertEquals("Last Title", viewPage.getDocumentTitle());

        viewPage.setDocumentTitle("final title");
        viewPage.saveAndView();
        assertEquals("final title", viewPage.getDocumentTitle());
    }

    @Test
    @Order(2)
    void saveFromSourceMode(TestUtils setup, TestReference testReference)
    {
        // Enter in-place edit mode.
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();
        CKEditor ckeditor = new CKEditor("content");
        RichTextAreaElement richTextArea = ckeditor.getRichTextArea();
        richTextArea.clear();

        // Insert a macro that is editable in-line.
        richTextArea.sendKeys("/inf");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/inf", "Info Box");
        richTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // The content is reloaded after the macro is inserted.
        ckeditor.getRichTextArea();

        // Switch to Source mode and save without making any change.
        ckeditor.getToolBar().toggleSourceMode();
        assertEquals("", ckeditor.getSourceTextArea().getText());
        viewPage.saveAndView();

        // Edit again and check the source.
        viewPage.editInplace();
        ckeditor = new CKEditor("content");
        // Focus the rich text area to get the floating toolbar.
        ckeditor.getRichTextArea().click();
        ckeditor.getToolBar().toggleSourceMode();
        WebElement sourceTextArea = ckeditor.getSourceTextArea();
        assertEquals("{{info}}\nType your information message here.\n{{/info}}", sourceTextArea.getAttribute("value"));

        // Modify the soure and save twice, without any change in between.
        sourceTextArea.clear();
        sourceTextArea.sendKeys("{{success}}test{{/success}}");
        viewPage.save().saveAndView();

        // Edit again and check the source.
        viewPage.editInplace();
        ckeditor = new CKEditor("content");
        ckeditor.getRichTextArea().click();
        ckeditor.getToolBar().toggleSourceMode();
        assertEquals("{{success}}\ntest\n{{/success}}", ckeditor.getSourceTextArea().getAttribute("value"));
        viewPage.cancel();
    }

    @Test
    @Order(3)
    void editInPlaceWithMandatoryTitle(TestUtils setup, TestReference testReference) throws Exception
    {
        // First of all, test that we can save with an empty title.
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();
        viewPage.setDocumentTitle("");
        assertEquals(testReference.getLastSpaceReference().getName(), viewPage.getDocumentTitlePlaceholder());
        assertFalse(viewPage.isDocumentTitleInvalid());
        assertEquals("", viewPage.getDocumentTitleValidationMessage());
        viewPage.saveAndView();

        // Now let's make document title mandatory.
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("xwiki.title.mandatory", "1");

        setup.loginAndGotoPage("alice", "pa$$word", setup.getURL(testReference));
        viewPage = new InplaceEditablePage().editInplace();

        // The title should be empty thus invalid.
        assertTrue(viewPage.isDocumentTitleInvalid());
        // We don't use a placeholder when document title is mandatory because it creates confusion.
        assertEquals("", viewPage.getDocumentTitlePlaceholder());

        // Typing something should make the title input valid.
        viewPage.setDocumentTitle("Title");
        assertFalse(viewPage.isDocumentTitleInvalid());

        // Now let's try to save with an empty title.
        viewPage.setDocumentTitle("").save(false);
        assertTrue(viewPage.isDocumentTitleInvalid());
        assertEquals("This field is required.", viewPage.getDocumentTitleValidationMessage());

        // Let's fix the title now.
        viewPage.setDocumentTitle("test title").saveAndView();
        assertEquals("test title", viewPage.getDocumentTitle());
    }

    @Test
    @Order(4)
    void editInPlaceWithMandatoryVersionSummary(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Reset for previous test.
        setup.setWikiPreference("xwiki.title.mandatory", "0");
        // Make version summaries mandatory.
        setup.setWikiPreference("editcomment_mandatory", "1");

        setup.loginAndGotoPage("alice", "pa$$word", setup.getURL(testReference));
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();

        // Try to save & view without a version summary.
        viewPage.saveAndView(false);
        Alert alert = setup.getDriver().switchTo().alert();
        assertEquals("Enter a brief description of your changes", alert.getText());
        // Accept without typing any text.
        alert.accept();

        // The empty change summary is not valid so the prompt (alert) is redisplayed, but not right away. It seems the
        // browser does this in the next event loop, so we need to wait for the prompt to reappear before interacting
        // with it.
        alert = setup.getDriver().waitUntilCondition(ExpectedConditions.alertIsPresent());

        // Let's dismiss the prompt this time, effectively canceling the save.
        alert.dismiss();

        // Try save & continue without a version summary.
        viewPage.save(false);
        setup.getDriver().switchTo().alert().dismiss();

        // Set the version summary. This should avoid the prompt.
        viewPage.setVersionSummary("test").save();

        // The version summary input is reset after each save.
        viewPage.save(false);
        alert = setup.getDriver().switchTo().alert();
        alert.sendKeys("foo");
        alert.accept();
        viewPage.waitForNotificationSuccessMessage("Saved");

        // Make version summaries optional again.
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("editcomment_mandatory", "0");

        setup.loginAndGotoPage("alice", "pa$$word", setup.getURL(testReference));
        new InplaceEditablePage().editInplace().saveAndView();
    }

    @Test
    @Order(5)
    void saveWithMergeReloadsEditor(TestUtils setup, TestReference testReference) throws Exception
    {
        // Enter in-place edit mode.
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();

        // Save the page outside the in-place editor to increase the version and trigger a merge conflict on save.
        setup.rest().savePage(testReference, "new content", "new title");

        // Save the page inside the in-place editor.
        viewPage.save();
        assertEquals("new title", viewPage.getDocumentTitle());

        viewPage.saveAndView();
        assertEquals("new content", viewPage.getContent());
    }
}
