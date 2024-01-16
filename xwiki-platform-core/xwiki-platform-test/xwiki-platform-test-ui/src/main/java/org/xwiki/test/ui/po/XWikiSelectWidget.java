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
package org.xwiki.test.ui.po;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents the XWiki Select Widget (select.js).
 * 
 * @version $Id$
 * @since 14.10
 */
public class XWikiSelectWidget extends BaseElement
{
    protected final WebElement container;

    protected final String fieldName;

    public XWikiSelectWidget(By selector, String fieldName)
    {
        this.container = getDriver().findElement(selector);
        this.fieldName = fieldName;
        waitUntilReady();
    }

    public XWikiSelectWidget(WebElement container, String fieldName)
    {
        this.container = container;
        this.fieldName = fieldName;
        waitUntilReady();
    }

    private void waitUntilReady()
    {
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(this.container, "data-ready", "true"));
    }

    /**
     * Select the option with the specified value. The option must be visible.
     * 
     * @param value the option value to look for
     */
    public void selectByValue(String value)
    {
        getOptionLabel(getOptionByValue(value)).click();
    }

    /**
     * Select the option with the specified label. The option must be visible.
     * 
     * @param label the option label to look for
     */
    public void selectByLabel(String label)
    {
        getOptionLabel(getOptionByLabel(label).get()).click();
    }

    /**
     * @return the list of category names
     */
    public List<String> getCategoryNames()
    {
        return getCategoriesStream().map(category -> StringUtils.substringBefore(category.getText(), '(').trim())
            .toList();
    }

    private Stream<WebElement> getCategoriesStream()
    {
        return this.container.findElements(By.className("xwiki-select-category")).stream()
            .filter(category -> category.isDisplayed());
    }

    /**
     * @param categoryName the name of a category
     * @return the category size displayed after the category name
     */
    public String getDisplayedCategorySize(String categoryName)
    {
        return getCategory(categoryName)
            .map(category -> category.findElement(By.className("xwiki-select-category-count")).getText()).orElse(null);
    }

    private Optional<WebElement> getCategory(String categoryName)
    {
        return getCategoriesStream().filter(category -> category.getText().startsWith(categoryName)).findFirst();
    }

    /**
     * @param categoryName the name of a category
     * @return the labels of the options available in the specified category
     */
    public List<String> getLabelsInCategory(String categoryName)
    {
        return getOptionsInCategory(categoryName).stream().map(this::getOptionLabel).map(label -> label.getText())
            .collect(Collectors.toList());
    }

    private WebElement getOptionLabel(WebElement option)
    {
        return option.findElement(By.tagName("label"));
    }

    private List<WebElement> getOptionsInCategory(String categoryName)
    {
        return getCategory(categoryName).map(category -> category.findElements(By.className("xwiki-select-option"))
            .stream().filter(option -> option.isDisplayed()).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    protected Optional<WebElement> getOptionByLabel(String label)
    {
        return this.container
            .findElements(By.xpath(".//li[contains(@class, 'xwiki-select-option')][.//label[. = '" + label + "']]"))
            .stream().filter(option -> option.isDisplayed()).findFirst();
    }

    protected WebElement getOptionByValue(String value)
    {
        return getOptionsStream().filter(option -> Objects.equals(value, getOptionInput(option).getAttribute("value")))
            .findFirst().get();
    }

    protected Stream<WebElement> getOptionsStream()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.className("xwiki-select-option")).stream()
            .filter(option -> option.isDisplayed());
    }

    protected WebElement getOptionInput(WebElement option)
    {
        return option.findElement(By.cssSelector("input[type=radio][name=" + this.fieldName + "]"));
    }

    protected Stream<WebElement> getOptionInputsStream()
    {
        return getOptionsStream().map(this::getOptionInput);
    }

    /**
     * Filter the options that match the specified text.
     * 
     * @param text the text to match
     */
    public void filter(String text)
    {
        WebElement filterInput = getFilterInput();
        filterInput.clear();
        filterInput.sendKeys(text);
    }

    private WebElement getFilterInput()
    {
        return this.container.findElement(By.className("xwiki-select-filter"));
    }

    /**
     * @param label the label of the option to look for
     * @return {@code true} if the specified option is present, {@code false} otherwise
     */
    public boolean hasOptionWithLabel(String label)
    {
        return getOptionByLabel(label).isPresent();
    }
}
