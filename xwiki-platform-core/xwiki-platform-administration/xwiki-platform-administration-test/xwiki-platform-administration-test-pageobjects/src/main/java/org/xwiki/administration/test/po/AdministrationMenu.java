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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.and;
import static org.openqa.selenium.support.ui.ExpectedConditions.attributeToBe;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

/**
 * Page object used to interact with the Administration menu.
 * 
 * @version $Id$
 * @since 9.2RC1
 */
public class AdministrationMenu extends BaseElement
{
    @FindBy(className = "admin-menu")
    private WebElement container;

    @FindBy(id = "adminsearchmenu")
    private WebElement searchInput;

    /**
     * Create a new instance and waits for the menu to be ready for user interaction.
     */
    public AdministrationMenu()
    {
        waitUntilReady();
    }

    private By categoryByName(String categoryName)
    {
        return By.xpath(".//a[contains(@class, 'panel-heading') and normalize-space(.) = '" + categoryName + "']");
    }

    private By categoryById(String categoryId)
    {
        return By.cssSelector("a[id='panel-heading-" + categoryId + "']");
    }

    public WebElement getCategoryByName(String categoryName)
    {
        return this.container.findElement(categoryByName(categoryName));
    }

    public WebElement getCategoryById(String categoryId)
    {
        return this.container.findElement(categoryById(categoryId));
    }

    public boolean hasCategoryWithId(String categoryId)
    {
        return getDriver().hasElement(this.container, categoryById(categoryId));
    }

    public boolean hasCategoryWithName(String categoryName)
    {
        return getDriver().hasElement(this.container, categoryByName(categoryName));
    }

    public boolean hasNotCategoryWithId(String categoryId)
    {
        return getDriver().findElementsWithoutWaiting(this.container, categoryById(categoryId)).size() == 0;
    }

    public boolean hasNotCategoryWithName(String categoryName)
    {
        return getDriver().findElementsWithoutWaiting(this.container, categoryByName(categoryName)).size() == 0;
    }

    public AdministrationMenu expandCategoryWithId(String categoryId)
    {
        return expandCategory(getCategoryById(categoryId));
    }

    public AdministrationMenu expandCategoryWithName(String categoryName)
    {
        return expandCategory(getCategoryByName(categoryName));
    }

    private AdministrationMenu expandCategory(WebElement categoryLink)
    {
        if (categoryLink.getAttribute("class").contains("collapsed")) {
            By categoryContent = By.cssSelector(categoryLink.getAttribute("data-target") + ".collapse.in");
            categoryLink.click();
            getDriver().waitUntilElementIsVisible(categoryContent);
        }
        return this;
    }

    private By sectionByName(String categoryName, String sectionName)
    {
        return By.xpath(".//a[contains(@class, 'panel-heading') and normalize-space(.) = '" + categoryName
            + "']/following-sibling::*//a[contains(@class, 'list-group-item') and . = '" + sectionName + "']");
    }

    private By sectionById(String sectionId)
    {
        return By.cssSelector("a.list-group-item[data-id='" + sectionId + "']");
    }

    public WebElement getSectionByName(String categoryName, String sectionName)
    {
        return this.container.findElement(sectionByName(categoryName, sectionName));
    }

    public WebElement getSectionById(String sectionId)
    {
        return this.container.findElement(sectionById(sectionId));
    }

    public boolean hasSectionWithId(String sectionId)
    {
        return getDriver().hasElement(this.container, sectionById(sectionId));
    }

    public boolean hasSectionWithName(String categoryName, String sectionName)
    {
        return getDriver().hasElement(this.container, sectionByName(categoryName, sectionName));
    }

    public boolean hasNotSectionWithId(String sectionId)
    {
        return getDriver().findElementsWithoutWaiting(this.container, sectionById(sectionId)).size() == 0;
    }

    public boolean hasNotSectionWithName(String categoryName, String sectionName)
    {
        return getDriver().findElementsWithoutWaiting(this.container, sectionByName(categoryName, sectionName))
            .size() == 0;
    }

    /**
     * Wait for the menu to be ready for user interaction.
     * 
     * @return this administration menu
     * @since 12.8RC1
     */
    protected AdministrationMenu waitUntilReady()
    {
        getDriver().waitUntilCondition(
            and(attributeToBe(this.container, "data-ready", "true"), elementToBeClickable(this.searchInput)));
        return this;
    }
}
