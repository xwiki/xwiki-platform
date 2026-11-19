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
package org.xwiki.user.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Common page object for all Profile page tabs (profile tab, preferences tab, watchlist tab).
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AbstractUserProfilePage extends ViewPage
{
    private static final String MENU_GROUP_SELECTOR = "#user-profile-menu > .panel";

    private static final String MENU_GROUP_HEADING_SELECTOR = ".panel-heading";

    private static final String MENU_ITEM_SELECTOR = ".list-group-item[data-id]";

    private static final String TEXT_CONTENT = "textContent";

    private static final String ARIA_EXPANDED = "aria-expanded";

    private final String username;

    public AbstractUserProfilePage(String username)
    {
        this.username = username;
    }

    public String getUsername()
    {
        return this.username;
    }

    /**
     * Retrieve the list of menu categories available. Note that if a category is not displayed in the menu, then it
     * cannot be displayed at all.
     * @return the list of menu items available, including the ones of the collapsed menu groups
     * @since 13.2RC1
     */
    public List<String> getAvailableCategories()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector("#user-profile-menu " + MENU_ITEM_SELECTOR))
            .stream().map(AbstractUserProfilePage::getTextContent).collect(Collectors.toList());
    }

    /**
     * @return the names of the groups displayed in the menu, in display order
     * @since 19.0.0RC1
     */
    public List<String> getMenuGroups()
    {
        return getMenuGroupElements().stream()
            .map(group -> getTextContent(group.findElement(By.cssSelector(MENU_GROUP_HEADING_SELECTOR))))
            .collect(Collectors.toList());
    }

    /**
     * @param groupName the name of a menu group
     * @return the names of the categories of the given menu group, in display order, even when the group is collapsed
     * @since 19.0.0RC1
     */
    public List<String> getCategoriesInGroup(String groupName)
    {
        return getMenuGroupElement(groupName).findElements(By.cssSelector(MENU_ITEM_SELECTOR)).stream()
            .map(AbstractUserProfilePage::getTextContent).collect(Collectors.toList());
    }

    /**
     * @return the names of the menu groups that are expanded
     * @since 19.0.0RC1
     */
    public List<String> getExpandedMenuGroups()
    {
        return getMenuGroupElements().stream()
            .map(group -> group.findElement(By.cssSelector(MENU_GROUP_HEADING_SELECTOR)))
            .filter(heading -> "true".equals(heading.getDomAttribute(ARIA_EXPANDED)))
            .map(AbstractUserProfilePage::getTextContent).collect(Collectors.toList());
    }

    /**
     * Open a category of the user profile, expanding its menu group first when it's collapsed.
     *
     * @param categoryId the identifier of the category, e.g. {@code preferences}
     * @since 19.0.0RC1
     */
    public void openCategory(String categoryId)
    {
        WebElement item = getDriver().findElement(By.id("vertical-menu-" + categoryId));
        WebElement heading = item.findElement(By.xpath("ancestor::div[contains(@class, 'panel')][1]"))
            .findElement(By.cssSelector(MENU_GROUP_HEADING_SELECTOR));
        if (!"true".equals(heading.getDomAttribute(ARIA_EXPANDED))) {
            heading.click();
            getDriver().waitUntilCondition(driver -> item.isDisplayed());
        }
        item.click();
        WebElement pane = getDriver().findElement(By.id(categoryId + "Pane"));
        getDriver().waitUntilCondition(driver -> pane.isDisplayed());
    }

    /**
     * @return the identifier of the category currently displayed
     * @since 19.0.0RC1
     */
    public String getCurrentCategory()
    {
        return getDriver().findElement(By.cssSelector("#user-profile-menu " + MENU_ITEM_SELECTOR + ".active"))
            .getDomAttribute("data-id");
    }

    /**
     * Go back in the browser history to the previously opened category and wait for it to be displayed.
     *
     * @since 19.0.0RC1
     */
    public void goBackToPreviousCategory()
    {
        String currentCategory = getCurrentCategory();
        getDriver().navigate().back();
        getDriver().waitUntilCondition(driver -> !currentCategory.equals(getCurrentCategory())
            && driver.findElement(By.id(getCurrentCategory() + "Pane")).isDisplayed());
    }

    public ProfileUserProfilePage switchToProfile()
    {
        openCategory("profile");
        return new ProfileUserProfilePage(getUsername());
    }

    public PreferencesUserProfilePage switchToPreferences()
    {
        openCategory("preferences");
        return new PreferencesUserProfilePage(getUsername());
    }

    private List<WebElement> getMenuGroupElements()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(MENU_GROUP_SELECTOR));
    }

    private WebElement getMenuGroupElement(String groupName)
    {
        return getMenuGroupElements().stream()
            .filter(group -> groupName.equals(getTextContent(group.findElement(By.cssSelector(
                MENU_GROUP_HEADING_SELECTOR)))))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("No menu group named " + groupName));
    }

    private static String getTextContent(WebElement element)
    {
        // Use the text content because the items of the collapsed groups are hidden, so their visible text is empty.
        return element.getDomProperty(TEXT_CONTENT).trim();
    }
}
