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

import java.util.Arrays;

import org.junit.Test;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;
import org.xwiki.test.wysiwyg.framework.XWikiExplorer;

import static org.junit.Assert.*;

/**
 * Test for the Wysiwyg editing features when editing as a regular user, not an admin.
 */
public class RegularUserTest extends AbstractWysiwygTestCase
{
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
     * {@inheritDoc}
     * <p>
     * Override to login as a regular user (and create the user if necessary).
     */
    @Override
    protected void login()
    {
        loginAndRegisterUser("Pokemon", "Pokemon", false);
    }

    /**
     * Test that creating a link to a page, logged in as a regular user, does not show technical documents in the search
     * results.
     * <p>
     * See https://jira.xwiki.org/browse/XWIKI-4412 and https://jira.xwiki.org/browse/XWIKI-7568.
     */
    @Test
    public void testWikiLinkSearchedPageHidesTechnicalSpaces()
    {
        openDialog("Link", "Wiki Page...");

        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab("Search");
        waitForStepToLoad("xPagesSearch");
        // Check the results list: Blog, Main and Sandbox are present.
        checkSpaceInSearchResults("Blog", true);
        checkSpaceInSearchResults("Main", true);
        checkSpaceInSearchResults("Sandbox", true);
        // Check the results list: ColorThemes, Panels, Scheduler, Stats, XWiki are not present.
        checkSpaceInSearchResults("ColorThemes", false);
        checkSpaceInSearchResults("Panels", false);
        checkSpaceInSearchResults("Scheduler", false);
        checkSpaceInSearchResults("Stats", false);
        checkSpaceInSearchResults("XWiki", false);

        closeDialog();
    }

    /**
     * Helper method to test if a space appears in the search results or not.
     * 
     * @param spaceName the name of the space to test whether it is returned among the search results or not
     * @param present {@code true} if the space is expected in the search results, {@code false} otherwise
     */
    private void checkSpaceInSearchResults(String spaceName, boolean expected)
    {
        typeInInput("Type a keyword to search for a wiki page", spaceName + ".WebHome");
        clickButtonWithText("Search");
        // We have to look for the new page selector inside the search panel because it is also present on the recent
        // pages panel (which is hidden, but still present in DOM, while the search tab is selected).
        String newPageSelector =
            "//div[contains(@class, 'xPagesSearch')]" + "//div[contains(@class, 'xListItem')]"
                + "//div[contains(@class, 'xNewPagePreview')]";
        // Wait for the search results. The list is cleared (including the new page selector) as soon as we click the
        // search button and is refilled when the search results are received. The new page selector is (re)added after
        // the list is filled with the search results.
        waitForElement(newPageSelector);
        // Check if the desired element is there or not, but look precisely inside the search panel.
        String pageInListLocator =
            "//div[contains(@class, 'xPagesSearch')]" + "//div[contains(@class, 'xListItem')]" + "//div[. = '"
                + String.format(LinkTest.PAGE_LOCATION, spaceName, "WebHome") + "']";
        if (expected) {
            assertElementPresent(pageInListLocator);
        } else {
            assertElementNotPresent(pageInListLocator);
        }
    }

    /**
     * Test that upon selecting the wiki page to create a link to from all the pages in the wiki, with the tree
     * explorer, the technical spaces are not displayed to the regular user to choose from.
     */
    @Test
    public void testWikiLinkAllPagesPageHidesTechnicalSpaces()
    {
        String currentSpace = getClass().getSimpleName();
        String currentPage = getTestMethodName();

        // Save the current page so that it appears in the tree.
        clickEditSaveAndContinue();

        openDialog("Link", "Wiki Page...");
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab("All pages");
        waitForStepToLoad("xExplorerPanel");
        explorer.waitForPageSelected(currentSpace, currentPage);

        // Now the tree is loaded. Check the list of top level documents.
        // Note that the home page of the XWiki space is hidden ATM although there are pages inside the XWiki space that
        // are not hidden, like the syntax guide. So the "XWiki" top level document is visible but is not provided
        // in the finder suggestions. We search for the syntax guide instead.
        for (String page : Arrays.asList("Blog", "Home", "Sandbox", "XWiki Syntax Guide")) {
            explorer.findAndSelectPage(page);
        }

        // Technical documents shouldn't be present.
        for (String space : Arrays.asList("ColorThemes", "Panels", "Scheduler", "Stats")) {
            assertFalse(explorer.hasPage(space, "WebHome"));
        }
        for (String page : Arrays.asList("Color Themes", "Panels", "Job Scheduler", "Statistics")) {
            assertFalse(explorer.find(page).hasPage(page));
        }

        closeDialog();
    }

    /**
     * Test that upon selecting an image from all the images in the wiki, the technical spaces are not listed in the
     * space selector for the regular user to choose from.
     */
    @Test
    public void testImageSelectorHidesTechnicalSpaces()
    {
        String currentSpace = getClass().getSimpleName();

        // Save the current page so that it appears in the tree.
        clickEditSaveAndContinue();

        openDialog(ImageTest.MENU_IMAGE, ImageTest.MENU_INSERT_ATTACHED_IMAGE);
        waitForStepToLoad("xSelectorAggregatorStep");
        clickTab("All pages");
        waitForStepToLoad("xImagesExplorer");
        // wait for the current space to load in the selector to be sure the spaces list is loaded
        waitForElement(ImageTest.SPACE_SELECTOR + "/option[@value=\"" + currentSpace + "\"]");
        // check the spaces: Blog, Main, Sandbox are present
        assertElementPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"Blog\"]");
        assertElementPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"Main\"]");
        assertElementPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"Sandbox\"]");
        // check the spaces: ColorThemes, Panels, Scheduler, Stats, XWiki are not present
        assertElementNotPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"ColorThemes\"]");
        assertElementNotPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"Panels\"]");
        assertElementNotPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"Scheduler\"]");
        assertElementNotPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"Stats\"]");
        // TODO: XWiki space should not be listed but currently there are few pages in this space that are not marked as
        // hidden/technical. Update this check as soon as the XWiki space is completely hidden.
        assertElementPresent(ImageTest.SPACE_SELECTOR + "/option[@value=\"XWiki\"]");

        closeDialog();
    }

    protected void waitForStepToLoad(String name)
    {
        waitForElement("//*[contains(@class, '" + name + "')]");
    }

    private void openDialog(String menuName, String menuItemName)
    {
        clickMenu(menuName);
        assertTrue(isMenuEnabled(menuItemName));
        clickMenu(menuItemName);
        waitForDialogToLoad();
    }

    private void clickTab(String tabName)
    {
        String tabSelector = "//div[.='" + tabName + "']";
        getSelenium().click(tabSelector);
    }
}
