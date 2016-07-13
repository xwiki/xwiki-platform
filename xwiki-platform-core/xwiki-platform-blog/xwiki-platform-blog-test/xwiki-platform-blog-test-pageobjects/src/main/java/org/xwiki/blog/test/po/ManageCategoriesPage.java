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
package org.xwiki.blog.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BasePage;

/**
 * Represents Blog category management page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ManageCategoriesPage extends BasePage
{
    @FindBy(xpath = "//li/span[@class='blog-add-category-label']/a")
    private WebElement addCategoryLink;

    @FindBy(xpath = "//form[@class='category-add-form']//input[@type='submit']")
    private WebElement addButton;

    @FindBy(xpath = "//form[@class='category-add-form']//a[text()='Cancel']")
    private WebElement cancelLink;

    @FindBy(xpath = "//input[@class='category-name-input']")
    private WebElement nameInput;

    @FindBy(xpath = "//form[@class='category-rename-form']//input[@type='submit']")
    private WebElement renameButton;

    public static ManageCategoriesPage gotoPage()
    {
        getUtil().gotoPage("Blog", "ManageCategories", "view");
        return new ManageCategoriesPage();
    }

    /**
     * Click the "Add a Category" link and wait until the form shows up
     */
    public void clickAddCategory()
    {
        this.addCategoryLink.click();
        getDriver().waitUntilElementIsVisible(By.className("category-add-form"));
    }

    /**
     * Click cancel on the category add form
     */
    public void cancelAddingCategory()
    {
        this.cancelLink.click();
    }

    /**
     * Add a new top-level category with the given name, {@link #clickAddCategory()} should be called first
     * 
     * @param name
     */
    public void addCategory(String name)
    {
        this.nameInput.sendKeys(name);
        this.addButton.submit();

        getDriver().waitUntilElementIsVisible(categoryLocator(name));
    }

    /**
     * Delete a category by name.
     * 
     * @param name category name, must exist
     */
    public void deleteCategory(String name)
    {
        // click delete button
        By deletePath =
            By.xpath("//a[@class='tool delete' and contains(@href, '" + getUtil().escapeURL(name) + "')]/img");
        hoverCategoryItem(name);
        getDriver().waitUntilElementIsVisible(deletePath);
        getDriver().findElement(deletePath).click();

        // answer yes in confirmation dialog
        getDriver().waitUntilElementIsVisible(By.className("xdialog-box-confirmation"));
        getDriver().findElement(By.xpath("//div[contains(@class, 'xdialog-box-confirmation')]//input[@value='Yes']"))
            .click();
        getDriver().waitUntilElementDisappears(categoryLocator(name));
    }

    /**
     * Rename an existing category.
     * 
     * @param fromName source category name, must exist
     * @param toName target category name, must not exist
     */
    public void renameCategory(String fromName, String toName)
    {
        // show the rename form
        By renamePath =
            By.xpath("//a[@class='tool rename' and contains(@href, '" + getUtil().escapeURL(fromName) + "')]/img");
        hoverCategoryItem(fromName);
        getDriver().waitUntilElementIsVisible(renamePath);
        getDriver().findElement(renamePath).click();
        getDriver().waitUntilElementIsVisible(By.className("category-rename-form"));

        // rename and wait for result
        this.nameInput.sendKeys(toName);
        this.renameButton.submit();
        getDriver().waitUntilElementIsVisible(categoryLocator(toName));
    }

    /**
     * Check if the given category is present in the categories tree
     * 
     * @param name category name
     */
    public boolean isCategoryPresent(String name)
    {
        return !getDriver().findElementsWithoutWaiting(categoryLocator(name)).isEmpty();
    }

    /**
     * Since the toolbox (with rename, add subcategory and delete buttons) is hidden by default, we need to hover it
     * before any interaction, otherwise Selenium will complain.
     * 
     * @param name category name, must exist
     */
    private void hoverCategoryItem(String name)
    {
        By locator =
            By.xpath("//a[@class='tool delete' and contains(@href, '" + getUtil().escapeURL(name)
                + "')]/ancestor::span[@class='blog-category-tools']");
        getDriver().makeElementVisible(locator);
    }

    /**
     * Return a xpath locator for the given category. Not guaranteed to find exactly one occurrence.
     * Excludes the RSS link for a better chance to find problems with the link to the category page 
     * 
     * @param name category name
     */
    private By categoryLocator(String name)
    {
        return By.xpath("//span[@class='blog-category']//a[contains(@href, '" + getUtil().escapeURL(name) + "') and not(@title='RSS')]");
    }
}
