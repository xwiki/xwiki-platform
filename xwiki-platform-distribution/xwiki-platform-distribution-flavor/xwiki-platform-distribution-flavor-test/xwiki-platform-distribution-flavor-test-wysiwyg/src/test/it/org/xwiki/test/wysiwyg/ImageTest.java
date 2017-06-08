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

import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;
import org.xwiki.test.wysiwyg.framework.XWikiExplorer;

import static org.junit.Assert.*;

/**
 * Tests the image insert and edit plugin. For the moment, it does not test the upload new image feature, since it needs
 * special selenium setup.
 *
 * @version $Id$
 */
public class ImageTest extends AbstractWysiwygTestCase
{
    public static final String MENU_IMAGE = "Image";

    public static final String MENU_INSERT_ATTACHED_IMAGE = "Attached Image...";

    public static final String MENU_INSERT_EXTERNAL_IMAGE = "External Image...";

    public static final String MENU_EDIT_IMAGE = "Edit Image...";

    public static final String MENU_REMOVE_IMAGE = "Remove Image";

    public static final String STEP_SELECTOR = "xSelectorAggregatorStep";

    public static final String STEP_EXPLORER = "xImagesExplorer";

    public static final String STEP_EXTERNAL_IMAGE = "xExternalImage";

    public static final String STEP_CONFIG = "xImageConfig";

    public static final String STEP_CURRENT_PAGE_SELECTOR = "xImagesSelector";

    public static final String STEP_UPLOAD = "xUploadPanel";

    public static final String TAB_CURRENT_PAGE = "Current page";

    public static final String TAB_ALL_PAGES = "All pages";

    public static final String BUTTON_SELECT = "Select";

    public static final String BUTTON_INSERT_IMAGE = "Insert Image";

    public static final String BUTTON_CHANGE_IMAGE = "Change Image";

    public static final String BUTTON_IMAGE_SETTINGS = "Image Settings";

    public static final String BUTTON_PREVIOUS = "Previous";

    public static final String INPUT_WIDTH = "//div[contains(@class, \"xSizePanel\")]//input[1]";

    public static final String INPUT_HEIGHT = "//div[contains(@class, \"xSizePanel\")]//input[2]";

    public static final String INPUT_ALT = "//div[contains(@class, \"xAltPanel\")]//input";

    public static final String IMAGES_LIST = "//div[contains(@class, 'xListBox')]";

    public static final String FILE_UPLOAD_INPUT = "//input[contains(@class, 'gwt-FileUpload')]";

    public static final String INPUT_EXTERNAL_IMAGE_LOCATION = "//input[@title = 'Image location']";

    public static final String ERROR_MSG_CLASS = "xErrorMsg";

    public static final String SPACE_SELECTOR = "//div[@class=\"xPageChooser\"]//select[1]";

