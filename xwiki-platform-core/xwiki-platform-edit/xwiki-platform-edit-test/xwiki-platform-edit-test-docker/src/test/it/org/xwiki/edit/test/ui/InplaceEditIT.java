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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Keys;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.RequiredRightsModal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        // We might have an alert in case one test failed during an edition, in which case we just want to get rid of
        // the alert to move to next page.
        try {
            setup.gotoPage(testReference);
        } catch (UnhandledAlertException e) {
            setup.getDriver().switchTo().alert().accept();
        }
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
        assertEquals("{{info}}\nType your information message here.\n{{/info}}",
            sourceTextArea.getDomProperty("value"));

        // Modify the soure and save twice, without any change in between.
        sourceTextArea.clear();
        sourceTextArea.sendKeys("{{success}}test{{/success}}");
        viewPage.save().saveAndView();

        // Edit again and check the source.
        viewPage.editInplace();
        ckeditor = new CKEditor("content");
        ckeditor.getRichTextArea().click();
        ckeditor.getToolBar().toggleSourceMode();
        assertEquals("{{success}}\ntest\n{{/success}}", ckeditor.getSourceTextArea().getDomProperty("value"));
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
        assertNull(viewPage.getDocumentTitlePlaceholder());

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

    @Test
    @Order(6)
    void macroPlaceholder(TestUtils setup, TestReference testReference)
    {
        // We test using the in-place editor (and not the standalone editor) because we want the JavaScript code used by
        // dynamic macros such as the Children macro to be executed. We want to test that the macro placeholder is
        // hidden after the Children macro is lazy loaded.

        // Enter in-place edit mode.
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();
        CKEditor ckeditor = new CKEditor("content");
        RichTextAreaElement richTextArea = ckeditor.getRichTextArea();
        richTextArea.clear();
        richTextArea.sendKeys("a first line", Keys.ENTER);

        // Insert the Id macro. The macro placeholder should be displayed.
        richTextArea.sendKeys(Keys.ENTER, "/id");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/id", "Id");
        richTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // We need to set the required name parameter through the Macro Edit dialog.
        MacroDialogEditModal macroEditModal = new MacroDialogEditModal().waitUntilReady();
        macroEditModal.setMacroParameter("name", "test").clickSubmit();
        richTextArea.waitForContentRefresh();

        // Insert the Children macro. The macro placeholder is initially displayed but then hidden, because the macro
        // output is empty until the tree is lazy loaded.
        richTextArea.sendKeys(Keys.UP, "/chi");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/chi", "Children");
        richTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        richTextArea.waitForContentRefresh();

        assertEquals("a first line\nNo pages found\nmacro:id", richTextArea.getText());
        viewPage.cancel();
    }

    @Test
    @Order(7)
    void selectionRestoreOnSwitchToSource(TestUtils setup, TestReference testReference)
    {
        // We test using the in-place editor because the Source area doesn't have the vertical scrollbar (as it happens
        // with the standalone editor) so the way the restored selection is scrolled into view is different.

        // Enter in-place edit mode.
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();
        CKEditor ckeditor = new CKEditor("content");
        RichTextAreaElement richTextArea = ckeditor.getRichTextArea();
        richTextArea.clear();

        // Insert some long text (vertically).
        for (int i = 1; i < 50; i++) {
            richTextArea.sendKeys(String.valueOf(i), Keys.ENTER);
        }
        richTextArea.sendKeys("50");
        // Go back to the start of the content, on the second line (paragraph).
        richTextArea.sendKeys(Keys.HOME, Keys.PAGE_UP, Keys.PAGE_UP, Keys.PAGE_UP, Keys.DOWN);
        // Select the text on the second line.
        richTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));

        // Switch to Source mode.
        ckeditor.getToolBar().toggleSourceMode();
        WebElement sourceTextArea = ckeditor.getSourceTextArea();

        // Verify that the selection is restored.
        assertEquals("2", sourceTextArea.getDomProperty("selectionStart"));
        assertEquals("4", sourceTextArea.getDomProperty("selectionEnd"));

        // Verify that the top left corner of the Source text area is visible (inside the viewport).
        assertTrue(setup.getDriver().isVisible(sourceTextArea, 0, 0));

        // Select something from the middle of the edited content.
        for (int i = 0; i < 46; i++) {
            sourceTextArea.sendKeys(Keys.DOWN);
        }
        sourceTextArea.sendKeys(Keys.HOME);
        sourceTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));

        // Switch back to WYSIWYG mode.
        ckeditor.getToolBar().toggleSourceMode();
        // Verify that the selection is restored.
        assertEquals("25", richTextArea.getSelectedText());
        // Verify that the restored selection is visible.
        assertTrue(richTextArea.isVisible(0, richTextArea.getSize().height / 2));

        // Switch back to Source.
        richTextArea.sendKeys(Keys.DOWN);
        richTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.HOME));
        ckeditor.getToolBar().toggleSourceMode();
        sourceTextArea = ckeditor.getSourceTextArea();

        // Verify that the selection is restored.
        int selectionStart = Integer.parseInt(sourceTextArea.getDomProperty("selectionStart"));
        int selectionEnd = Integer.parseInt(sourceTextArea.getDomProperty("selectionEnd"));
        assertEquals("26", sourceTextArea.getDomProperty("value").substring(selectionStart, selectionEnd));

        // Verify that the restored selection is visible (inside the viewport).
        assertTrue(setup.getDriver().isVisible(sourceTextArea, 0, sourceTextArea.getSize().height / 2));

        sourceTextArea.sendKeys(Keys.PAGE_DOWN, Keys.PAGE_DOWN, Keys.UP, Keys.UP);
        sourceTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.HOME));

        // Switch back to WYSIWYG mode.
        ckeditor.getToolBar().toggleSourceMode();
        // Verify that the selection is restored.
        assertEquals("49", richTextArea.getSelectedText());
        // Verify that the restored selection is visible.
        // Note that we have to subtract 1 from the height because the floating toolbar is overlapping the text area.
        assertTrue(richTextArea.isVisible(0, richTextArea.getSize().height - 1));

        // Switch back to Source.
        richTextArea.sendKeys(Keys.DOWN);
        richTextArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END));
        ckeditor.getToolBar().toggleSourceMode();
        sourceTextArea = ckeditor.getSourceTextArea();

        // Verify that the selection is restored.
        selectionStart = Integer.parseInt(sourceTextArea.getDomProperty("selectionStart"));
        selectionEnd = Integer.parseInt(sourceTextArea.getDomProperty("selectionEnd"));
        assertEquals("50", sourceTextArea.getDomProperty("value").substring(selectionStart, selectionEnd));

        // Verify that the restored selection is visible (inside the viewport).
        // Note that we have to subtract 1 from the height because the floating toolbar is overlapping the text area.
        assertTrue(setup.getDriver().isVisible(sourceTextArea, 0, sourceTextArea.getSize().height - 1));

        viewPage.cancel();
    }

    @Test
    @Order(8)
    void refreshOnRequiredRightsChange(TestUtils setup, TestReference testReference)
    {
        // Test that updating required rights refreshes the content.

        // Grant alice script right on this page to allow using the Velocity macro.
        setup.loginAsSuperAdmin();
        setup.setRights(testReference, null, "XWiki.alice", "script", true);
        setup.loginAndGotoPage("alice", "pa$$word", setup.getURL(testReference));

        // Enter in-place edit mode.
        InplaceEditablePage viewPage = new InplaceEditablePage().editInplace();
        CKEditor ckeditor = new CKEditor("content");
        RichTextAreaElement richTextArea = ckeditor.getRichTextArea();
        richTextArea.clear();

        // Insert the Velocity macro. The macro placeholder should be displayed.
        richTextArea.sendKeys(Keys.ENTER, "/velocity");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velocity", "Velocity");
        richTextArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        new MacroDialogEditModal().waitUntilReady().setMacroContent(" ").clickSubmit();
        richTextArea.waitForContentRefresh();

        assertEquals("macro:velocity", richTextArea.getText());

        InformationPane informationPane = viewPage.openInformationDocExtraPane();

        RequiredRightsModal requiredRightsModal = informationPane.openRequiredRightsModal();
        requiredRightsModal.setEnforceRequiredRights(true);
        requiredRightsModal.clickSave(true);

        richTextArea.waitForContentRefresh();

        assertThat(richTextArea.getText(), startsWith("Failed to execute the [velocity] macro."));

        viewPage.save();

        assertTrue(viewPage.hasRequiredRightsWarning(true));

        requiredRightsModal = viewPage.openRequiredRightsModal();
        requiredRightsModal.setEnforcedRequiredRight("script");
        requiredRightsModal.clickSave(true);

        richTextArea.waitForContentRefresh();

        assertEquals("macro:velocity", richTextArea.getText());

        setup.getDriver().waitUntilCondition(driver -> !viewPage.hasRequiredRightsWarning(false));

        viewPage.cancel();
    }
}
