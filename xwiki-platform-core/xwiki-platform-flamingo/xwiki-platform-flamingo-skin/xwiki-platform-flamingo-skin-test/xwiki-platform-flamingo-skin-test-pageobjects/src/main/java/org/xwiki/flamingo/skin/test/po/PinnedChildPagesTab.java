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
package org.xwiki.flamingo.skin.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.panels.test.po.NavigationTreeElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the tab for configuring pinned child pages in children viewer.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
public class PinnedChildPagesTab extends BaseElement
{
    private final WebElement container;

    /**
     * Default constructor.
     * @param container the content of the tab.
     */
    public PinnedChildPagesTab(WebElement container)
    {
        this.container = container;
    }

    private WebElement getTreeElement()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector(".panel .xtree"));
    }

    /**
     * @return the navigation tree containing the top level pages to manipulate.
     */
    public NavigationTreeElement getNavigationTree()
    {
        return (NavigationTreeElement) new NavigationTreeElement(getTreeElement()).waitForIt();
    }

    private WebElement getPageByTitle(String pageTitle)
    {
        return getDriver().findElementWithoutWaiting(getTreeElement(),
            By.xpath("(.//li[contains(@class, 'jstree-node')]/a[. = '%s'])".formatted(pageTitle)));
    }

    /**
     * Check if the page with the given title is pinned or not.
     * @param pageTitle the title of the page for which to check if it's pinned
     * @return {@code true} if the page is pinned.
     */
    public boolean isPinned(String pageTitle)
    {
        WebElement pageItem = getPageByTitle(pageTitle);
        return "true".equals(pageItem.getDomAttribute("data-pinned"));
    }

    private void togglePinnedPageStatus(String pageTitle)
    {
        WebElement pageItem = getPageByTitle(pageTitle);
        pageItem.click();
        WebElement buttonElement = getDriver().findElementWithoutWaiting(getTreeElement(),
            By.xpath("(.//li[contains(@class, 'jstree-node')]/a[. = '" + pageTitle + "']/following-sibling::div"
                + "[contains(@class, 'jstree-action')]/button)"));
        getDriver().createActions().moveToElement(buttonElement).perform();
        getDriver().waitUntilCondition(driver -> buttonElement.isDisplayed());
        buttonElement.click();
    }

    /**
     * Pin the given page if it's not pinned already.
     * @param pageTitle the title of the page to pin.
     */
    public void pinPage(String pageTitle)
    {
        if (!isPinned(pageTitle)) {
            togglePinnedPageStatus(pageTitle);
        }
    }

    /**
     * Unpin the page if it's pinned.
     * @param pageTitle the title of the page to unpin.
     */
    public void unpinPage(String pageTitle)
    {
        if (isPinned(pageTitle)) {
            togglePinnedPageStatus(pageTitle);
        }
    }

    /**
     * Drag the given page before the second page argument. Note that performing the drag&amp;drop should also perform a
     * pin.
     * @param pageTitleToDrag the title of the page to drag
     * @param pageTitleToDropBefore the title of the page before which to drag
     */
    public void dragBefore(String pageTitleToDrag, String pageTitleToDropBefore)
    {
        WebElement pageItemToDrag = getPageByTitle(pageTitleToDrag);
        WebElement pageItemToDropBefore = getPageByTitle(pageTitleToDropBefore);

        getDriver().dragAndDrop(pageItemToDrag, pageItemToDropBefore);
    }

    /**
     * Save the changes.
     */
    public void save()
    {
        WebElement saveButton =
            getDriver().findElementWithoutWaiting(this.container, By.cssSelector(".admin-buttons input.btn-primary"));
        saveButton.click();
        waitForNotificationSuccessMessage("Saved");
    }
}