    public static final String PAGE_SELECTOR = "//div[@class=\"xPageChooser\"]//select[2]";

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
     * Test adding an image from a page different from the current one.
     */
    @Test
    public void testInsertImageFromAllPages()
    {
        String imageSpace = "XWiki";
        String imagePage = "AdminSheet";
        String imageFile = "registration.png";
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        // switch to all pages view
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        selectImage(imageSpace, imagePage, imageFile);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[image:" + imageSpace + "." + imagePage + "@" + imageFile + "]]");
    }

    /**
     * Test add and edit an image from a page different from the current one.
     */
    @Test
    public void testInsertAndEditImageFromAllPages()
    {
        String imageSpace = "Blog";
        String imagePage = "Categories";
        String imageFile = "icon.png";

        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        // switch to all pages view
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        selectImage(imageSpace, imagePage, imageFile);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        moveCaret("document.body", 1);
        typeText("x");

        // cannot select the image otherwise but like this: click won't work, nor push button
        selectNode("document.body.firstChild");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_EXPLORER);
        // test that the page is the right page
        assertImageSelected(imageSpace, imagePage, imageFile);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        // the image alt text should be now present
        assertEquals(imageFile, getSelenium().getValue(INPUT_ALT));
        clickButtonWithText(BUTTON_INSERT_IMAGE);

        switchToSource();
        assertSourceText("[[image:" + imageSpace + "." + imagePage + "@" + imageFile + "]]x");
    }

    /**
     * Test adding and editing an image with parameters preserves parameters values.
     */
    @Test
    public void testInsertAndEditImageWithParameters()
    {
        String imageSpace = "XWiki";
        String imagePage = "AdminSheet";
        String imageFile = "rights.png";

        applyStyleTitle1();
        typeText("H");
        typeEnter();

        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        // switch to all pages view
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        selectImage(imageSpace, imagePage, imageFile);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);

        getSelenium().type(INPUT_WIDTH, "200");
        getSelenium().type(INPUT_ALT, "No parking sign");
        selectAlignment("CENTER");
        clickButtonWithText(BUTTON_INSERT_IMAGE);

        // Place the caret after the inserted image.
        getRichTextArea().executeScript("window.getSelection().collapseToEnd()");

        typeText("x");

        switchToSource();
        assertSourceText("= H =\n\n[[image:XWiki.AdminSheet@rights.png||alt=\"No parking sign\" "
            + "style=\"display: block; margin-left: auto; margin-right: auto;\" width=\"200\"]]" + "x");
        switchToWysiwyg();

        // now edit
        selectNode("document.body.getElementsByTagName('img')[0]");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_EXPLORER);
        // test that the page is the right page
        assertImageSelected(imageSpace, imagePage, imageFile);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        assertEquals("200", getSelenium().getValue(INPUT_WIDTH));
        assertEquals("No parking sign", getSelenium().getValue(INPUT_ALT));
        assertTrue(isAlignmentSelected("CENTER"));
        // To reset the image size we have to leave both width and height empty.
        getSelenium().type(INPUT_WIDTH, "");
        getSelenium().type(INPUT_HEIGHT, "");
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("= H =\n\n[[image:XWiki.AdminSheet@rights.png||alt=\"No parking sign\" "
            + "style=\"display: block; margin-left: auto; margin-right: auto;\"]]x");
    }

    /**
     * Test that the insert image dialog defaults to the current page, and the page selection is preserved across
     * multiple inserts.
     */
    @Test
    public void testDefaultSelection()
    {
        String imageSpace = "XWiki";
        String imagePage = "AdminSheet";
        String imageFile1 = "import.png";
        String imageFile2 = "lf.png";

        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepSelector();
        // test that the default loaded view is the current page view
        assertElementPresent("//div[contains(@class, \"" + STEP_CURRENT_PAGE_SELECTOR + "\")]");
        // now switch view
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        selectImage(imageSpace, imagePage, imageFile1);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        moveCaret("document.body", 1);

        typeText("x");

        // now second image
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        // test that the default loaded view is the current page view
        assertElementPresent("//div[contains(@class, \"" + STEP_CURRENT_PAGE_SELECTOR + "\")]");
        // now switch view
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        // test that the selectors are positioned to the old page
        assertEquals(imageSpace, getSelenium().getSelectedValue(SPACE_SELECTOR));
        assertEquals(imagePage, getSelenium().getSelectedValue(PAGE_SELECTOR));
        // and select the new one
        selectImage(imageFile2);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[image:XWiki.AdminSheet@import.png]]x[[image:XWiki.AdminSheet@lf.png]]");
    }

    /**
     * Test that hitting the previous button in the configuration dialog preserves the image selector selection.
     */
    @Test
    public void testPreviousPreservesSelection()
    {
        String imageSpace = "XWiki";
        String imagePage = "AdminSheet";
        String imageFile1 = "export.png";
        String imageFile2 = "import.png";

        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        selectImage(imageSpace, imagePage, imageFile1);

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_CHANGE_IMAGE);

        waitForStepToLoad(STEP_EXPLORER);
        // wait for the inner selector to load
        getDriver().waitUntilElementIsVisible(
            By.xpath("//*[contains(@class, \"" + STEP_EXPLORER + "\")]//*[contains(@class, \""
                + STEP_CURRENT_PAGE_SELECTOR + "\")]"));
        assertImageSelected(imageSpace, imagePage, imageFile1);

        selectImage(imageSpace, imagePage, imageFile2);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);

        // test that the correct image has been inserted
        switchToSource();
        assertSourceText("[[image:" + imageSpace + "." + imagePage + "@" + imageFile2 + "]]");
    }

    /**
     * Tests that an image can be removed from the menu, as well as using the delete key.
     */
    @Test
    public void testRemoveImage()
    {
        switchToSource();
        setSourceText("[[image:xwiki:XWiki.AdminSheet@import.png]]Mmmh, cheese!"
            + "[[image:xwiki:XWiki.AdminSheet@export.png]]");
        switchToWysiwyg();
        selectNode("document.body.firstChild.childNodes[2]");
        clickMenu(MENU_IMAGE);
        assertTrue(isMenuEnabled(MENU_REMOVE_IMAGE));
        clickMenu(MENU_REMOVE_IMAGE);

        switchToSource();
        assertSourceText("[[image:xwiki:XWiki.AdminSheet@import.png]]Mmmh, cheese!");
        switchToWysiwyg();

        selectNode("document.body.firstChild.firstChild");
        typeDelete();

        switchToSource();
        assertSourceText("Mmmh, cheese!");
    }

    /**
     * Test that selecting the "Upload new image" option leads to the upload file dialog.
     */
    @Test
    public void testNewImageOptionLoadsFileUploadStep()
    {
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);

        // wait for the default option to load and then click it
        getDriver().waitUntilElementIsVisible(By.className("xNewImagePreview"));
        getSelenium().click("//div[contains(@class, \"xNewImagePreview\")]");
        clickButtonWithText(BUTTON_SELECT);

        waitForStepToLoad(STEP_UPLOAD);
        closeDialog();

        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);

        // wait for the default option to show up and then click it
        String newImageLocator = "//div[@class = 'xImagesExplorer']//div[@class = 'xNewImagePreview']";
        waitForElement(newImageLocator);
        getSelenium().click(newImageLocator);
        clickButtonWithText(BUTTON_SELECT);

        waitForStepToLoad(STEP_UPLOAD);
        closeDialog();
    }

    /**
     * Test that editing an image and not changing its location preserves a full reference and does not change it to a
     * relative one.
     */
    @Test
    public void testEditImagePreservesFullReferences()
    {
        switchToSource();
        setSourceText("[[image:xwiki:XWiki.AdminSheet@import.png]] [[image:XWiki.AdminSheet@export.png]]");
        switchToWysiwyg();

        // edit first image
        selectNode("document.body.firstChild.firstChild");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        waitForStepToLoad(STEP_EXPLORER);
        assertImageSelected("XWiki", "AdminSheet", "import.png");

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);

        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        // edit second image too
        selectNode("document.body.firstChild.childNodes[2]");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        waitForStepToLoad(STEP_EXPLORER);
        assertImageSelected("XWiki", "AdminSheet", "export.png");

        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);

        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[image:xwiki:XWiki.AdminSheet@import.png]] [[image:XWiki.AdminSheet@export.png]]");
    }

    /**
     * Test that, upon editing an image which is the label of a link, the link is preserved.
     * <p>
     * See https://jira.xwiki.org/browse/XWIKI-3784.
     */
    @Test
    public void testEditImageWithLink()
    {
        // add all the image & link, otherwise it will not reproduce, it only reproduces if container is body
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        // switch to all pages view
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        selectImage("XWiki", "AdminSheet", "registration.png");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);

        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        selectNode("document.body.firstChild");

        // add link around the image
        clickMenu(LinkTest.MENU_LINK);
        clickMenu(LinkTest.MENU_WIKI_PAGE);
        waitForDialogToLoad();
        waitForStepToLoad(STEP_SELECTOR);
        // get the all pages tree
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(LinkTest.STEP_EXPLORER);
        explorer.waitForIt().findAndSelectPage("First blog post");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad("xLinkConfig");
        assertEquals("registration.png", getInputValue(LinkTest.LABEL_INPUT_TITLE));
        // check that the label is readonly
        assertFalse(getSelenium().isEditable("//input[@title=\"" + LinkTest.LABEL_INPUT_TITLE + "\"]"));

        clickButtonWithText("Create Link");
        waitForDialogToClose();

        // edit image
        selectNode("document.body.firstChild.firstChild");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        waitForStepToLoad(STEP_EXPLORER);
        assertImageSelected("XWiki", "AdminSheet", "registration.png");
        selectImage("Sandbox", "WebHome", "XWikiLogo.png");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        // clear the alt text
        getSelenium().type(INPUT_ALT, "");
        // We need to reset the image size because we changed the image source and the previous size will be compared
        // either with the old image if the new image hasn't been loaded yet or with the new image if it was previously
        // loaded. In the first case the size didn't change so it doesn't appear in the wiki syntax. In the second case
        // the new image has a different size so the old size (if not changed) must be explicitly set in the wiki
        // syntax.
        getSelenium().type(INPUT_WIDTH, "");
        getSelenium().type(INPUT_HEIGHT, "");
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        switchToSource();
        assertSourceText("[[[[image:Sandbox.WebHome@XWikiLogo.png]]>>doc:Blog.BlogIntroduction]]");
    }

    /**
     * Tests that the option to upload a new image is selected by default when inserting a new image and when the edited
     * image is not attached to the current page.
     */
    @Test
    public void testNewImageOptionIsSelectedByDefault()
    {
        // Insert a new image.
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        // Look on the current page selector.
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        assertElementPresent("//div[@class = 'xImagesSelector']//" + "div[contains(@class, 'xListItem-selected')]"
            + "//div[contains(@class, 'xNewImagePreview')]");
        // Look on the all pages selector.
        clickTab(TAB_ALL_PAGES);
        waitForStepToLoad(STEP_EXPLORER);
        assertElementPresent("//div[@class = 'xImagesExplorer']//" + "div[contains(@class, 'xListItem-selected')]"
            + "//div[contains(@class, 'xNewImagePreview')]");
        closeDialog();

        // Edit an image.
        switchToSource();
        setSourceText("[[image:xwiki:Sandbox.WebHome@XWikiLogo.png]]");
        switchToWysiwyg();
        selectNode("document.body.firstChild.firstChild");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        // Look on the current page selector.
        clickTab(TAB_CURRENT_PAGE);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        assertElementPresent("//div[@class = 'xImagesSelector']//" + "div[contains(@class, 'xListItem-selected')]"
            + "//div[contains(@class, 'xNewImagePreview')]");
        closeDialog();
    }

    /**
     * Test that the validation errors in the image insert steps are hidden on the next display of the steps.
     */
    @Test
    public void testErrorIsHiddenOnNextDisplay()
    {
        // Get an error in the file upload step and check that on previous, next is not displayed anymore.
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        assertElementPresent("//*[contains(@class, 'xListItem-selected')]//*[contains(@class, 'xNewImagePreview')]");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_UPLOAD);
        clickButtonWithText(BUTTON_IMAGE_SETTINGS);
        waitForStepToLoad(STEP_UPLOAD);
        assertFieldErrorIsPresent("The file path was not set", FILE_UPLOAD_INPUT);
        clickButtonWithText(BUTTON_PREVIOUS);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_UPLOAD);
        assertFieldErrorIsNotPresent();
        // Get the error again, close, open and test that error is no longer there.
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForStepToLoad(STEP_UPLOAD);
        assertFieldErrorIsPresent("The file path was not set", FILE_UPLOAD_INPUT);
        closeDialog();
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        assertElementPresent("//*[contains(@class, 'xListItem-selected')]//*[contains(@class, 'xNewImagePreview')]");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_UPLOAD);
        assertFieldErrorIsNotPresent();
        closeDialog();
    }

    /**
     * Tests fast navigation in the images list: double click and enter advance to the next step.
     */
    @Test
    public void testFastNavigationToSelectImage()
    {
        // double click to select the new image option
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        // click first to make sure selection is set
        getSelenium().click("//*[contains(@class, 'xListItem')]//*[contains(@class, 'xNewImagePreview')]");
        getSelenium().doubleClick("//*[contains(@class, 'xListItem')]//*[contains(@class, 'xNewImagePreview')]");
        waitForStepToLoad(STEP_UPLOAD);
        closeDialog();

        // enter to select the new image option
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_CURRENT_PAGE_SELECTOR);
        getSelenium().click("//div[contains(@class, \"xNewImagePreview\")]");
        getSelenium().typeKeys(IMAGES_LIST, "\\13");
        waitForStepToLoad(STEP_UPLOAD);
        closeDialog();

        // double click to add an image from another page
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        clickTab(TAB_ALL_PAGES);
        selectLocation("XWiki", "AdminSheet");
        getSelenium().click(getImageLocator("registration.png"));
        getSelenium().doubleClick(getImageLocator("registration.png"));
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);

        switchToSource();
        setSourceText("[[image:XWiki.AdminSheet@registration.png]]");
        switchToWysiwyg();

        // Reset the image selection.
        moveCaret("document.body", 0);

        // enter to test enter upload in all pages
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        clickTab(TAB_ALL_PAGES);
        selectLocation("XWiki", "AdminSheet");
        getSelenium().click("//div[@class = '" + STEP_EXPLORER + "']//div[contains(@class, \"xNewImagePreview\")]");
        getSelenium().typeKeys("//div[@class = '" + STEP_EXPLORER + "']" + IMAGES_LIST, "\\13");
        waitForStepToLoad(STEP_UPLOAD);
        closeDialog();
    }

    /**
     * @see XWIKI-3741: Editing an image removes existing unknown custom parameters.
     */
    @Test
    public void testUneditableAttributesArePreserved()
    {
        // Insert an image with attribute that cannot be edited through the Edit Image wizard.
        switchToSource();
        setSourceText("[[image:XWiki.AdminSheet@export.png||id=\"foobar\" "
            + "title=\"abc\" foo=\"bar\" style=\"margin-top:12px\"]]");
        switchToWysiwyg();

        // Select the image and edit it.
        selectNode("document.body.firstChild.firstChild");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_EXPLORER);
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        getSelenium().type(INPUT_WIDTH, "75px");
        getSelenium().type(INPUT_HEIGHT, "7.5em");
        selectAlignment("RIGHT");
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        assertSourceText("[[image:XWiki.AdminSheet@export.png||foo=\"bar\" id=\"foobar\" "
            + "style=\"margin-top: 12px; height: 7.5em; float: right;\" title=\"abc\" width=\"75px\"]]");
    }

    /**
     * Tests that images attached to pages with special characters in their names are properly inserted.
     */
    @Test
    public void testInsertImageFromPageWithSpecialCharactersInName()
    {
        // Create a page with special characters in its name by copying a page with image attachments.
        String spaceName = this.getClass().getSimpleName() + ":x.y@z";
        String escapedSpaceName = spaceName.replaceAll("([\\:\\.])", "\\\\$1");
        String pageName = getTestMethodName() + ":a.b@c";
        String escapedPageName = pageName.replace(".", "\\.");
        String pageFullName = String.format("%s.%s", escapedSpaceName, escapedPageName);
        deletePage(spaceName, pageName);
        assertTrue(copyPage("XWiki", "AdminSheet", escapedSpaceName, pageName));

        // Come back to the edited page.
        open(this.getClass().getSimpleName(), getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();

        // Insert an image from the created page.
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        clickTab(TAB_ALL_PAGES);
        selectLocation(spaceName, pageName);
        getSelenium().click(getImageLocator("export.png"));
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        assertSourceText(String.format("[[image:%s@export.png]]", pageFullName));
        switchToWysiwyg();

        // Edit the inserted image.
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        waitForStepToLoad(STEP_EXPLORER);
        assertImageSelected(spaceName, pageName, "export.png");
        selectImage(spaceName, pageName, "import.png");
        clickButtonWithText(BUTTON_SELECT);
        waitForStepToLoad(STEP_CONFIG);
        getSelenium().type(INPUT_ALT, "");
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        assertSourceText(String.format("[[image:%s@import.png]]", pageFullName));
    }

    /**
     * Tests if the edited image is selected when the image selector wizard step is opened.
     */
    @Test
    public void testEditedImageIsSelected()
    {
        // Insert two different images.
        switchToSource();
        setSourceText("image:XWiki.AdminSheet@users.png\n\nimage:XWiki.AdminSheet@export.png");
        switchToWysiwyg();

        // Edit the first image and check if it is selected in the image selector wizard step.
        selectNode("document.body.getElementsByTagName('img')[0]");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        waitForStepToLoad(STEP_EXPLORER);
        assertImageSelected("XWiki", "AdminSheet", "users.png");
        closeDialog();

        // Edit the second image and check if it is selected in the image selector wizard step.
        selectNode("document.body.getElementsByTagName('img')[1]");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_SELECTOR);
        waitForStepToLoad(STEP_EXPLORER);
        assertImageSelected("XWiki", "AdminSheet", "export.png");

        // Select a different image and refresh the image list to see if the edited image is reselected.
        selectImage("presentation.png");
        getSelenium().click("//div[@class=\"xPageChooser\"]//button[text()=\"Update\"]");
        getDriver().waitUntilElementIsVisible(
            By.xpath("//*[contains(@class, \"" + STEP_EXPLORER + "\")]//*[contains(@class, \""
                + STEP_CURRENT_PAGE_SELECTOR + "\")]"));
        waitForElement(getImageLocator("export.png"));
    }

    /**
     * Tests if an external image can be inserted.
     */
    @Test
    public void testInsertExternalImage()
    {
        openImageDialog(MENU_INSERT_EXTERNAL_IMAGE);
        waitForStepToLoad(STEP_EXTERNAL_IMAGE);

        // Try to move to the next step without setting the image location.
        clickButtonWithText(BUTTON_IMAGE_SETTINGS);
        waitForStepToLoad(STEP_EXTERNAL_IMAGE);
        assertFieldErrorIsPresent("Please specify the image location.", INPUT_EXTERNAL_IMAGE_LOCATION);

        // Set the image URL and insert the image.
        String imageURL = "http://www.xwiki.org/xwiki/skins/toucan/logo.png";
        getSelenium().type(INPUT_EXTERNAL_IMAGE_LOCATION, imageURL);
        clickButtonWithText(BUTTON_IMAGE_SETTINGS);
        waitForStepToLoad(STEP_CONFIG);

        // The alternative text should be set by default to the image URL.
        assertEquals(imageURL, getSelenium().getValue(INPUT_ALT));
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        assertSourceText(String.format("[[image:%s]]", imageURL));
    }

    /**
     * Tests if an external image can be selected.
     */
    @Test
    public void testEditExternalImage()
    {
        // Insert an external image.
        switchToSource();
        String imageURL = "http://www.xwiki.org/xwiki/skins/toucan/logo.png";
        String alternativeText = "xyz";
        setSourceText(String.format("[[image:%s||alt=\"%s\" title=\"abc\"]]", imageURL, alternativeText));
        switchToWysiwyg();

        // Edit the external image and change its location.
        selectNode("document.body.getElementsByTagName('img')[0]");
        openImageDialog(MENU_EDIT_IMAGE);
        waitForStepToLoad(STEP_EXTERNAL_IMAGE);
        assertEquals(imageURL, getSelenium().getValue(INPUT_EXTERNAL_IMAGE_LOCATION));

        // Change the image location.
        imageURL = "http://www.xwiki.org/xwiki/skins/colibri/logo.png";
        getSelenium().type(INPUT_EXTERNAL_IMAGE_LOCATION, imageURL);
        // Quickly insert the image, skipping the settings step.
        clickButtonWithText(BUTTON_INSERT_IMAGE);
        waitForDialogToClose();

        // Check the result.
        switchToSource();
        // The image parameters must be preserved.
        setSourceText(String.format("[[image:%s||alt=\"%s\" title=\"abc\"]]", imageURL, alternativeText));
    }

    /**
     * Tests that the configuration that limits the image selection to the current page works as expected.
     */
    @Test
    public void testImageSelectionLimitedToCurrentPage()
    {
        String allPagesTabLocator =
            "//*[contains(@class, 'xStepsTabs')]//*[@class = 'gwt-TabBarItem' and . = '" + TAB_ALL_PAGES + "']";
        String location = getSelenium().getLocation();

        // By default, attachment selection shoudn't be limited to the current page.
        openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        assertElementPresent(allPagesTabLocator);

        // Change the configuration.
        open("XWiki", "WysiwygEditorConfig", "edit", "editor=object");
        expandObject("XWiki.WysiwygEditorConfigClass", 0);
        checkField("XWiki.WysiwygEditorConfigClass_0_imageSelectionLimited");
        clickEditSaveAndContinue();

        try {
            open(location);
            waitForEditorToLoad();

            // The "All Pages" tab should be hidden now.
            openImageDialog(MENU_INSERT_ATTACHED_IMAGE);
            waitForStepToLoad("xSelectorAggregatorStep");
            assertElementNotPresent(allPagesTabLocator);
        } finally {
            // Restore the configuration.
            open("XWiki", "WysiwygEditorConfig", "edit", "editor=object");
            expandObject("XWiki.WysiwygEditorConfigClass", 0);
            checkField("XWiki.WysiwygEditorConfigClass_0_imageSelectionLimited_false");
            clickEditSaveAndContinue();
        }
    }

    private void waitForStepToLoad(String stepClass)
    {
        getDriver().waitUntilElementIsVisible(By.className(stepClass));
    }

    private void selectImage(String space, String page, String filename)
    {
        selectLocation(space, page);
        selectImage(filename);
    }

    private void selectLocation(String space, String page)
    {
        // Note: The wiki selector is generated only in multiwiki mode, so for XE the first HTML select from the page
        // chooser panel is in fact the space selector.
        waitForElement(SPACE_SELECTOR + "/option[. = '" + space + "']");
        getSelenium().select(SPACE_SELECTOR, space);

        waitForElement(PAGE_SELECTOR + "/option[. = '" + page + "']");
        getSelenium().select(PAGE_SELECTOR, page);

        // If the list of spaces/pages is long then the space/page we want to select might not be visible when the drop
        // down is opened so Selenium will try to scroll it into view. Unfortunately Selenium doesn't scroll only the
        // drop down list but also the entire page. The select fields end up at the top of the page where, for some
        // unknown reason, the drop down list remains opened after an option is selected. As a consequence clicking on
        // the Update button has only the effect of closing the drop down without triggering the expected update. Make
        // sure the drop down is closed by pressing the Tab key to move the focus to the Update button.
        getSelenium().typeKeys(PAGE_SELECTOR, "\t");

        getSelenium().click("//div[@class=\"xPageChooser\"]//button[text()=\"Update\"]");
        waitForElement("//*[@class = '" + STEP_EXPLORER + "']//*[contains(@class, '" + STEP_CURRENT_PAGE_SELECTOR
            + "')]");
    }

    private void selectImage(String filename)
    {
        waitForElement(getImageLocator(filename));
        getSelenium().click(getImageLocator(filename));
    }

    private String getImageLocator(String filename)
    {
        return "//div[@class=\"xImagesSelector\"]//img[@title=\"" + filename + "\"]";
    }

    private void assertImageSelected(String space, String page, String filename)
    {
        getDriver().waitUntilElementIsVisible(By.xpath(SPACE_SELECTOR + "/option[. = \"" + space + "\"]"));
        assertEquals(space, getSelenium().getSelectedLabel(SPACE_SELECTOR));

        getDriver().waitUntilElementIsVisible(By.xpath(PAGE_SELECTOR + "/option[. = \"" + page + "\"]"));
        assertEquals(page, getSelenium().getSelectedLabel(PAGE_SELECTOR));

        assertImageSelected(filename);
    }

    private void clickTab(String tabName)
    {
        String tabSelector = "//div[.='" + tabName + "']";
        getSelenium().click(tabSelector);
    }

    private void assertImageSelected(String filename)
    {
        String imageItem =
            "//div[@class=\"xImagesSelector\"]//div[contains(@class, \"xListItem-selected\")]//img[@title=\""
                + filename + "\"]";
        assertElementPresent(imageItem);
    }

    private void selectAlignment(String alignment)
    {
        getSelenium().click(
            "//div[contains(@class, \"AlignPanel\")]//input[@name=\"alignment\" and @value=\"" + alignment + "\"]");
    }

    public boolean isAlignmentSelected(String alignment)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[contains(@class, \"AlignPanel\")]//input[@name=\"alignment\" and @value=\"" + alignment
                + "\" and @checked=\"\"]"));
    }

    private void openImageDialog(String menuName)
    {
        clickMenu(MENU_IMAGE);
        assertTrue(isMenuEnabled(menuName));
        clickMenu(menuName);
        waitForDialogToLoad();
    }

    /**
     * Wait for the step selector to load.
     */
    private void waitForStepSelector()
    {
        getDriver().waitUntilElementIsVisible(
            By.xpath("//table[contains(@class, 'xStepsTabs') and not(contains(@class, 'loading'))]"));
    }
}
