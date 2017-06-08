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
package org.xwiki.test.wysiwyg;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;
import org.xwiki.test.wysiwyg.framework.XWikiExplorer;

import static org.junit.Assert.*;

public class LinkTest extends AbstractWysiwygTestCase
{
    public static final String MENU_LINK = "Link";

    public static final String MENU_WEB_PAGE = "Web Page...";

    public static final String MENU_EMAIL_ADDRESS = "Email Address...";

    public static final String MENU_WIKI_PAGE = "Wiki Page...";

    public static final String MENU_ATTACHMENT = "Attached File...";

    public static final String MENU_LINK_EDIT = "Edit Link...";

    public static final String MENU_LINK_REMOVE = "Remove Link";

    public static final String BUTTON_SELECT = "Select";

    public static final String BUTTON_LINK_SETTINGS = "Link Settings";

    public static final String BUTTON_CREATE_LINK = "Create Link";

    public static final String CURRENT_PAGE_TAB = "Current page";

    public static final String ALL_PAGES_TAB = "All pages";

    public static final String RECENT_PAGES_TAB = "My recent changes";

    public static final String SEARCH_TAB = "Search";

    public static final String STEP_EXPLORER = "xExplorerPanel";

    public static final String LABEL_INPUT_TITLE = "Type the label of the created link.";

    public static final String ERROR_MSG_CLASS = "xErrorMsg";

    public static final String ITEMS_LIST = "//div[contains(@class, 'xListBox')]";

    public static final String TREE_EXPLORER = "//div[contains(@class, 'xExplorer')]";

    public static final String FILE_UPLOAD_INPUT = "//input[contains(@class, 'gwt-FileUpload')]";

    public static final String PAGE_LOCATION = "Located in xwiki \u00BB %s \u00BB %s";

    public static final String NEW_PAGE_FROM_SEARCH_LOCATOR = "//div[contains(@class, 'xPagesSearch')]"
        + "//div[contains(@class, 'xListItem')]/div[contains(@class, 'xNewPagePreview')]";

    public static final String NEW_ATTACHMENT = "//div[@class = 'xAttachmentsSelector']"
        + "//div[contains(@class, \"xListItem\")]" + "/div[contains(@class, \"xNewFilePreview\")]";

    public static final String NEW_ATTACHMENT_SELECTED = "//div[@class = 'xAttachmentsSelector']"
        + "//div[contains(@class, \"xListItem-selected\")]" + "/div[contains(@class, \"xNewFilePreview\")]";

    public static final String ABSOLUTE_DOCUMENT_REFERENCE = "xwiki:%s.%s";

    public static final String ABSOLUTE_ATTACHMENT_REFERENCE = ABSOLUTE_DOCUMENT_REFERENCE + "@%s";

    /**
     * The object used to assert the state of the XWiki Explorer tree.
     */
    private XWikiExplorer explorer;

    @Override
    public void setUp()
    {
        super.setUp();
        
        this.explorer = new XWikiExplorer(getDriver());
    }

    /**
     * Test the basic feature for adding a link to an existing page.
     */
    @Test
    public void testCreateLinkToExistingPage()
    {
        String linkLabel = "x";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");

        openLinkDialog(MENU_WIKI_PAGE);
        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);

