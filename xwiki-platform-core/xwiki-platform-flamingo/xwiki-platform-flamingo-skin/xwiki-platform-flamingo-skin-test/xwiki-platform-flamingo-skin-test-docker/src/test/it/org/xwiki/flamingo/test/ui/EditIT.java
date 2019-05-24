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
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.flamingo.skin.test.po.EditConflictModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.Assert.assertTrue;
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
        testToolBarButton(setup, reference,"Bold", "**%s**", "Text in Bold");
        testToolBarButton(setup, reference,"Italics", "//%s//", "Text in Italics");
        testToolBarButton(setup, reference,"Underline", "__%s__", "Text in Underline");
        testToolBarButton(setup, reference,"Internal Link", "[[%s]]", "Link Example");
        testToolBarButton(setup, reference,"Horizontal ruler", "\n----\n", "");
        testToolBarButton(setup, reference,"Attached Image", "[[image:%s]]", "example.jpg");
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
     * Ensure that the Save&View display a "Saving..." message and that the form is disabled before loading the
     * new page.
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
        setup.createPage(testReference, "", "");

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

        // Ensure to have the proper secret token for further tests.
        setup.recacheSecretToken();
    }

    @Test
    @Order(8)
    public void editWithConflict(TestUtils setup, TestReference testReference)
    {
        String testPageName = testReference.getLastSpaceReference().getName();
        setup.createPage(testReference, "", testPageName);

        String firstTab = setup.getDriver().getWindowHandle();
        WikiEditPage wikiEditPageTab1 = setup.gotoPage(testReference).editWiki();

        // Open link in a new window
        setup.getDriver().findElementByLinkText(testPageName).sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));

        // It might take a bit of time for the driver to know there's another window.
        setup.getDriver().waitUntilCondition(input -> input.getWindowHandles().size() == 2);
        Set<String> windowHandles = setup.getDriver().getWrappedDriver().getWindowHandles();
        String secondTab = null;
        for (String handle : windowHandles) {
            if (!handle.equals(firstTab)) {
                secondTab = handle;
            }
        }

        setup.getDriver().switchTo().window(secondTab);
        ViewPage viewPage = new ViewPage();
        WikiEditPage wikiEditPageTab2 = viewPage.editWiki();
        wikiEditPageTab2.setContent("A first edit from a tab.");
        wikiEditPageTab2.clickSaveAndContinue();

        setup.getDriver().switchTo().window(firstTab);
        wikiEditPageTab1.waitUntilPageJSIsLoaded();
        wikiEditPageTab1.setContent("A second edit from another tab");
        wikiEditPageTab1.clickSaveAndContinue(false);

        EditConflictModal editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals("Version conflict", editConflictModal.getTitle());
        assertEquals("1.2", editConflictModal.getVersionDiff());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-A <del>fir</del>s<del>t</del> edit from a tab<del>.</del>",
            "+A s<ins>econd</ins> edit from a<ins>nother</ins> tab"), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.forceSave(true);

        // Check that the force save indeed save the content
        viewPage = setup.gotoPage(testReference);
        assertEquals("A second edit from another tab", viewPage.getContent());
        wikiEditPageTab1 = viewPage.editWiki();

        // Go back on the other tab: the editor is still open, so we can create another conflict
        setup.getDriver().switchTo().window(secondTab);
        wikiEditPageTab2.waitUntilPageJSIsLoaded();
        wikiEditPageTab2.setContent("A third edit.");
        wikiEditPageTab2.clickSaveAndView(false);

        // check that the cancel/close buttons aren't changing the state of the page.
        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals("1.3", editConflictModal.getVersionDiff());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-A <del>secon</del>d edit<del> from another tab</del>",
            "+A <ins>thir</ins>d edit<ins>.</ins>"), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.cancelModal();

        wikiEditPageTab2.clickSaveAndContinue(false);
        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-A <del>secon</del>d edit<del> from another tab</del>",
            "+A <ins>thir</ins>d edit<ins>.</ins>"), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.close();

        wikiEditPageTab2.clickSaveAndView(false);
        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-A <del>secon</del>d edit<del> from another tab</del>",
            "+A <ins>thir</ins>d edit<ins>.</ins>"), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.forceSave(false);
        viewPage = new ViewPage();
        assertEquals("A third edit.", viewPage.getContent());

        // reopen the editor to continue
        wikiEditPageTab2 = viewPage.editWiki();
        setup.getDriver().switchTo().window(firstTab);
        wikiEditPageTab1.setContent("forth content");
        wikiEditPageTab1.clickSaveAndContinue(false);

        // check that after a force save we can continue to save normally.
        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals("2.1", editConflictModal.getVersionDiff());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-<del>A </del>th<del>ird</del> e<del>di</del>t<del>.</del>",
            "+<ins>for</ins>th <ins>cont</ins>e<ins>n</ins>t"), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.forceSave(true);
        wikiEditPageTab1.setContent("fifth content");
        wikiEditPageTab1.clickSaveAndContinue();
        viewPage = setup.gotoPage(testReference);
        assertEquals("fifth content", viewPage.getContent());

        // check that after a reload we obtain the last saved content in the editor and we can save normally.
        setup.getDriver().switchTo().window(secondTab);
        wikiEditPageTab2.waitUntilPageJSIsLoaded();
        wikiEditPageTab2.setContent("sixth content");
        wikiEditPageTab2.clickSaveAndContinue(false);
        editConflictModal = new EditConflictModal();
        assertTrue(editConflictModal.isDisplayed());
        assertEquals("2.3", editConflictModal.getVersionDiff());
        assertEquals(Arrays.asList("@@ -1,1 +1,1 @@", "-<del>f</del>i<del>f</del>th content",
            "+<ins>s</ins>i<ins>x</ins>th content"), editConflictModal.getDiff().getDiff("Content"));
        editConflictModal.reloadEditor();
        wikiEditPageTab2 = new WikiEditPage();
        assertEquals("fifth content", wikiEditPageTab2.getContent());
        wikiEditPageTab2.setContent("sixth content");
        viewPage = wikiEditPageTab2.clickSaveAndView();
        assertEquals("sixth content", viewPage.getContent());

        // close second tab and switch back to first for other tests.
        setup.getDriver().close();
        setup.getDriver().switchTo().window(firstTab);
    }

    /**
     * Test that a user who leave the editor by clicking on a link and come back won't have a conflict modal.
     */
    @Test
    @Order(9)
    public void editLeaveAndBack(TestUtils setup, TestReference testReference) throws InterruptedException
    {
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

        WYSIWYGEditPage wysiwygEditPage = setup.gotoPage(testReference).editWYSIWYG();
        wysiwygEditPage.setContent("fourth edit");
        wysiwygEditPage.clickSaveAndContinue();

        // object editor -> view page
        setup.getDriver().navigate().back();
        // view page -> wysiwyg editor
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
    public void editTitle255Characters(TestUtils setup, TestReference testReference)
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
    }
}
