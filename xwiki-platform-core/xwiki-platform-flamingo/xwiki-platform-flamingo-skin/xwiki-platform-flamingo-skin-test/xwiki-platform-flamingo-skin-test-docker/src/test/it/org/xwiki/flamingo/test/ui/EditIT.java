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
package org.xwiki.flamingo.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.flamingo.skin.test.po.EditConflictModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.panels.test.po.DocumentInformationPanel;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.LoginPage;
import org.xwiki.test.ui.po.ResubmissionPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the wiki edit UI.
 *
 * @version $Id$
 * @since 11.2RC1
 */
@UITest
public class EditIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterEach
    public void tearDown(LogCaptureConfiguration logCaptureConfiguration)
    {
        logCaptureConfiguration.registerExpected("CSRFToken: Secret token verification failed");
    }

    /**
     * Test the ability to add edit comments and the ability to disable the edit comments feature, and verify.
     */
    @Test
    @Order(1)
    public void showAndHideEditComments(TestUtils setup, TestReference reference) throws Exception
    {
        ViewPage vp = setup.gotoPage(reference);

        // Verify that the edit comment field is there and that we can type in it.
        WikiEditPage wep = vp.editWiki();
        wep.setEditComment("some comment");
        wep.clickCancel();

        // Verify that we can disable the edit comment field
        // (Test for XWIKI-2487: Hiding the edit comment field doesn't work)
        try {
            setup.setPropertyInXWikiCfg("xwiki.editcomment.hidden=1");
            vp = setup.gotoPage(reference);
            wep = vp.editWiki();
            assertFalse(wep.isEditCommentDisplayed());
        } finally {
            setup.setPropertyInXWikiCfg("xwiki.editcomment.hidden=0");
        }
    }

    /**
     * Verify minor edit feature is working.
     */
    @Test
    @Order(2)
    public void minorEdit(TestUtils setup, TestReference reference)
    {
        setup.deletePage(reference);
        ViewPage vp = setup.gotoPage(reference);
        WikiEditPage wep = vp.editWiki();
        wep.setContent("version=1.1");

        // Save & Continue = minor edit.
        wep.clickSaveAndContinue();

        wep.setContent("version=2.1");

        // Save & View = major edit
        wep.clickSaveAndView();

        // Verify that the revision exists by navigating to it and by asserting its content
        setup.gotoPage(reference, "viewrev", "rev=2.1");

        vp = new ViewPage();
        assertEquals("version=2.1", vp.getContent());

        wep = vp.editWiki();
        wep.setContent("version=2.2");
        wep.setMinorEdit(true);
        wep.clickSaveAndView();

        // Verify that the minor revision exists by navigating to it and by asserting its content
        setup.gotoPage(reference, "viewrev", "rev=2.2");
        vp = new ViewPage();
        assertEquals("version=2.2", vp.getContent());
    }

    /**
     * Tests that users can completely remove the content from a document (make the document empty). In previous
     * versions (pre-1.5M2), removing all content in page had no effect. See XWIKI-1007.
     */
    @Test
    @Order(3)
    public void emptyDocumentContentIsAllowed(TestUtils setup, TestReference reference)
    {
        setup.deletePage(reference);
        setup.createPage(reference, "this is some content", "EmptyContentAllowed");
        ViewPage vp = setup.gotoPage(reference);
        WikiEditPage wep = vp.editWiki();
        wep.setContent("");
        vp = wep.clickSaveAndView();
        assertNull(ExpectedConditions.alertIsPresent().apply(setup.getDriver()));
        assertEquals(-1, setup.getDriver().getCurrentUrl().indexOf("/edit/"));
        assertEquals("", vp.getContent());
    }

    @Test
    @Order(4)
    public void emptyLineAndSpaceCharactersBeforeSectionTitleIsNotRemoved(TestUtils setup, TestReference reference)
    {
        setup.deletePage(reference);
        String content = "\n== Section ==\n\ntext";
        setup.createPage(reference, content, "Empty Line is not removed");
        ViewPage vp = setup.gotoPage(reference);
        WikiEditPage wep = vp.editWiki();
        assertEquals(content, wep.getExactContent());
    }

    @Test
    @Order(5)
    public void testBoldButton(TestUtils setup, TestReference reference)
    {
        testToolBarButton(setup, reference, "Bold", "**%s**", "Text in Bold");
        testToolBarButton(setup, reference, "Italics", "//%s//", "Text in Italics");
        testToolBarButton(setup, reference, "Underline", "__%s__", "Text in Underline");
        testToolBarButton(setup, reference, "Internal Link", "[[%s]]", "Link Example");
        testToolBarButton(setup, reference, "Horizontal ruler", "\n----\n", "");
        testToolBarButton(setup, reference, "Attached Image", "[[image:%s]]", "example.jpg");
    }

    /**
     * Tests that the specified tool bar button works.
     *
     * @param buttonTitle the title of a tool bar button
     * @param format the format of the text inserted by the specified button
     * @param defaultText the default text inserted if there's no text selected in the text area
     */
    private void testToolBarButton(TestUtils setup, TestReference reference, String buttonTitle,
        String format, String defaultText)
    {
        ViewPage vp = setup.gotoPage(reference);
        WikiEditPage wikiEditPage = vp.editWiki();
        wikiEditPage.clearContent();
        wikiEditPage.sendKeys("a");
        wikiEditPage.clickToolbarButton(buttonTitle);
        // Type b and c on two different lines and move the caret after b.
        wikiEditPage.sendKeys("b", Keys.ENTER, "c", Keys.ARROW_LEFT, Keys.ARROW_LEFT);
        wikiEditPage.clickToolbarButton(buttonTitle);
        // Move the caret after c, type d and e, then select d.
        wikiEditPage.sendKeys(Keys.PAGE_DOWN, Keys.END, "de", Keys.ARROW_LEFT);
        wikiEditPage.sendKeysWithAction(Keys.SHIFT, Keys.ARROW_LEFT);
        wikiEditPage.clickToolbarButton(buttonTitle);
        wikiEditPage = new WikiEditPage();
        if (defaultText.isEmpty()) {
            assertEquals("a" + format + "b" + format + "\nc" + format + "de", wikiEditPage.getExactContent());
        } else {
            assertEquals(
                String.format("a" + format + "b" + format + "\nc" + format + "e", defaultText, defaultText, "d"),
                wikiEditPage.getExactContent());
        }
    }

    /**
     * Ensure that the Save&View display a "Saving..." message and that the form is disabled before loading the new
     * page.
     */
    @Test
    @Order(6)
    public void saveAndFormManipulation(TestUtils setup, TestReference reference)
    {
        setup.deletePage(reference);
        ViewPage viewPage = setup.gotoPage(reference);
        WikiEditPage editWiki = viewPage.editWiki();

        // Prevent from leaving the page so that we can check the UI before moving out of the page
        setup.getDriver().executeJavascript("window.onbeforeunload = function () { return false; }");
        editWiki.clickSaveAndView(false);

        // An alert should appear to ask the user if he wants to leave the page.
        setup.getDriver().waitUntilCondition(ExpectedConditions.alertIsPresent());

        // We dismiss it so we can stay on the page and check the UI.
        setup.getDriver().switchTo().alert().dismiss();

        // Check that the saving message is displayed.
        editWiki.waitForNotificationInProgressMessage("Saving...");
        // the form should remain disabled since we normally should be driven to another page.
        assertFalse(editWiki.isEnabled());

        // Now allow to leave the page.
        setup.getDriver().executeJavascript("window.onbeforeunload = null;");
        // Go back to the editor to reset the status
        viewPage = setup.gotoPage(reference);
        editWiki = viewPage.editWiki();

        editWiki.clickSaveAndContinue(true);
        // After a save&continue the form remains enabled
        assertTrue(editWiki.isEnabled());
        editWiki.clickSaveAndView();

        // Ensure the reload lead to the right page
        assertEquals(setup.getURL(reference, "view", ""), setup.getDriver().getCurrentUrl() + "WebHome");
    }

    @Test
    @Order(7)
    public void allowForceSaveWhenCSRFIssue(TestUtils setup, TestReference testReference)
    {
        DocumentReference invalidateCSRF = new DocumentReference("InvalidateCSRF",
            testReference.getLastSpaceReference());
        String invalidateCSRFContent = "{{velocity}}$services.csrf.clearToken(){{/velocity}}";
        setup.createPage(invalidateCSRF, invalidateCSRFContent, "InvalidateCSRF");
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());

        WikiEditPage editWiki = setup.gotoPage(testReference).editWiki();

        // we clear the token and navigate back to the editor
        setup.gotoPage(invalidateCSRF);
        setup.getDriver().navigate().back();

        editWiki.setContent("Some content 42");
        editWiki.clickSaveAndContinue(false);
        assertTrue(editWiki.isCSRFWarningDisplayed());

        // check that after a cancel we can still edit
        editWiki.clickCancelCSRFWarningButton();
        editWiki.setContent("Another content 42");
        editWiki.clickSaveAndContinue(false);

        // check that the warning is still displayed after a cancel
        assertTrue(editWiki.isCSRFWarningDisplayed());
        editWiki.clickForceSaveCSRFButton();
        editWiki.waitForNotificationSuccessMessage("Saved");

        // reload the editor and check the change have been saved
        editWiki = setup.gotoPage(testReference).editWiki();
        assertEquals("Another content 42", editWiki.getContent());

        // we clear the token and navigate back to the editor
        setup.gotoPage(invalidateCSRF);
        setup.getDriver().navigate().back();

        editWiki.setContent("Foo bar");

        // check with save and view
        editWiki.clickSaveAndView(false);
        assertTrue(editWiki.isCSRFWarningDisplayed());
        editWiki.clickForceSaveCSRFButton();

        // Ensure the page is properly loaded after a save and view
        ViewPage viewPage = new ViewPage();
        viewPage.waitUntilPageJSIsLoaded();
        assertEquals("Foo bar", viewPage.getContent());

        // check with preview
        editWiki = setup.gotoPage(testReference).editWiki();
        // we clear the token and navigate back to the editor
        setup.gotoPage(invalidateCSRF);
        setup.getDriver().navigate().back();
        editWiki.setContent("Preview test & cancel");
        editWiki.getPreviewButton().click();
        ResubmissionPage resubmissionPage = new ResubmissionPage();
        assertTrue(resubmissionPage.isOnResubmissionPage());

        // check cancelling: it leads back to the page in view mode without any change
        resubmissionPage.cancel();
        viewPage = new ViewPage();
        assertEquals(testReference.getLastSpaceReference().getName(), viewPage.getDocumentTitle());
        assertEquals("Foo bar", viewPage.getContent());

        editWiki = setup.gotoPage(testReference).editWiki();
        // we clear the token and navigate back to the editor
        setup.gotoPage(invalidateCSRF);
        setup.getDriver().navigate().back();
        editWiki.setContent("Preview test & back to editor");
        editWiki.getPreviewButton().click();
        resubmissionPage = new ResubmissionPage();
        assertTrue(resubmissionPage.isOnResubmissionPage());
        // check resubmit and go back to editor
        resubmissionPage.resubmit();
        PreviewEditPage previewEditPage = new PreviewEditPage(editWiki);
        assertEquals("Preview test & back to editor", previewEditPage.getContent());
        editWiki = (WikiEditPage) previewEditPage.clickBackToEdit();
        assertEquals("Preview test & back to editor", editWiki.getContent());
        editWiki.setContent("Foo bar 2");
        // Ensure we can now save.
        viewPage = editWiki.clickSaveAndView();
        assertEquals("Foo bar 2", viewPage.getContent());

        editWiki = setup.gotoPage(testReference).editWiki();
        // we clear the token and navigate back to the editor
        setup.gotoPage(invalidateCSRF);
        setup.getDriver().navigate().back();
        editWiki.setContent("Preview test & save&continue");
        editWiki.getPreviewButton().click();
        resubmissionPage = new ResubmissionPage();
        assertTrue(resubmissionPage.isOnResubmissionPage());
        // check resubmit and save and continue: it should led back to the editor with the saved changes
        resubmissionPage.resubmit();
        previewEditPage = new PreviewEditPage(editWiki);
        assertEquals("Preview test & save&continue", previewEditPage.getContent());
        setup.getDriver().addPageNotYetReloadedMarker();
        previewEditPage.clickSaveAndContinue(false);
        setup.getDriver().waitUntilPageIsReloaded();
        editWiki = new WikiEditPage();
        assertEquals("Preview test & save&continue", editWiki.getContent());
        viewPage = setup.gotoPage(testReference);
        assertEquals("Preview test & save&continue", viewPage.getContent());

        editWiki = setup.gotoPage(testReference).editWiki();
        // we clear the token and navigate back to the editor
        setup.gotoPage(invalidateCSRF);
        setup.getDriver().navigate().back();
        editWiki.setContent("Preview test & save&view");
        editWiki.getPreviewButton().click();
        resubmissionPage = new ResubmissionPage();
        assertTrue(resubmissionPage.isOnResubmissionPage());
        // check resubmit and save and view
        resubmissionPage.resubmit();
        previewEditPage = new PreviewEditPage(editWiki);
        assertEquals("Preview test & save&view", previewEditPage.getContent());
        viewPage = previewEditPage.clickSaveAndView();
        assertEquals("Preview test & save&view", viewPage.getContent());

        // Ensure to have the proper secret token for further tests.
        setup.recacheSecretToken();
    }

    /**
     * A complete scenario of edit with conflicts, manipulating the merging conflict window.
     * The following scenario is performed by doing edition of the same document in two tabs:
     *   1. Edit same line, save&continue, ensure the merge conflict window appears,
     *      Save with fixing merge conflict by merging and using current changes
     *   2. Edit another line, Save & view and ensure the automatic merge is transparent for the user when there's
     *      no conflict.
     *   3. Edit first line to create another conflict on a not refreshed editor, check that the force save and merge
     *      save diffs are different, verify manipulating diffs, submit reload of the editor
     *   4. Edit different places, save&continue, ensure the automatic merge is performed and the editor is reloaded
     *      with fresh content.
     *   5. Create another last conflict, Save&Continue, fix by forcing save.
     */
    @Test
    @Order(8)
    public void editWithConflict(TestUtils setup, TestReference testReference)
    {
        // Fixture
        String title = testReference.getLastSpaceReference().getName();
        setup.deletePage(testReference);
        ViewPage vp = setup.createPage(testReference, "", title);

        // Prepare the two tabs
        String firstTabHandle = setup.getCurrentTabHandle();
        WikiEditPage wikiEditPageTab1 = vp.editWiki();
        String secondTabHandle = setup.openLinkInTab(By.linkText(title), firstTabHandle);

        // Step 1: Edit same lines and fix conflict by merging
        setup.switchTab(secondTabHandle);
        ViewPage viewPage = new ViewPage();
        WikiEditPage wikiEditPageTab2 = viewPage.editWiki();
        wikiEditPageTab2.setContent("A first edit from a tab.");
        wikiEditPageTab2.clickSaveAndContinue();

        setup.switchTab(firstTabHandle);
        wikiEditPageTab1.waitUntilPageJSIsLoaded();
        setup.getDriver().addPageNotYetReloadedMarker();
        wikiEditPageTab1.setContent("A second edit from another tab.");
        wikiEditPageTab1.clickSaveAndContinue(false);

        EditConflictModal editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals(EditConflictModal.ConflictChoice.MERGE, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.MERGED, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@",
            "-A <del>fir</del>s<del>t</del> edit from a tab.",
            "+A s<ins>econd</ins> edit from a<ins>nother</ins> tab."), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.submitCurrentChoice(true);
        setup.getDriver().waitUntilPageIsReloaded();

        // Check that merge with save&continue indeed save the content
        setup.gotoPage(testReference);
        viewPage = new ViewPage();
        assertEquals("A second edit from another tab.", viewPage.getContent());

        // Step 2: Edit another line and save&view, an automatic merge should occur
        // We cannot directly make the changes since the
        setup.switchTab(secondTabHandle);
        wikiEditPageTab2 = new WikiEditPage();
        assertEquals("A first edit from a tab.", wikiEditPageTab2.getExactContent());
        // We only add a new line from the content displayed in the editor.
        wikiEditPageTab2.setContent("A first edit from a tab.\nAnother line.");

        // The merge should be automatic.
        viewPage = wikiEditPageTab2.clickSaveAndView();
        assertEquals("A second edit from another tab.\nAnother line.", viewPage.getContent());
        wikiEditPageTab2 = viewPage.editWiki();

        // Step 3: Create another conflict, check the different diffs and discard changes (reload the editor)
        setup.switchTab(firstTabHandle);
        wikiEditPageTab1 = setup.gotoPage(testReference).editWiki();
        wikiEditPageTab1.setContent("A third edit from another tab.\nAnother line.\nYet another line.");
        wikiEditPageTab1.clickSaveAndContinue();

        setup.switchTab(secondTabHandle);
        wikiEditPageTab2.waitUntilPageJSIsLoaded();
        // the page will be reloaded by choice
        setup.getDriver().addPageNotYetReloadedMarker();
        wikiEditPageTab2.setContent("A fourth edit from second tab.\nAnother line.");
        wikiEditPageTab2.clickSaveAndView(false);

        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals(EditConflictModal.ConflictChoice.MERGE, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.MERGED, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList("@@ -1,3 +1,3 @@",
            "-A th<del>ird</del> edit from <del>anoth</del>e<del>r</del> tab.",
            "+A <ins>four</ins>th edit from <ins>s</ins>e<ins>cond</ins> tab.",
            " Another line.",
            " Yet another line."),
            editConflictModal.getDiff().getDiff("Content"));

        // Check that the forceSave diff is not the same as the merge diff
        editConflictModal = editConflictModal.makeChoice(EditConflictModal.ConflictChoice.OVERRIDE);
        assertEquals(EditConflictModal.ConflictChoice.OVERRIDE, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.CURRENT, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList(
            "@@ -1,3 +1,2 @@",
            "-A th<del>ird</del> edit from <del>anoth</del>e<del>r</del> tab.",
            "+A <ins>four</ins>th edit from <ins>s</ins>e<ins>cond</ins> tab.",
            " Another line.",
            "-Yet another line."),
            editConflictModal.getDiff().getDiff("Content"));

        // Check the reload diff
        editConflictModal = editConflictModal.makeChoice(EditConflictModal.ConflictChoice.RELOAD);
        assertEquals(EditConflictModal.ConflictChoice.RELOAD, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.CURRENT, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList(
            "@@ -1,2 +1,3 @@",
            "-A <del>four</del>th edit from <del>seco</del>n<del>d</del> tab.",
            "+A th<ins>ird</ins> edit from <ins>a</ins>n<ins>other</ins> tab.",
            " Another line.",
            "+Yet another line."),
            editConflictModal.getDiff().getDiff("Content"));

        // check that changing the diff independently from the choice works and doesn't change the choice made
        editConflictModal = editConflictModal.changeDiff(EditConflictModal.AvailableDiffVersions.PREVIOUS,
            EditConflictModal.AvailableDiffVersions.NEXT);
        assertEquals(EditConflictModal.ConflictChoice.RELOAD, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.PREVIOUS, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList(
            "@@ -1,2 +1,3 @@",
            "-A <del>secon</del>d edit from another tab.",
            "+A <ins>thir</ins>d edit from another tab.",
            " Another line.",
            "+Yet another line."),
            editConflictModal.getDiff().getDiff("Content"));
        // reload the editor
        editConflictModal.submitCurrentChoice(false);
        setup.getDriver().waitUntilPageIsReloaded();
        wikiEditPageTab2 = new WikiEditPage();
        assertEquals("A third edit from another tab.\nAnother line.\nYet another line.",
            wikiEditPageTab2.getContent());

        // Step 4: Edit different places, ensure the automatic merge is performed and the editor refreshed
        setup.switchTab(firstTabHandle);
        wikiEditPageTab1.waitUntilPageJSIsLoaded();
        wikiEditPageTab1.setContent("A third edit from another tab.\nAnother line."
            + "\nYet another line with other few changes.");
        wikiEditPageTab1.clickSaveAndContinue();

        setup.switchTab(secondTabHandle);
        wikiEditPageTab2.waitUntilPageJSIsLoaded();
        // The editor will be reloaded because of the merge
        setup.getDriver().addPageNotYetReloadedMarker();
        wikiEditPageTab2.setContent("A fourth edit from another tab.\nAnother line.\nYet another line.");
        wikiEditPageTab2.clickSaveAndContinue(false);
        setup.getDriver().waitUntilPageIsReloaded();
        wikiEditPageTab2 = new WikiEditPage();
        assertEquals("A fourth edit from another tab.\nAnother line.\nYet another line with other few changes.",
            wikiEditPageTab2.getContent());

        // Step 5: Create a conflict, force save, ensure the data are saved
        wikiEditPageTab2.setContent("A fourth edit from another tab.\nAnother line."
            + "\nYet another line with other few changes.\nAnd again a new line");
        wikiEditPageTab2.clickSaveAndContinue();
        setup.switchTab(firstTabHandle);
        wikiEditPageTab1.waitUntilPageJSIsLoaded();
        wikiEditPageTab1.setContent("A fifth edit from another tab.\nAnother line."
            + "\nYet another line with other few changes.");
        wikiEditPageTab1.clickSaveAndContinue(false);

        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals(EditConflictModal.ConflictChoice.MERGE, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.MERGED, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList("@@ -1,4 +1,4 @@",
            "-A f<del>our</del>th edit from another tab.",
            "+A f<ins>if</ins>th edit from another tab.",
            " Another line.",
            " Yet another line with other few changes.",
            " And again a new line"), editConflictModal.getDiff().getDiff("Content"));

        // Choose force save
        editConflictModal = editConflictModal.makeChoice(EditConflictModal.ConflictChoice.OVERRIDE);
        assertEquals(EditConflictModal.ConflictChoice.OVERRIDE, editConflictModal.getCurrentChoice());
        assertEquals(EditConflictModal.AvailableDiffVersions.NEXT, editConflictModal.getOriginalDiffVersion());
        assertEquals(EditConflictModal.AvailableDiffVersions.CURRENT, editConflictModal.getRevisedDiffVersion());
        assertEquals(Arrays.asList("@@ -1,4 +1,3 @@",
            "-A f<del>our</del>th edit from another tab.",
            "+A f<ins>if</ins>th edit from another tab.",
            " Another line.",
            " Yet another line with other few changes.",
            "-And again a new line"),
            editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.submitCurrentChoice(true);
        // The content should remain the same in the editor
        assertEquals("A fifth edit from another tab.\nAnother line."
            + "\nYet another line with other few changes.", wikiEditPageTab1.getExactContent());
        // It should have been saved.
        viewPage = setup.gotoPage(testReference);
        // mitigate the flicker: this should be removed when XWIKI-16406 is fixed.
        setup.getDriver().navigate().refresh();
        viewPage = new ViewPage();
        assertEquals("A fifth edit from another tab.\nAnother line."
            + "\nYet another line with other few changes.", viewPage.getContent());
        setup.closeTab(secondTabHandle);
    }

    /**
     * Ensure that document can be created with very long titles with more than 255 characters.
     */
    @Test
    @Order(9)
    public void createDocumentLongTitle(TestUtils setup, TestReference reference)
    {
        String name1 = "Company Presentation Events";
        String name2 = "Presentation from 10 december 2015 at the Fourth edition of the International Conference for "
            + "the Environmental Responsibility (ICER 2015)";
        String name3 = "Intervention from the President of the Interdisciplinary Commission for Responsible Development"
            + " of Alternative Fuels (ICRDA)";

        SpaceReference spaceReference = new SpaceReference(reference.getWikiReference().getName(),
            Arrays.asList(name1, name2));
        DocumentReference documentReference = new DocumentReference(name3, spaceReference);
        setup.gotoPage(documentReference, "create");
        CreatePagePage createPagePage = new CreatePagePage();
        createPagePage.waitForErrorMessage();
        assertTrue(createPagePage.getErrorMessage()
            .contains("The full path of the page you want to create is too long"));
    }

    /*
     * Test that a user who leave the editor by clicking on a link and come back won't have a conflict modal.
     */
    @Test
    @Order(10)
    public void editLeaveAndBack(TestUtils setup, TestReference testReference) throws InterruptedException
    {
        setup.deletePage(testReference);
        WikiEditPage wikiEditPage = setup.gotoPage(testReference).editWiki();
        wikiEditPage.setContent("First edit");
        wikiEditPage.clickSaveAndContinue();

        // Simple way, just by going to the view page and going back to edit
        setup.gotoPage(testReference);
        setup.getDriver().navigate().back();
        wikiEditPage = new WikiEditPage();
        wikiEditPage.setContent("Second edit");
        ViewPage viewPage = wikiEditPage.clickSaveAndView();
        assertEquals("Second edit", viewPage.getContent());

        // Complex way: continue to another editor, make other changes, and then get back to the editor.
        wikiEditPage = viewPage.editWiki();
        wikiEditPage.setContent("third edit");
        wikiEditPage.clickSaveAndContinue();
        // the page will be reloaded next time we go on it.
        setup.getDriver().addPageNotYetReloadedMarker();

        viewPage = setup.gotoPage(testReference);
        viewPage.waitUntilPageJSIsLoaded();

        WYSIWYGEditPage wysiwygEditPage = viewPage.editWYSIWYG();
        wysiwygEditPage.setContent("fourth edit");
        wysiwygEditPage.clickSaveAndContinue();

        // WYSIWYG editor -> view page
        setup.getDriver().navigate().back();
        // view page -> wiki editor
        setup.getDriver().navigate().back();

        wikiEditPage = new WikiEditPage();

        setup.getDriver().waitUntilPageIsReloaded();
        assertEquals("fourth edit", wikiEditPage.getContent());
        wikiEditPage.setContent("fifth edit");
        viewPage = wikiEditPage.clickSaveAndView();
        assertEquals("fifth edit", viewPage.getContent());
    }

    @Test
    @Order(11)
    public void editTitle255Characters(TestUtils setup, TestConfiguration testConfiguration,
        TestReference testReference, LogCaptureConfiguration logCaptureConfiguration)
    {
        setup.deletePage(testReference);

        // Title of 300 characters.
        String veryLongTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin viverra, erat et "
            + "placerat hendrerit, tellus eros fermentum tellus, vel laoreet orci ligula sodales augue. Aenean "
            + "fermentum suscipit magna. Donec erat orci, mollis at ligula molestie, ornare consequat mi. Duis "
            + "ultrices sed elit ac nullam.";

        // Right now error messages from the server are different if we are using Save&View or Save&Continue.
        // This needs to be fixed as part of XWIKI-16425.
        String saveContinueErrorMessage = "Failed to save the page. Reason: An error occured while saving: Error number"
            + " 3201 in 3: Exception while saving document " + setup.serializeReference(testReference) +".";

        String saveViewErrorMessage = "Failed to save the page. Reason: Server Error";
        String errorServerNotRespondingMessage = "Failed to save the page. Reason: Server not responding";

        // Apparently for MYSQL and Oracle the error message are also different.
        if (testConfiguration.getDatabase() == Database.MYSQL || testConfiguration.getDatabase() == Database.ORACLE) {
            saveContinueErrorMessage = errorServerNotRespondingMessage;
            saveViewErrorMessage = errorServerNotRespondingMessage;
        }

        // try with save and continue
        WikiEditPage wikiEditPage = setup.gotoPage(testReference).editWiki();
        wikiEditPage.setTitle(veryLongTitle);
        wikiEditPage.clickSaveAndContinue(false);

        wikiEditPage.waitForNotificationErrorMessage(saveContinueErrorMessage);
        wikiEditPage.setTitle("Lorem Ipsum");
        wikiEditPage.clickSaveAndContinue();

        // try with save and view
        wikiEditPage.setTitle(veryLongTitle);
        wikiEditPage.clickSaveAndView(false);
        wikiEditPage.waitForNotificationErrorMessage(saveViewErrorMessage);
        wikiEditPage.setTitle("Lorem Ipsum version 2");
        ViewPage viewPage = wikiEditPage.clickSaveAndView();
        assertEquals("Lorem Ipsum version 2", viewPage.getDocumentTitle());

        // Logs generated because of the >255 characters title error.
        logCaptureConfiguration.registerExpected(
            "JDBCExceptionReporter",
            "Error number 3201 in 3",
            "org.hibernate.HibernateException",
            "org.hibernate.exception.DataException",
            "java.sql.BatchUpdateException: data exception: string data, right truncation",
            "SQL Error: 1406, SQLState: 22001",
            "Data truncation: Data too long for column 'XWD_TITLE' at row 1",
            "SQL Error: 3401, SQLState: 22001",
            "SQL Error: -3401, SQLState: 22001",
            "SQL Error: 0, SQLState: 22001",
            "data exception: string data, right truncation",
            "SQL Error: 12899, SQLState: 72000",
            "ORA-12899: value too large for column \"XWIKI\".\"XWIKIDOC\".\"XWD_TITLE\" (actual: 300, maximum: 255)",
            // PostgreSQL specific log
            "value too long for type character varying(255)"
        );
    }

    @Test
    @Order(12)
    public void switchSyntax(TestUtils setup, TestReference testReference) throws Exception
    {
        // Fixture: enable xhtml syntax
        setup.addObject("Rendering", "RenderingConfig", "Rendering.RenderingConfigClass",
            "disabledSyntaxes", "plain/1.0,xdom+xml/current,xwiki/2.0");
        setup.deletePage(testReference);

        String pageContent = "= First heading =\n"
            + "\n"
            + "Paragraph containing some **bold content**.";
        setup.createPage(testReference, pageContent, "Test page");

        try {
            // Test that we can switch with wiki editor without loosing change not saved
            WikiEditPage wikiEditPage = setup.gotoPage(testReference).editWiki();
            final String newContent = pageContent + "\n\nA new paragraph with some new content.";
            wikiEditPage.setContent(newContent);

            DocumentInformationPanel documentInformationPanel = new DocumentInformationPanel();
            assertEquals(Arrays.asList("xwiki/2.1", "xhtml/1.0"), documentInformationPanel.getAvailableSyntaxes());
            assertEquals("xwiki/2.1", documentInformationPanel.getSelectedSyntax());
            try {
                documentInformationPanel.selectSyntax("xhtml/1.0");
                fail("A confirm alert should be triggered");
            } catch (UnhandledAlertException e) {
                Alert alert = setup.getDriver().switchTo().alert();
                assertTrue(alert.getText().contains("Do you want to also convert the document's content and "
                    + "objects to the selected syntax?"));
                // in wiki edit mode, we shouldn't loose any content.
                assertFalse(alert.getText().contains("you will loose modifications"));
                alert.accept();
            }
            final WikiEditPage reloadedWikiEdit = new WikiEditPage();

            String expectedContent = "<h1 id=\"HFirstheading\" class=\"wikigeneratedid\">"
                + "<span>First heading</span>"
                + "</h1>"
                + "<p>Paragraph containing some <strong>bold content</strong>.</p>"
                + "<p>A new paragraph with some new content.</p>";

            // The editor content is changed by triggering a JS event, we wait for a new content so we can do a proper
            // assertEquals afterwards that is easier to read in case of failure.
            setup.getDriver().waitUntilCondition(item -> !reloadedWikiEdit.getExactContent().equals(newContent));
            assertEquals(expectedContent, reloadedWikiEdit.getExactContent());
            // Ensure there's no edit conflict.
            wikiEditPage.clickSaveAndContinue();

            // We set back the syntax to XWiki 2.1, dismissing the alert should only change the syntax and save it
            // without processing the content.
            documentInformationPanel = new DocumentInformationPanel();
            assertEquals(Arrays.asList("xwiki/2.1", "xhtml/1.0"), documentInformationPanel.getAvailableSyntaxes());
            assertEquals("xhtml/1.0", documentInformationPanel.getSelectedSyntax());
            try {
                documentInformationPanel.selectSyntax("xwiki/2.1");
                fail("A confirm alert should be triggered");
            } catch (UnhandledAlertException e) {
                Alert alert = setup.getDriver().switchTo().alert();
                alert.dismiss();
            }

            wikiEditPage.clickSaveAndContinue();

            // Ensure that only the syntax has changed, not the content.
            wikiEditPage = setup.gotoPage(testReference).editWiki();
            documentInformationPanel = new DocumentInformationPanel();
            assertEquals("xwiki/2.1", documentInformationPanel.getSelectedSyntax());
            assertEquals(expectedContent, wikiEditPage.getContent());
            wikiEditPage.setContent(pageContent);
            ViewPage viewPage = wikiEditPage.clickSaveAndView();

            // Test that we can switch the syntax with the wysiwyg editor
            WYSIWYGEditPage wysiwygEditPage = viewPage.editWYSIWYG();

            // With WYSIWYG editor the page is actually reloaded.
            setup.getDriver().addPageNotYetReloadedMarker();
            documentInformationPanel = new DocumentInformationPanel();
            assertEquals(Arrays.asList("xwiki/2.1", "xhtml/1.0"), documentInformationPanel.getAvailableSyntaxes());
            assertEquals("xwiki/2.1", documentInformationPanel.getSelectedSyntax());

            try {
                documentInformationPanel.selectSyntax("xhtml/1.0");
                fail("A confirm alert should be triggered.");
            } catch (UnhandledAlertException e) {
                Alert alert = setup.getDriver().switchTo().alert();
                assertTrue(alert.getText().contains("Do you want to also convert the document's content and "
                    + "objects to the selected syntax?"));
                assertTrue(alert.getText().contains("you will loose modifications"));
                alert.accept();
            }

            setup.getDriver().waitUntilPageIsReloaded();
            wikiEditPage = new WikiEditPage();
            expectedContent = "<h1 id=\"HFirstheading\" class=\"wikigeneratedid\"><span>First heading</span></h1>"
                + "<p>Paragraph containing some <strong>bold content</strong>.</p>";
            assertEquals(expectedContent, wikiEditPage.getContent());
            // Ensure there's no edit conflict
            wikiEditPage.clickSaveAndContinue();
        } finally {
            // disable back xhtml syntax
            setup.deleteObject("Rendering", "RenderingConfig", "Rendering.RenderingConfigClass", 0);
        }
    }

    @Test
    @Order(13)
    public void saveActionValidatesWhenXValidateIsPresent(TestUtils setup, TestReference testReference)
    {
        String content = "{{velocity}}"
            + "value: $doc.display('prop')\n\n"
            + "#foreach($e in $xcontext.validationStatus.errors)"
            + "$e "
            + "#end"
            + "{{/velocity}}";

        setup.deletePage(testReference);
        setup.createPage(testReference, content, "");
        setup.addClassProperty(testReference, "prop", "String");
        setup.updateClassProperty(testReference,
            "prop_validationRegExp", "/^[0-4][0-2]$/",
            "prop_validationMessage", "invalid value for prop");

        String className = setup.serializeReference(testReference).split(":")[1];
        setup.addObject(testReference, className, "prop", "22");
        setup.gotoPage(testReference, "save", "xvalidate=1");
        ViewPage viewPage = new ViewPage();
        assertEquals("value: 22", viewPage.getContent());


        setup.updateObject(testReference, className, 0, "prop", "44");
        setup.gotoPage(testReference, "save", "xvalidate=1");
        InlinePage inlinePage = new InlinePage();
        assertTrue(inlinePage.getForm().isDisplayed());
        assertEquals("44", inlinePage.getValue("prop"));
        assertTrue(inlinePage.getForm().getText().contains("invalid value for prop"));

        String queryString = String.format("xvalidate=1&%s_0_prop=11", className);
        setup.gotoPage(testReference, "save", queryString);
        viewPage = new ViewPage();
        assertEquals("value: 11", viewPage.getContent());
    }

    @Test
    @Order(14)
    public void logoutDuringEdit(TestUtils setup, TestReference testReference)
    {
        // fixture: deny right edit to the guest user on the page, since we want to get a 401 as in XWiki Standard
        setup.createPage(testReference, "", "");
        setup.addObject(testReference, "XWiki.XWikiRights",
            "levels", "edit",
            "users", "XWiki.XWikiGuest",
            "allow", "Deny");

        String test = "Test string " + System.currentTimeMillis();

        // start editing a page
        WikiEditPage editPage = setup.gotoPage(testReference).editWiki();
        editPage.setTitle(test);
        editPage.setContent(test);
        // emulate expired session: delete the cookies
        // We cannot use forceGuestUser since it also recache the secret token and reload the page
        // here we really want to remain on the same page.
        setup.getDriver().manage().deleteAllCookies();
        // try to save
        editPage.clickSaveAndView(false);
        assertTrue(editPage.loginModalDisplayed());
        String mainWindow = setup.getCurrentTabHandle();
        // this will switch the tab
         LoginPage loginPage = editPage.clickModalLoginLink();

        String newTabHandle = setup.getCurrentTabHandle();
        loginPage.loginAs(TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        // Don't forget to properly close the new tab
        setup.switchTab(mainWindow);
        setup.closeTab(newTabHandle);
        editPage = new WikiEditPage();
        editPage.closeLoginModal();
        ViewPage viewPage = editPage.clickSaveAndView();

        assertEquals(test, viewPage.getDocumentTitle());
        assertEquals(test, viewPage.getContent());
    }
}