        explorer.waitForIt().findAndSelectPage("News");
        clickButtonWithText(BUTTON_SELECT);
        // make sure the existing page config parameters are loaded
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);

        // wait for the link dialog to close
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>doc:Blog.News]]");
    }

    /**
     * Test the basic feature for adding a link to a new page.
     */
    @Test
    public void testCreateLinkToNewPage()
    {
        String linkLabel = "a";
        String newPageName = "AliceInWonderwiki";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");
        openLinkDialog(MENU_WIKI_PAGE);
        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);
        explorer.waitForIt().findAndSelectPage("Home").selectNewPage("Main");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", newPageName);
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        // wait for the link dialog to close
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>doc:Main." + newPageName + "]]");
    }

    /**
     * Test the basic feature for adding a link to a new page in a new space.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3511">XWIKI-3511</a>
     */
    @Test
    public void testCreateLinkToNewPageInNewSpace()
    {
        String linkLabel = "b";
        String newSpace = "Bob";
        String newPage = "Cat";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");
        openLinkDialog(MENU_WIKI_PAGE);
        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);
        explorer.waitForIt().find("document:xwiki:" + newSpace + "." + newPage);

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        // wait for the link dialog to close
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>doc:" + newSpace + "." + newPage + "]]");
    }

    /**
     * Test the basic feature for adding a link to a web page.
     */
    @Test
    public void testCreateLinkToWebPage()
    {
        String linkLabel = "x";
        String url = "http://www.xwiki.org";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");

        openLinkDialog(MENU_WEB_PAGE);
        // ensure wizard step is loaded
        waitForStepToLoad("xLinkToUrl");
        typeInInput("Web page address", url);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>url:" + url + "]]");
    }

    /**
     * Test adding a link to a web page with a different label than the selected text.
     */
    @Test
    public void testCreateLinkToWebPageWithChangedLabel()
    {
        String linkLabel = "x";
        String url = "http://www.xwiki.org";
        typeText(linkLabel);
        selectAllContent();
        openLinkDialog(MENU_WEB_PAGE);
        // ensure wizard step is loaded
        waitForStepToLoad("xLinkToUrl");
        String newLabel = "xwiki rox";
        typeInInput(LABEL_INPUT_TITLE, newLabel);
        typeInInput("Web page address", url);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + newLabel + ">>url:" + url + "]]");
    }

    /**
     * Test the basic feature for adding a link to an email address.
     */
    @Test
    public void testCreateLinkToEmailAddress()
    {
        String linkLabel = "c";
        String email = "carol@xwiki.org";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");
        openLinkDialog(MENU_EMAIL_ADDRESS);

        typeInInput("Email address", email);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>mailto:" + email + "]]");
    }

    /**
     * Test adding a link by typing the link label instead of selecting it.
     */
    @Test
    public void testCreateLinkWithNewLabel()
    {
        String linkLabel = "xwiki";
        String linkURL = "www.xwiki.org";
        openLinkDialog(MENU_WEB_PAGE);

        typeInInput("Web page address", linkURL);
        typeInInput(LABEL_INPUT_TITLE, linkLabel);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>url:http://" + linkURL + "]]");
    }

    /**
     * Test that anchor label formatting is preserved.
     */
    @Test
    public void testCreateLinkPreservesLabelFormatting()
    {
        typeText("1");
        clickBoldButton();
        typeText("2");
        clickBoldButton();
        typeText("3");
        selectAllContent();
        openLinkDialog(MENU_WEB_PAGE);

        // test that the picked up label of the link is the right text
        assertEquals("123", getInputValue(LABEL_INPUT_TITLE));
        typeInInput("Web page address", "www.xwiki.org");
        clickButtonWithText(BUTTON_CREATE_LINK);

        waitForDialogToClose();
        switchToSource();
        assertSourceText("[[1**2**3>>url:http://www.xwiki.org]]");
    }

    /**
     * Test creating a link with some text around and then editing it. Test that the link type and parameters are
     * correctly read and that the wiki syntax is correctly generated.
     */
    @Test
    public void testCreateThenEditLink()
    {
        // put everything in a paragraph because editing in body is sometimes parsed wrong
        applyStyleTitle1();
        applyStylePlainText();
        typeText("1");
        String linkLabel = "xwiki";
        String linkURL = "http://www.xwiki.com";
        String newLinkURL = "http://www.xwiki.org";

        openLinkDialog(MENU_WEB_PAGE);

        typeInInput("Web page address", linkURL);
        typeInInput(LABEL_INPUT_TITLE, linkLabel);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        moveCaret("document.body.firstChild.childNodes[1].firstChild", 5);
        triggerToolbarUpdate();

        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(MENU_LINK_REMOVE));
        // unlink here should only move the caret out
        clickMenu(MENU_LINK_REMOVE);
        typeText("2");
        switchToSource();
        assertSourceText("1[[" + linkLabel + ">>url:" + linkURL + "]]2");
        switchToWysiwyg();

        select("document.body.firstChild", 1, "document.body.firstChild.childNodes[1].firstChild", 5);

        openLinkDialog(MENU_LINK_EDIT);

        assertEquals(linkLabel, getInputValue(LABEL_INPUT_TITLE));
        assertEquals(linkURL, getInputValue("Web page address"));

        typeInInput("Web page address", newLinkURL);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("1[[" + linkLabel + ">>url:" + newLinkURL + "]]2");
    }

    /**
     * Test creating and editing link around an image. Test that the displayed label is the alt text of the image and is
     * not editable, that the link parameters are correctly read, and that the wiki syntax is correctly generated.
     */
    @Test
    public void testCreateAndEditLinkOnImage()
    {
        clickMenu("Image");
        clickMenu("Attached Image...");

        waitForDialogToLoad();

        // Switch to "All Pages" tab.
        clickTab(ALL_PAGES_TAB);

        String imageSpace = "XWiki";
        waitForCondition("selenium.isElementPresent('" + ImageTest.SPACE_SELECTOR + "/option[@value=\"" + imageSpace
            + "\"]');");
        getSelenium().select(ImageTest.SPACE_SELECTOR, imageSpace);

        String imagePage = "AdminSheet";
        waitForCondition("selenium.isElementPresent('" + ImageTest.PAGE_SELECTOR + "/option[@value=\"" + imagePage
            + "\"]');");
        getSelenium().select(ImageTest.PAGE_SELECTOR, imagePage);

        getSelenium().click("//div[@class=\"xPageChooser\"]//button[text()=\"Update\"]");

        String imageSelector = "//div[@class=\"xImagesSelector\"]//img[@title=\"presentation.png\"]";
        waitForCondition("selenium.isElementPresent('" + imageSelector + "');");
        getSelenium().click(imageSelector);

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xImageConfig");
        clickButtonWithText("Insert Image");

        waitForDialogToClose();

        // Now add a link around the image we just inserted.
        openLinkDialog(MENU_WIKI_PAGE);
        // Select the link target page from the tree.
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);
        explorer.waitForIt().findAndSelectPage("Sandbox");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        assertEquals("presentation.png", getInputValue(LABEL_INPUT_TITLE));
        // Check that the link label is read-only.
        assertElementPresent("//input[@title=\"" + LABEL_INPUT_TITLE + "\" and @disabled=\"\"]");

        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[[[image:XWiki.AdminSheet@presentation.png]]>>doc:Sandbox.WebHome]]");
        switchToWysiwyg();

        // Move caret at the end and type some text.
        moveCaret("document.body", 1);
        typeText("x");

        openLinkDialog(MENU_WEB_PAGE);
        typeInInput(LABEL_INPUT_TITLE, "bar");
        typeInInput("Web page address", "http://bar.myxwiki.org");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        // Now go on and edit the inserted image.
        selectNode("document.body.firstChild.firstChild");
        openLinkDialog(MENU_LINK_EDIT);

        // Check the page selected in the XWiki Explorer tree.
        explorer.waitForPageSelected("Sandbox", "WebHome");
        explorer.findAndSelectPage("Space Index");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[[[image:XWiki.AdminSheet@presentation.png]]>>doc:Main.SpaceIndex]]x[[bar>>url:http://bar.myxwiki.org]]");
    }

    /**
     * Test that the link existence is detected correctly for a couple of cases of the selection around a link, and that
     * unlink executes correctly for these situations.
     */
    @Test
    public void testDetectAndUnlinkSelectedAnchor()
    {
        switchToSource();
        setSourceText("foo [[bar>>http://xwiki.org]] [[far>>Main.WebHome]] [[alice>>Main.NewPage]] "
            + "[[carol>>mailto:carol@xwiki.org]] [[b**o**b>>http://xwiki.org]] blog webhome [[Blog.WebHome]] "
            + "[[image:XWiki.AdminSheet@presentation.png>>Blog.Photos]]");
        switchToWysiwyg();

        // put selection inside first text
        moveCaret("document.body.firstChild.firstChild", 2);
        clickMenu(MENU_LINK);
        assertFalse(isMenuEnabled(MENU_LINK_REMOVE));
        assertTrue(isMenuEnabled(MENU_WIKI_PAGE));
        assertTrue(isMenuEnabled(MENU_WEB_PAGE));
        assertTrue(isMenuEnabled(MENU_EMAIL_ADDRESS));
        clickMenu(MENU_LINK);

        // put selection inside the first link
        moveCaret("document.body.firstChild.childNodes[1].firstChild", 2);
        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(MENU_LINK_EDIT));
        assertTrue(isMenuEnabled(MENU_LINK_REMOVE));
        // now unlink it
        clickMenu(MENU_LINK_REMOVE);

        // put selection around the second link, in the parent
        select("document.body.firstChild", 3, "document.body.firstChild", 4);
        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(MENU_LINK_EDIT));
        assertTrue(isMenuEnabled(MENU_LINK_REMOVE));
        // now unlink it
        clickMenu(MENU_LINK_REMOVE);

        // put selection with ends at the end of previous text and at the beginning of the next text
        select("document.body.firstChild.childNodes[4]", 1, "document.body.firstChild.childNodes[6]", 0);
        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(MENU_LINK_EDIT));
        assertTrue(isMenuEnabled(MENU_LINK_REMOVE));
        // now unlink it
        clickMenu(MENU_LINK_REMOVE);

        // put selection with one end inside the anchor and one end at the end of the text before or after
        select("document.body.firstChild.childNodes[6]", 1, "document.body.firstChild.childNodes[7].firstChild", 5);
        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(MENU_LINK_EDIT));
        assertTrue(isMenuEnabled(MENU_LINK_REMOVE));
        // now unlink it
        clickMenu(MENU_LINK_REMOVE);

        // put selection around the bold text inside a link label
        select("document.body.firstChild.childNodes[9].childNodes[1].firstChild", 0,
            "document.body.firstChild.childNodes[9].childNodes[1].firstChild", 1);
        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(MENU_LINK_EDIT));
        assertTrue(isMenuEnabled(MENU_LINK_REMOVE));
        // now unlink it
        clickMenu(MENU_LINK_REMOVE);

        // set selection starting in the text before the link and ending in the link
        select("document.body.firstChild.childNodes[12]", 5,
            "document.body.firstChild.childNodes[13].firstChild.firstChild", 4);
        clickMenu(MENU_LINK);
        assertFalse(isMenuEnabled(MENU_LINK_EDIT));
        assertFalse(isMenuEnabled(MENU_LINK_REMOVE));
        assertFalse(isMenuEnabled(MENU_WEB_PAGE));
        assertFalse(isMenuEnabled(MENU_EMAIL_ADDRESS));
        assertFalse(isMenuEnabled(MENU_WIKI_PAGE));
        clickMenu(MENU_LINK);

        // set selection in two different links
        select("document.body.firstChild.childNodes[13].firstChild.firstChild", 4,
            "document.body.firstChild.childNodes[15]", 1);
        clickMenu(MENU_LINK);
        assertFalse(isMenuEnabled(MENU_LINK_EDIT));
        assertFalse(isMenuEnabled(MENU_LINK_REMOVE));
        assertFalse(isMenuEnabled(MENU_WEB_PAGE));
        assertFalse(isMenuEnabled(MENU_EMAIL_ADDRESS));
        assertFalse(isMenuEnabled(MENU_WIKI_PAGE));

        switchToSource();
        assertSourceText("foo bar far alice carol b**o**b blog webhome [[Blog.WebHome]] "
            + "[[image:XWiki.AdminSheet@presentation.png>>Blog.Photos]]");
    }

    /**
     * Test editing a link which is the single text in a list item. This case is special because the delete command is
     * invoked upon replacing the link, which causes clean up of the list.
     */
    @Test
    public void testEditLinkInList()
    {
        switchToSource();
        setSourceText("* one\n* [[two>>http://www.xwiki.com]]\n** three");
        switchToWysiwyg();

        // now edit the link in the second list item
        moveCaret("document.body.firstChild.childNodes[1].firstChild.firstChild", 1);

        openLinkDialog(MENU_LINK_EDIT);

        // now check if the dialog has loaded correctly
        waitForStepToLoad("xLinkToUrl");
        assertEquals("two", getInputValue(LABEL_INPUT_TITLE));
        assertEquals("http://www.xwiki.com", getInputValue("Web page address"));
        typeInInput("Web page address", "http://www.xwiki.org");

        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("* one\n* [[two>>http://www.xwiki.org]]\n** three");
    }

    /**
     * Test that the link dialogs are correctly validated and alerts are displayed when mandatory fields are not filled
     * in.
     */
    @Test
    public void testValidationOnLinkInsert()
    {
        // try to create a link to an existing page without a label
        openLinkDialog(MENU_WIKI_PAGE);
        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);

        explorer.waitForIt().findAndSelectPage("Home");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        // try to create link without filling in the label
        clickButtonWithText(BUTTON_CREATE_LINK);

        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkConfig");

        // fill in the label and create link
        typeInInput(LABEL_INPUT_TITLE, "foo");
        clickButtonWithText(BUTTON_CREATE_LINK);

        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[foo>>doc:Main.WebHome]]");

        // clean up
        setSourceText("");
        switchToWysiwyg();

        // now try again with a new page link
        openLinkDialog(MENU_WIKI_PAGE);
        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);

        explorer.waitForIt().findAndSelectPage("Sandbox Test Page 1");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);

        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkConfig");

        // fill in the label and create link
        typeInInput(LABEL_INPUT_TITLE, "foo");
        clickButtonWithText(BUTTON_CREATE_LINK);

        switchToSource();
        assertSourceText("[[foo>>doc:Sandbox.TestPage1]]");

        // clean up
        setSourceText("");
        switchToWysiwyg();

        // now create a link to a web page
        openLinkDialog(MENU_WEB_PAGE);

        // test that initially 2 errors are displayed
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkToUrl");
        assertFieldErrorIsPresentInStep("The web page address was not set", "//input[@title='Web page address']",
            "xLinkToUrl");

        typeInInput("Web page address", "http://www.xwiki.org");
        clickButtonWithText(BUTTON_CREATE_LINK);

        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkToUrl");
        // now the web page address error is no longer there
        assertFieldErrorIsNotPresent("The web page address was not set", "//input[@title='Web page address']");

        // fill in the label and create link
        typeInInput(LABEL_INPUT_TITLE, "xwiki");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[xwiki>>url:http://www.xwiki.org]]");

        // clean up
        setSourceText("");
        switchToWysiwyg();

        // now create a link to an email page
        openLinkDialog(MENU_EMAIL_ADDRESS);

        clickButtonWithText(BUTTON_CREATE_LINK);

        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkToUrl");
        assertFieldErrorIsPresentInStep("The email address was not set", "//input[@title='Email address']",
            "xLinkToUrl");

        typeInInput(LABEL_INPUT_TITLE, "alice");
        clickButtonWithText(BUTTON_CREATE_LINK);

        assertFieldErrorIsPresentInStep("The email address was not set", "//input[@title='Email address']",
            "xLinkToUrl");
        assertFieldErrorIsNotPresent("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']");

        typeInInput("Email address", "alice@wonderla.nd");
        clickButtonWithText(BUTTON_CREATE_LINK);

        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[alice>>mailto:alice@wonderla.nd]]");
    }

    /**
     * Test that the link button is not enabled when the selection contains some block elements.
     */
    @Test
    public void testCannotCreateLinkAroundBlockElements()
    {
        setContent("<p>foo</p><p>bar</p>");
        select("document.body.firstChild.firstChild", 2, "document.body.childNodes[1].firstChild", 2);
        clickMenu(MENU_LINK);
        assertFalse(isMenuEnabled(MENU_WEB_PAGE));
        assertFalse(isMenuEnabled(MENU_WIKI_PAGE));
        assertFalse(isMenuEnabled(MENU_EMAIL_ADDRESS));
        assertFalse(isMenuEnabled(MENU_ATTACHMENT));
        assertFalse(isMenuEnabled(MENU_LINK_EDIT));
        assertFalse(isMenuEnabled(MENU_LINK_REMOVE));
    }

    /**
     * Test that the location of the link is preserved if we go back from the configuration step to the page selection
     * step.
     */
    @Test
    public void testLinkLocationIsPreservedOnPrevious()
    {
        String linkLabel = "x";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");

        openLinkDialog(MENU_WIKI_PAGE);
        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);

        explorer.waitForIt().findAndSelectPage("News");
        clickButtonWithText(BUTTON_SELECT);
        // make sure the existing page config parameters are loaded
        waitForStepToLoad("xLinkConfig");

        // now hit previous
        clickButtonWithText("Previous");
        // wait for tree to load
        waitForStepToLoad("xExplorerPanel");
        // Make sure the selection in the tree reflects the previously inserted values.
        explorer.waitForPageSelected("Blog", "News");

        // and now change it
        explorer.findAndSelectPage("Activity Stream");
        clickButtonWithText(BUTTON_SELECT);
        // make sure the existing page config parameters are loaded
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);

        // wait for the link dialog to close
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>doc:Main.Activity]]");
    }

    /**
     * Test the basic feature of adding a link to an attached file with the label from the selected text.
     */
    @Test
    public void testCreateLinkToAttachment()
    {
        String linkLabel = "x";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");

        openLinkDialog(MENU_ATTACHMENT);

        // click the tree explorer tab
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad("xExplorerPanel");

        explorer.waitForIt().findAndSelectAttachment("export.png");

        clickButtonWithText(BUTTON_SELECT);
        // make sure the existing page config parameters are loaded
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);

        // wait for the link dialog to close
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>attach:XWiki.AdminSheet@export.png]]");
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-8440">XWIKI-8440: Unable to link to an attachment from the
     *      current page in the WYSIWYG Editor using "All Pages" tab</a>
     */
    @Test
    public void testCreateLinkToAttachmentFromCurrentPage()
    {
        // Edit a page that has an attachment.
        open("Sandbox", "WebHome", "edit", "editor=wysiwyg");
        waitForEditorToLoad();

        // Insert a link to the attached file using the All Pages tab.
        openLinkDialog(MENU_ATTACHMENT);
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad("xExplorerPanel");

        explorer.waitForIt().findAndSelectAttachment("XWikiLogo.png");

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertTrue(getSourceText().startsWith("[[XWikiLogo.png>>attach:XWikiLogo.png"));
    }

    /**
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-8465">Unable to upload a new attachment using the "All pages"
     *      tab in the WYSIWYG editor</a>
     */
    @Test
    public void testCreateLinkToNewAttachment()
    {
        // We have to save the page in order to appear in the tree.
        clickEditSaveAndContinue();
        clickEditPageInWysiwyg();
        waitForEditorToLoad();

        openLinkDialog(MENU_ATTACHMENT);
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad("xExplorerPanel");

        String spaceName = this.getClass().getSimpleName();
        String pageName = getTestMethodName();
        explorer.waitForAttachmentsSelected(spaceName, pageName).selectNewAttachment(spaceName, pageName);

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xUploadPanel");
        // TODO: Test real file upload.
    }

    /**
     * Test the basic feature of adding a link to an attached file, configuring its parameters in the parameter panel.
     */
    @Test
    public void testCreateLinkToAttachmentWithParameters()
    {
        String linkLabel = "XWiki.org Logo";
        String linkTooltip = "Download XWiki's Logo";
        openLinkDialog(MENU_ATTACHMENT);

        // click the tree explorer tab
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad("xExplorerPanel");

        explorer.waitForIt().findAndSelectAttachment("XWikiLogo.png");

        clickButtonWithText(BUTTON_SELECT);
        // make sure the existing page config parameters are loaded
        waitForStepToLoad("xLinkConfig");
        // fill in the link label and title
        typeInInput(LABEL_INPUT_TITLE, linkLabel);
        typeInInput("Type the tooltip of the created link, which appears when mouse is over the link.", linkTooltip);

        clickButtonWithText(BUTTON_CREATE_LINK);

        // wait for the link dialog to close
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>attach:Sandbox.WebHome@XWikiLogo.png||title=\"" + linkTooltip + "\"]]");
    }

    /**
     * Test that he creation of a link to an attached file is validated correctly.
     */
    @Test
    public void testValidationOnLinkToAttachment()
    {
        String linkLabel = "boo";

        openLinkDialog(MENU_ATTACHMENT);

        // Select the attachment from a different page.
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad("xExplorerPanel");

        String attachSpace = "XWiki";
        String attachPage = "AdminSheet";
        String attachment = "users.png";

        // Get an error from not inserting the attachment name.
        explorer.waitForIt().findAndSelectPage("Administration");

        clickButtonWithText(BUTTON_SELECT);

        assertFieldErrorIsPresentInStep("No attachment was selected", TREE_EXPLORER, "xExplorerPanel");

        // Select an attachment.
        explorer.openAttachments(attachSpace, attachPage).selectAttachment(attachSpace, attachPage, attachment);

        // Move to the next step: link configuration.
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");

        // The link label should be the attachment name.
        assertEquals(attachment, getInputValue(LABEL_INPUT_TITLE));
        // Try to create a link with an empty label.
        typeInInput(LABEL_INPUT_TITLE, "");
        clickButtonWithText(BUTTON_CREATE_LINK);

        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkConfig");

        typeInInput(LABEL_INPUT_TITLE, linkLabel);
        clickButtonWithText(BUTTON_CREATE_LINK);

        // Wait for the link dialog to close.
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>attach:" + attachSpace + "." + attachPage + "@" + attachment + "]]");
    }

    /**
     * Test editing an existing link to an attachment
     */
    @Test
    public void testEditLinkToAttachment()
    {
        switchToSource();
        setSourceText("[[foobar>>attach:Sandbox.WebHome@XWikiLogo.png]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForAttachmentSelected("Sandbox", "WebHome", "XWikiLogo.png");
        explorer.findAndSelectAttachment("export.png");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[foobar>>attach:XWiki.AdminSheet@export.png]]");
    }

    /**
     * @see XWIKI-7237: mailto is not stripped when editing a link to an e-mail address
     */
    @Test
    public void testEditLinkToEmailAddress()
    {
        switchToSource();
        setSourceText("[[test>>mailto:test@xwiki.org]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 2);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xLinkToUrl");
        assertEquals("test@xwiki.org", getInputValue("Email address"));
        typeInInput("Email address", "test@gmail.com");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[test>>mailto:test@gmail.com]]");
    }

    /**
     * Test that editing a link with custom parameters set from wiki syntax preserves the parameters of the link.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3568">XWIKI-3568</a>
     */
    @Test
    public void testEditLinkPreservesCustomParameters()
    {
        switchToSource();
        setSourceText("[[foobar>>Main.Activity||class=\"foobarLink\"]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        openLinkDialog(MENU_LINK_EDIT);

        waitForStepToLoad("xExplorerPanel");
        explorer.waitForPageSelected("Main", "Activity");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, "barfoo");
        typeInInput("Type the tooltip of the created link, which appears when mouse is over the link.", "Foo and bar");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[barfoo>>Main.Activity||class=\"foobarLink\" title=\"Foo and bar\"]]");
    }

    /**
     * Test creating a link to open in a new page.
     */
    @Test
    public void testCreateLinkToOpenInNewWindow()
    {
        String linkLabel = "XWiki rox";
        String url = "http://www.xwiki.org";

        openLinkDialog(MENU_WEB_PAGE);
        // ensure wizard step is loaded
        waitForStepToLoad("xLinkToUrl");
        typeInInput("Web page address", url);
        typeInInput(LABEL_INPUT_TITLE, linkLabel);
        // open in new window
        getSelenium().check("//div[contains(@class, 'xLinkToUrl')]//span[contains(@class, 'gwt-CheckBox')]/input");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>url:" + url + "||rel=\"__blank\"]]");
        switchToWysiwyg();

        // now edit
        moveCaret("document.body.firstChild.firstChild", 4);
        openLinkDialog(MENU_LINK_EDIT);

        assertEquals(linkLabel, getInputValue(LABEL_INPUT_TITLE));
        assertEquals(url, getInputValue("Web page address"));
        assertTrue(isChecked("//div[contains(@class, 'xLinkToUrl')]//span[contains(@class, 'gwt-CheckBox')]/input"));
    }

    /**
     * Test that quotes in link tooltips are correctly escaped.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3569">XWIKI-3569</a>
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3569">XWIKI-3575</a>
     */
    @Test
    public void testQuoteInLinkTooltip()
    {
        String linkLabel = "x";
        String url = "http://www.xwiki.org";
        String tooltip = "our xwiki \"rox\"";
        String tooltipTitle = "Type the tooltip of the created link, which appears when mouse is over the link.";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");
        openLinkDialog(MENU_WEB_PAGE);
        // ensure wizard step is loaded
        waitForStepToLoad("xLinkToUrl");
        typeInInput(tooltipTitle, tooltip);
        typeInInput("Web page address", url);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>url:" + url + "||title=\"our xwiki ~\"rox~\"\"]]");
        switchToWysiwyg();

        // now test the link is correctly parsed back
        moveCaret("document.body.firstChild.firstChild", 1);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xLinkToUrl");
        assertEquals(tooltip, getInputValue(tooltipTitle));
    }

    /**
     * Test that the default selection is set to the current page when opening the wizard to create a link to a wiki
     * page.
     */
    @Test
    public void testDefaultWikipageExplorerSelection()
    {
        // make sure this page is saved so that the tree can load the reference to it
        clickEditSaveAndContinue();

        String currentSpace = this.getClass().getSimpleName();
        String currentPage = getTestMethodName();

        String newSpace = "XWiki";
        String newPage = "AdminSheet";
        // check the wikipage link dialog
        openLinkDialog(MENU_WIKI_PAGE);

        // check the recent changes selection
        waitForStepToLoad("xSelectorAggregatorStep");

        // test that the default open tab is the recent changes tab
        assertElementPresent("//div[contains(@class, 'gwt-TabBarItem-selected')]/div[.='" + RECENT_PAGES_TAB + "']");

        waitForStepToLoad("xPagesSelector");
        // Test that the selected element is the edited page.
        assertElementPresent("//div[contains(@class, 'xPagesRecent')]"
            + "//div[contains(@class, 'xListItem-selected')]//div[. = '"
            + String.format(PAGE_LOCATION, currentSpace, currentPage) + "']");

        // get the all pages tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);

        explorer.waitForPageSelected(currentSpace, currentPage).findAndSelectPage("Administration");
        closeDialog();
        waitForDialogToClose();

        // now type something and check second display of the dialog, that it stays to the last inserted page
        typeText("z");
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForPageSelected(newSpace, newPage);
        closeDialog();
        waitForDialogToClose();
    }

    /**
     * Test the creation of a link to a recent page (the current page, saved).
     */
    @Test
    public void testCreateLinkToRecentPage()
    {
        // make sure this page is saved so that the recent pages can load reference to it
        clickEditSaveAndContinue();

        openLinkDialog(MENU_WIKI_PAGE);
        // The tab with the recently modified pages should be selected.
        waitForStepToLoad("xSelectorAggregatorStep");
        waitForStepToLoad("xPagesSelector");
        // The currently edited page should be selected.
        assertElementPresent("//div[contains(@class, 'xPagesRecent')]"
            + "//div[contains(@class, 'xListItem-selected')]//div[. = '"
            + String.format(PAGE_LOCATION, getClass().getSimpleName(), getTestMethodName()) + "']");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        String label = "barfoo";
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:" + getTestMethodName() + "]]");
    }

    /**
     * Test the creation of a link to a new page in the current space, from the default tab in the link dialog.
     */
    @Test
    public void testCreateLinkToNewPageInCurrentSpace()
    {
        String newPageName = "NewPage";
        String label = "new page label";

        openLinkDialog(MENU_WIKI_PAGE);

        // check the recent changes selection
        waitForStepToLoad("xSelectorAggregatorStep");
        waitForStepToLoad("xPagesSelector");
        // test that the selected element is the new page element
        assertElementPresent("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", newPageName);
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:" + newPageName + "]]");
    }

    /**
     * Tests the default selection for the search tab.
     */
    @Test
    public void testDefaultSearchSelection()
    {
        // check the wikipage link dialog
        openLinkDialog(MENU_WIKI_PAGE);

        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab(SEARCH_TAB);
        waitForStepToLoad("xPagesSearch");
        // test that the selected element is the new page element
        assertElementPresent("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");

        closeDialog();
        waitForDialogToClose();
    }

    /**
     * Test adding a link to a page from the search tab.
     */
    @Test
    public void testCreateLinkToSearchedPage()
    {
        String label = "foobar";
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab(SEARCH_TAB);
        waitForStepToLoad("xPagesSearch");

        // Search for "Main.WebHome".
        typeInInput("Type a keyword to search for a wiki page", "Main.WebHome");
        clickButtonWithText("Search");

        // Wait for the target page to appear in the list and then select it.
        String targetPageLocator =
            "//div[contains(@class, 'xPagesSearch')]//div[contains(@class, 'xListItem')]//div[.='"
                + String.format(PAGE_LOCATION, "Main", "WebHome") + "']";
        waitForElement(targetPageLocator);
        getSelenium().click(targetPageLocator);

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:Main.WebHome]]");
    }

    /**
     * Tests the creation of a link in the current space from the search pages dialog.
     */
    @Test
    public void testCreateLinkToNewPageInCurrentSpaceFromSearch()
    {
        String newPageName = "AnotherNewPage";
        String label = "x";

        typeText(label);
        // Select the text.
        selectNodeContents("document.body.firstChild");

        openLinkDialog(MENU_WIKI_PAGE);

        // check the recent changes selection
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab(SEARCH_TAB);
        waitForStepToLoad("xPagesSearch");
        // test that the selected element is the new page element
        assertElementPresent("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", newPageName);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:" + newPageName + "]]");
    }

    /**
     * Tests that the link attachments step is loaded on the current page attachments every time it's displayed.
     */
    @Test
    public void testDefaultAttachmentSelectorSelection()
    {
        // make sure this page is saved so that the tree can load the reference to it
        clickEditSaveAndContinue();

        String currentSpace = this.getClass().getSimpleName();
        String currentPage = getTestMethodName();

        // check the wikipage link dialog
        openLinkDialog(MENU_ATTACHMENT);

        waitForStepToLoad("xAttachmentsSelector");
        // test that there is a "new attachment" option
        assertElementPresent(NEW_ATTACHMENT);

        clickTab(ALL_PAGES_TAB);
        explorer.waitForAttachmentsSelected(currentSpace, currentPage);
        closeDialog();
        waitForDialogToClose();

        // now type something and check second display of the dialog, that it opens on the current page
        typeText("z");
        openLinkDialog(MENU_ATTACHMENT);
        waitForStepToLoad("xAttachmentsSelector");
        // test that there is a "new attachment" option
        assertElementPresent(NEW_ATTACHMENT);
        closeDialog();
        waitForDialogToClose();
    }

    /**
     * Test that a relative link is correctly edited.
     * 
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-3676">XWIKI-3676</a>
     */
    @Test
    public void testEditRelativeLink()
    {
        // Edit a page in the Main space because we know pages that already exit there.
        String currentSpace = "Main";
        open(currentSpace, getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();

        String pageToLinkTo = "SpaceIndex";
        switchToSource();
        setSourceText("[[the main page>>" + pageToLinkTo + "]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        openLinkDialog(MENU_LINK_EDIT);

        waitForStepToLoad("xExplorerPanel");
        explorer.waitForPageSelected(currentSpace, pageToLinkTo);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, "space index");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[space index>>" + pageToLinkTo + "]]");
    }

    /**
     * Test that a relative link to a file attachment is correctly edited
     */
    @Test
    public void testEditRelativeLinkToAttachment()
    {
        // Edit a page in the Sandbox space because we know pages that already exit there and have attachments.
        String currentSpace = "Sandbox";
        open(currentSpace, getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();

        String pageToLinkTo = "WebHome";
        String fileToLinkTo = "XWikiLogo.png";

        switchToSource();
        setSourceText("[[XWiki Logo>>attach:" + pageToLinkTo + "@" + fileToLinkTo + "]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForAttachmentSelected(currentSpace, pageToLinkTo, fileToLinkTo);

        // check the current page step is correctly loaded when we switch to it
        clickTab(CURRENT_PAGE_TAB);
        waitForStepToLoad("xAttachmentsSelector");
        // test that there is a "new attachment" option
        assertElementPresent(NEW_ATTACHMENT);

        // switch back to the tree
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad("xExplorerPanel");
        // test that the position in the tree was preserved
        explorer.waitForAttachmentSelected(currentSpace, pageToLinkTo, fileToLinkTo);

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, "XWiki.org Logo");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[XWiki.org Logo>>attach:" + pageToLinkTo + "@" + fileToLinkTo + "]]");

        // ensure this opens on the current page selector
        setSourceText("[[attach.png>>attach:attach.png]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xAttachmentsSelector");
        // The option for uploading a new attachment should be selected.
        assertElementPresent(NEW_ATTACHMENT_SELECTED);
        closeDialog();
        waitForDialogToClose();
    }

    /**
     * Test that when no option is selected in the current page attachments selector and a "select" is tried, an alert
     * is displayed to show the error.
     */
    @Test
    public void testValidationOnCurrentPageAttachmentsSelector()
    {
        switchToSource();
        setSourceText("[[Export>>attach:XWiki.AdminSheet@export.png]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 3);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForAttachmentSelected("XWiki", "AdminSheet", "export.png");
        explorer.findAndSelectAttachment("import.png");

        clickTab(CURRENT_PAGE_TAB);

        waitForStepToLoad("xAttachmentsSelector");
        // The option to upload a new attachment should be selected.
        assertElementPresent(NEW_ATTACHMENT_SELECTED);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xUploadPanel");
        assertFieldErrorIsNotPresentInStep("xUploadPanel");
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        assertFieldErrorIsPresentInStep("The file path was not set", FILE_UPLOAD_INPUT, "xUploadPanel");

        closeDialog();
    }

    /**
     * Test that editing a link and not changing its location preserves a full reference and does not transform it into
     * a relative one.
     */
    @Test
    public void testEditLinkPreservesFullReferences()
    {
        // Edit a page in the Sandbox space because we know pages that already exit there and have attachments.
        open("Sandbox", getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();

        switchToSource();
        // Insert links to resources from the same space, using full references.
        setSourceText("[[bob>>Sandbox.WebHome]] [[alice>>Sandbox.NewPage]] "
            + "[[carol>>attach:Sandbox.WebHome@XWikiLogo.png]]");
        switchToWysiwyg();

        // Edit first link, a link to an existing page.
        moveCaret("document.body.firstChild.firstChild.firstChild", 1);
        openLinkDialog(MENU_LINK_EDIT);

        waitForStepToLoad("xExplorerPanel");
        explorer.waitForPageSelected("Sandbox", "WebHome");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        // Edit second link, a link to a new page.
        moveCaret("document.body.firstChild.childNodes[2].firstChild", 2);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForFinderValue("document:xwiki:Sandbox.NewPage.WebHome");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        // Edit third link, a link to an existing file.
        moveCaret("document.body.firstChild.childNodes[4].firstChild", 2);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForAttachmentSelected("Sandbox", "WebHome", "XWikiLogo.png");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[bob>>Sandbox.WebHome]] [[alice>>Sandbox.NewPage]] "
            + "[[carol>>attach:Sandbox.WebHome@XWikiLogo.png]]");
    }

    /**
     * Test that the error markers are removed for the following displays of an external link step with an error: either
     * the dialog with the error is closed by canceling or by selecting a correct value and continuing, upon return to
     * the error dialog, the error message and markers are now hidden.
     */
    @Test
    public void testErrorIsHiddenOnNextDisplayOfExternalLink()
    {
        // for a web page
        openLinkDialog(MENU_WEB_PAGE);
        typeInInput("Web page address", "http://www.xwiki.org");
        clickButtonWithText(BUTTON_CREATE_LINK);
        // check that an error is present
        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkToUrl");
        // cancel everything
        closeDialog();
        // now open a new one
        openLinkDialog(MENU_WEB_PAGE);
        // check that the error is no longer there
        assertElementNotPresent(ERROR_MSG_CLASS);
        closeDialog();

        // for an email
        openLinkDialog(MENU_EMAIL_ADDRESS);
        typeInInput("Email address", "xwiki@xwiki.com");
        clickButtonWithText(BUTTON_CREATE_LINK);
        // check that an error is present
        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[@title='" + LABEL_INPUT_TITLE
            + "']", "xLinkToUrl");
        // cancel everything
        closeDialog();
        // now open a new one
        openLinkDialog(MENU_EMAIL_ADDRESS);
        // check that the error is no longer there
        assertElementNotPresent(ERROR_MSG_CLASS);
        closeDialog();
    }

    /**
     * Test that the error markers are removed for the following displays of an attachment link step with an error:
     * either the dialog with the error is closed by canceling or by selecting a correct value and continuing, upon
     * return to the error dialog, the error message and markers are now hidden.
     */
    @Test
    public void testErrorIsHiddenOnNextDisplayOfAttachmentLink()
    {
        // Get an error at the upload step and check that it's hidden on next display.
        openLinkDialog(MENU_ATTACHMENT);
        waitForStepToLoad("xAttachmentsSelector");
        // The option to upload a new attachment should be selected.
        assertElementPresent(NEW_ATTACHMENT_SELECTED);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xUploadPanel");
        // Click the upload button without selecting a file.
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        assertFieldErrorIsPresentInStep("The file path was not set", FILE_UPLOAD_INPUT, "xUploadPanel");
        // Go to the previous step and come back: the error should be gone.
        clickButtonWithText("Previous");
        // We need to wait for the previous step to load because it is reinitialized (the list of attachments is fetched
        // again with an asynchronous request).
        waitForStepToLoad("xAttachmentsSelector");
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsNotPresentInStep("xUploadPanel");
        // Get the error again to check that closing it and displaying this step again makes it go away.
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The file path was not set", FILE_UPLOAD_INPUT, "xUploadPanel");
        closeDialog();
        openLinkDialog(MENU_ATTACHMENT);
        waitForStepToLoad("xAttachmentsSelector");
        assertElementPresent(NEW_ATTACHMENT_SELECTED);
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsNotPresentInStep("xUploadPanel");
    }

    /**
     * Test that the error markers are removed for the following displays of a wiki page link step with an error: either
     * the dialog with the error is closed by canceling or by selecting a correct value and continuing, upon return to
     * the error dialog, the error message and markers are now hidden.
     */
    @Test
    public void testErrorIsHiddenOnNextDisplayOfWikipageLink()
    {
        // 1/ get an error on the new page step, fix it, go to previous, next => error not displayed anymore. Get
        // another error, go next, previous, error should not be there anymore. Get another error, close dialog. Open
        // again, get there, the error should not be displayed.
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepAggregatorAndAssertSelectedStep(RECENT_PAGES_TAB);
        assertElementPresent("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkToNewPage");
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        assertFieldErrorIsPresentInStep("The name of the new page was not set", "//input", "xLinkToNewPage");
        clickButtonWithText("Previous");
        waitForStepAggregatorAndAssertSelectedStep(RECENT_PAGES_TAB);
        assertElementPresent("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");
        clickButtonWithText(BUTTON_SELECT);
        // error not present on coming back from previous
        assertFieldErrorIsNotPresentInStep("xLinkToNewPage");
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The name of the new page was not set", "//input", "xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", "NewPage");
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText("Previous");
        // error not present when coming back from next
        assertFieldErrorIsNotPresentInStep("xLinkToNewPage");
        // check the content of the field
        assertEquals("NewPage", getSelenium().getValue("//div[contains(@class, 'xLinkToNewPage')]//input"));
        // get error again
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", "");
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The name of the new page was not set", "//input", "xLinkToNewPage");
        closeDialog();
        // open again, check the error is not still there
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepAggregatorAndAssertSelectedStep(RECENT_PAGES_TAB);
        assertElementPresent("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");
        clickButtonWithText(BUTTON_SELECT);
        // error not present when re-creating a link
        assertFieldErrorIsNotPresentInStep("xLinkToNewPage");
        closeDialog();
        resetContent();

        // 2/ get to the link config and get an error on the label -> previous, next, error should not be there anymore.
        // close everything, open again, error should not be there anymore. get error, fix it, add the link, on new
        // dialog error should not be there anymore
        switchToSource();
        setSourceText("[[the sandbox>>Sandbox.WebHome]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 4);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepAggregatorAndAssertSelectedStep(ALL_PAGES_TAB);
        explorer.waitForPageSelected("Sandbox", "WebHome");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, "");
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[position() = 1]",
            "xLinkConfig");
        // previous, next => error is not present
        clickButtonWithText("Previous");
        waitForStepAggregatorAndAssertSelectedStep(ALL_PAGES_TAB);
        explorer.waitForPageSelected("Sandbox", "WebHome");
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsNotPresentInStep("xLinkConfig");
        // error, again, to close this time
        typeInInput(LABEL_INPUT_TITLE, "");
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[position() = 1]",
            "xLinkConfig");
        closeDialog();
        // now again, check that the error is no longer there
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepAggregatorAndAssertSelectedStep(ALL_PAGES_TAB);
        explorer.waitForPageSelected("Sandbox", "WebHome");
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsNotPresentInStep("xLinkConfig");
        // get an error
        typeInInput(LABEL_INPUT_TITLE, "");
        clickButtonWithText(BUTTON_CREATE_LINK);
        assertFieldErrorIsPresentInStep("The label of the link cannot be empty", "//input[position() = 1]",
            "xLinkConfig");
        // now go ahead, edit the link
        typeInInput(LABEL_INPUT_TITLE, "PageNew");
        clickButtonWithText(BUTTON_CREATE_LINK);
        // now open again, check error is not there anymore
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepAggregatorAndAssertSelectedStep(ALL_PAGES_TAB);
        explorer.waitForPageSelected("Sandbox", "WebHome");
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsNotPresentInStep("xLinkConfig");
        closeDialog();

        // 5/ get to the tree explorer, don't select any page, get an error. Fill in, next, previous -> error is hidden.
        // Get error again, close. Open and error is hidden
        switchToSource();
        setSourceText("[[the blog>>Blog.WebHome]]");
        switchToWysiwyg();
        moveCaret("document.body.firstChild.firstChild.firstChild", 4);
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepAggregatorAndAssertSelectedStep(ALL_PAGES_TAB);
        explorer.waitForPageSelected("Blog", "WebHome");
        // Clear the selection.
        explorer.togglePageSelection("Blog", "WebHome");
        // At this point no page is selected.
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsPresentInStep("No page was selected", TREE_EXPLORER, "xExplorerPanel");
        explorer.findAndSelectPage("Blog");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText("Previous");
        // The previously selected page should still be selected.
        explorer.waitForPageSelected("Blog", "WebHome");
        assertFieldErrorIsNotPresentInStep("xExplorerPanel");
        // Repeat the steps to get the validation error.
        // Select a space so that no page is selected.
        explorer.togglePageSelection("Blog", "WebHome");
        // At this point no page is selected.
        clickButtonWithText(BUTTON_SELECT);
        assertFieldErrorIsPresentInStep("No page was selected", TREE_EXPLORER, "xExplorerPanel");
        closeDialog();
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepAggregatorAndAssertSelectedStep(ALL_PAGES_TAB);
        assertFieldErrorIsNotPresentInStep("xExplorerPanel");
        closeDialog();
    }

    /**
     * Test fast navigation for adding a link to an attachment: double click and enter in the list of attachments
     * advance to the next step.
     */
    @Test
    public void testFastNavigationToSelectAttachment()
    {
        // can't test but the current page attachment selector, the tree doesn't receive the click events
        // double click
        openLinkDialog(MENU_ATTACHMENT);
        waitForStepToLoad("xAttachmentsSelector");
        getSelenium().click(NEW_ATTACHMENT);
        getSelenium().doubleClick(NEW_ATTACHMENT);
        waitForStepToLoad("xUploadPanel");
        closeDialog();

        // enter
        openLinkDialog(MENU_ATTACHMENT);
        waitForStepToLoad("xAttachmentsSelector");
        getSelenium().click(NEW_ATTACHMENT);
        getSelenium().typeKeys(ITEMS_LIST, "\\13");
        waitForStepToLoad("xUploadPanel");
        closeDialog();
    }

    /**
     * Test fast navigation for adding a link to a recent page: double click and enter on a page advance to the next
     * step.
     */
    @Test
    public void testFastNavigationToSelectRecentPage()
    {
        // 1. link to existing page, double click
        // make sure this page is saved so that the recent pages can load reference to it
        clickEditSaveAndContinue();
        String currentPageLocation = String.format(PAGE_LOCATION, this.getClass().getSimpleName(), getTestMethodName());
        String label = "barfoo";
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepAggregatorAndAssertSelectedStep(RECENT_PAGES_TAB);
        getSelenium().click(
            "//div[contains(@class, 'xPagesSelector')]//div[contains(@class, 'gwt-Label') and .='"
                + currentPageLocation + "']");
        getSelenium().doubleClick(
            "//div[contains(@class, 'xPagesSelector')]//div[contains(@class, 'gwt-Label') and .='"
                + currentPageLocation + "']");
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:" + getTestMethodName() + "]]");

        setSourceText("");
        switchToWysiwyg();

        // 2. link to new page in current space, with enter
        String newPageName = "NewPage";
        label = "foobar";

        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepAggregatorAndAssertSelectedStep(RECENT_PAGES_TAB);
        // select the current page
        getSelenium().click("//div[contains(@class, 'xListItem')]/div[contains(@class, 'xNewPagePreview')]");
        getSelenium().typeKeys(ITEMS_LIST, "\\13");
        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", newPageName);
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:" + newPageName + "]]");
    }

    /**
     * Test fast navigation for adding a link to a searched for page: double click and enter on a page advance to the
     * next step.
     */
    @Test
    public void testFastNavigationToSelectSearchedPage()
    {
        // 1. Link to existing page. Use Enter key to select the target page.
        String label = "foobar";
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab(SEARCH_TAB);
        waitForStepToLoad("xPagesSearch");

        // Search for "Main.WebHome" page.
        typeInInput("Type a keyword to search for a wiki page", "Main.WebHome");
        clickButtonWithText("Search");

        // Wait for target page to appear in the list, then select it using the Enter key.
        String targetPageLocator =
            "//div[contains(@class, 'xPagesSearch')]//div[contains(@class, 'xListItem')]//div[.='"
                + String.format(PAGE_LOCATION, "Main", "WebHome") + "']";
        waitForElement(targetPageLocator);
        getSelenium().click(targetPageLocator);
        getSelenium().typeKeys("//div[contains(@class, 'xPagesSearch')]" + ITEMS_LIST, "\\13");

        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:Main.WebHome]]");

        setSourceText("");
        switchToWysiwyg();

        // 2. Link to a new page. Use double click to select the target page.
        String newPageName = "PageNew";
        label = "barfoo";
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab(SEARCH_TAB);
        waitForStepToLoad("xPagesSearch");
        getSelenium().click(NEW_PAGE_FROM_SEARCH_LOCATOR);
        getSelenium().doubleClick(NEW_PAGE_FROM_SEARCH_LOCATOR);
        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", newPageName);
        clickButtonWithText(BUTTON_LINK_SETTINGS);
        waitForStepToLoad("xLinkConfig");
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + label + ">>doc:" + newPageName + "]]");
    }

    /**
     * Tests if an empty link is filtered.
     */
    @Test
    public void testFilterEmptyLink()
    {
        typeText("ab");
        // Select the text.
        selectNodeContents("document.body.firstChild");
        // Make it bold.
        clickBoldButton();
        // Make it a link to a web page.
        openLinkDialog(MENU_WEB_PAGE);
        // Ensure wizard step is loaded.
        waitForStepToLoad("xLinkToUrl");
        typeInInput("Web page address", "http://www.xwiki.org");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();
        // Place the caret inside the text.
        moveCaret("document.body.firstChild.firstChild.firstChild", 1);
        // Remove the bold style around the caret (not for the entire link).
        clickBoldButton();
        // The link must have been split in three.
        assertEquals(3L, getRichTextArea().executeScript("return document.body.getElementsByTagName('a').length"));
        // Check the source text.
        switchToSource();
        assertSourceText("**[[a>>url:http://www.xwiki.org]][[b>>url:http://www.xwiki.org]]**");
    }

    /**
     * @see XWIKI-4536: wiki pages can no longer be edited when there are links to Wiki pages within a section heading
     */
    @Test
    public void testPreviewHeadingContainingLinkWithNoLabel()
    {
        switchToSource();
        setSourceText("= x [[y]] =");
        switchToWysiwyg();
        clickEditPreview();
        assertElementNotPresent("//div[@class = 'errormessage']");
        clickBackToEdit();
    }

    /**
     * Creates a link to a recently modified page that has special characters in its name.
     */
    @Test
    public void testCreateLinkToRecentlyModifiedPageWithSpecialCharactersInName()
    {
        // Create a page with special characters in its name.
        String spaceName = this.getClass().getSimpleName() + ":s.t@u?v=w&x=y#z";
        String escapedSpaceName = spaceName.replaceAll("([\\:\\.])", "\\\\$1");
        String pageName = getTestMethodName() + ":a.b@c?d=e&f=g#h";
        String escapedPageName = pageName.replace(".", "\\.");
        String linkReference = String.format("%s.%s", escapedSpaceName, escapedPageName);
        open("Main", "WebHome");
        createPage(spaceName, pageName, "");

        // Come back to the edited page.
        open(this.getClass().getSimpleName(), getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();

        // Create a link to the created page.
        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        waitForStepToLoad("xPagesSelector");
        String createdPageLocator =
            "//div[contains(@class, 'xListItem')]//div[contains(@class, 'gwt-Label') and .='"
                + String.format(PAGE_LOCATION, spaceName, pageName) + "']";
        waitForElement(createdPageLocator);
        getSelenium().click(createdPageLocator);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        String label = "Label";
        typeInInput(LABEL_INPUT_TITLE, label);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        assertSourceText(String.format("[[%s>>doc:%s]]", label, linkReference));
        switchToWysiwyg();

        // Edit the created link.
        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab(RECENT_PAGES_TAB);
        waitForStepToLoad("xPagesSelector");
        waitForElement("//div[contains(@class, 'xListItem-selected')]//div[. = '"
            + String.format(PAGE_LOCATION, spaceName, pageName) + "']");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        // Change the link to open in new window.
        getSelenium().check("//div[contains(@class, 'xLinkConfig')]//span[contains(@class, 'gwt-CheckBox')]/input");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        assertSourceText(String.format("[[%s>>doc:%s||rel=\"__blank\"]]", label, linkReference));
    }

    /**
     * Creates a link to a new page that has special characters in its name.
     */
    @Test
    public void testCreateLinkToNewPageWithSpecialCharactersInName()
    {
        String pageName = getTestMethodName() + ":a.b@c?d=e&f=g#h";
        String linkReference = pageName.replace(".", "\\.");
        String label = "x";

        typeText(label);
        selectNode("document.body.firstChild");

        openLinkDialog(MENU_WIKI_PAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        waitForStepToLoad("xPagesSelector");
        waitForElement("//div[contains(@class, 'xListItem-selected')]/div[contains(@class, 'xNewPagePreview')]");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", pageName);
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText(String.format("[[%s>>doc:%s]]", label, linkReference));
    }

    /**
     * Creates a link to the currently edited page by going through the "All Pages" step.
     * 
     * @see XWIKI-5849: Cannot create link to current page
     */
    @Test
    public void testCreateLinkToCurrentPageThroughAllPages()
    {
        // We have to save the page to make it appear in the all pages tree.
        clickEditSaveAndContinue();

        String linkLabel = "x";
        typeText(linkLabel);
        selectNodeContents("document.body.firstChild");

        openLinkDialog(MENU_WIKI_PAGE);
        clickTab(ALL_PAGES_TAB);
        waitForStepToLoad(STEP_EXPLORER);

        String currentSpaceName = this.getClass().getSimpleName();
        String currentPageName = getTestMethodName();

        // The current page should be selected by default.
        explorer.waitForPageSelected(currentSpaceName, currentPageName);
        clickButtonWithText(BUTTON_SELECT);

        // Wait for the link configuration step to load.
        waitForStepToLoad("xLinkConfig");
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[" + linkLabel + ">>doc:" + currentPageName + "]]");
    }

    /**
     * @see XWIKI-4473: Cannot edit a link to an existing page to change its target to a new page using the All pages
     *      tab
     */
    @Test
    public void testChangeLinkTargetToNewPage()
    {
        switchToSource();
        setSourceText("[[Home>>Main.WebHome]]");
        switchToWysiwyg();

        // Edit the link and change its target to a new page.
        selectAllContent();

        openLinkDialog(MENU_LINK_EDIT);
        waitForStepToLoad("xSelectorAggregatorStep");
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForPageSelected("Main", "WebHome");

        explorer.openPage("Blog", "WebHome").selectNewPage("Blog");
        clickButtonWithText(BUTTON_SELECT);

        waitForStepToLoad("xLinkToNewPage");
        getSelenium().type("//div[contains(@class, 'xLinkToNewPage')]//input", getTestMethodName());
        clickButtonWithText(BUTTON_CREATE_LINK);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[Home>>Blog." + getTestMethodName() + "]]");
    }

    /**
     * XWIKI-6657: Attachment selection limited set to Yes still shows "All Pages" tab when creating a link in WYSIWYG
     */
    @Test
    public void testAttachmentSelectionLimitedToCurrentPage()
    {
        String allPagesTabLocator =
            "//*[contains(@class, 'xStepsTabs')]//*[@class = 'gwt-TabBarItem' and . = '" + ALL_PAGES_TAB + "']";
        String location = getSelenium().getLocation();

        // By default, attachment selection shoudn't be limited to the current page.
        openLinkDialog(MENU_ATTACHMENT);
        waitForStepToLoad("xSelectorAggregatorStep");
        assertElementPresent(allPagesTabLocator);

        // Change the configuration.
        open("XWiki", "WysiwygEditorConfig", "edit", "editor=object");
        expandObject("XWiki.WysiwygEditorConfigClass", 0);
        checkField("XWiki.WysiwygEditorConfigClass_0_attachmentSelectionLimited");
        clickEditSaveAndContinue();

        try {
            open(location);
            waitForEditorToLoad();

            // The "All Pages" tab should be hidden now.
            openLinkDialog(MENU_ATTACHMENT);
            waitForStepToLoad("xSelectorAggregatorStep");
            assertElementNotPresent(allPagesTabLocator);
        } finally {
            // Restore the configuration.
            open("XWiki", "WysiwygEditorConfig", "edit", "editor=object");
            expandObject("XWiki.WysiwygEditorConfigClass", 0);
            checkField("XWiki.WysiwygEditorConfigClass_0_attachmentSelectionLimited_false");
            clickEditSaveAndContinue();
        }
    }

    /**
     * @see XWIKI-7593: Links from other WYSIWYG fields are removed if the page is saved while a WYSIWYG field is edited
     *      in full screen
     */
    @Test
    public void testEmptyLinkFilterWhenEditingFullScreen() throws UnsupportedEncodingException
    {
        // Add a link to the summary field.
        open("Blog", "BlogIntroduction", "save",
            "Blog.BlogPostClass_0_extract=" + URLEncoder.encode("[[XWiki>>http://www.xwiki.org]]", "UTF-8"));

        // Edit in "Inline form" edit mode.
        open("Blog", "BlogIntroduction", "edit", "editor=inline");
        // Edit the blog post content field in full screen mode and save.
        // Note: The following works because the content field is the first WYSIWYG field.
        waitForEditorToLoad();
        clickEditInFullScreen();
        clickEditSaveAndView();

        // Check if the link is still present.
        open("/xwiki/rest/wikis/xwiki/spaces/Blog/pages/BlogIntroduction/objects/Blog.BlogPostClass/0/properties/extract");
        assertTrue(getDriver().getPageSource().contains("<value>[[XWiki&gt;&gt;http://www.xwiki.org]]</value>"));
    }

    protected void waitForStepToLoad(String name)
    {
        getDriver().waitUntilElementIsVisible(By.className(name));
    }

    private void clickTab(String tabName)
    {
        String tabSelector = "//div[.='" + tabName + "']";
        getSelenium().click(tabSelector);
    }

    private void openLinkDialog(String menuName)
    {
        clickMenu(MENU_LINK);
        assertTrue(isMenuEnabled(menuName));
        clickMenu(menuName);
        waitForDialogToLoad();
    }

    /**
     * In addition to {@link #assertFieldErrorIsPresent(String, String)}, this function does the checks in the specified
     * step.
     * 
     * @param errorMessage the expected error message
     * @param fieldXPathLocator the locator for the field in error
     * @param step the step in which the error should appear
     * @see {@link #assertFieldErrorIsPresent(String, String)}
     */
    public void assertFieldErrorIsPresentInStep(String errorMessage, String fieldXPathLocator, String step)
    {
        waitForStepToLoad(step);
        assertFieldErrorIsPresent(errorMessage, fieldXPathLocator);
    }

    /**
     * In addition to {@link #assertFieldErrorIsNotPresent()}, this function does the checks in the specified step.
     * 
     * @param step the step to check for errors
     */
    public void assertFieldErrorIsNotPresentInStep(String step)
    {
        waitForStepToLoad(step);
        assertFieldErrorIsNotPresent();
    }

    /**
     * Waits for the step aggregator to load and asserts the specified step is selected.
     * 
     * @param selectedStepTitle the title of step that is expected to be selected
     */
    private void waitForStepAggregatorAndAssertSelectedStep(String selectedStepTitle)
    {
        getDriver().waitUntilElementIsVisible(
            By.xpath("//*[@class = 'xSelectorAggregatorStep']/"
                + "*[contains(@class, 'xStepsTabs') and not(contains(@class, 'loading'))]"));
        assertTrue(getDriver().hasElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'xStepsTabs')]"
                + "//*[contains(@class, 'gwt-TabBarItem-selected') and . = '" + selectedStepTitle + "']")));
    }
}
