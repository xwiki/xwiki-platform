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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/**
 * Represents the actions possible on the selectize.
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class SelectizeElement extends BaseElement
{
    public static class SelectizeElementItem extends BaseElement
    {
        /**
         * The item in the selectize input.
         */
        private WebElement item;

        /**
         * Creates a new selectize item element that wraps the given {@link WebElement}.
         *
         * @param item the item element
         */
        private SelectizeElementItem(WebElement item)
        {
            this.item = item;
        }

        public WebElement getIcon()
        {
            return item.findElement(By.className("xwiki-selectize-option-icon"));
        }

        public String getLabel()
        {
            return item.findElement(By.className("xwiki-selectize-option-label")).getText();
        }

        /**
         * Removes this item from the current selection.
         */
        public void delete()
        {
            item.click();
            getDriver().getKeyboard().sendKeys(Keys.BACK_SPACE);
        }
    }

    /**
     * The initial select.
     */
    private final WebElement select;

    /**
     * The text input that is enhanced with a selectize.
     */
    private final WebElement textInput;

    /**
     * Exposes the selectize bound to the given text input;
     *
     * @param select select used with the selectize input
     */
    public SelectizeElement(WebElement select)
    {
        this.select = select;
        this.textInput = getDriver().findElementById(this.select.getAttribute("id") + "-selectized");
    }

    /**
     * Types into the text input.
     *
     * @param keysToSend the keys to type into the text input
     * @return this
     */
    public SelectizeElement sendKeys(CharSequence... keysToSend)
    {
        textInput.click();
        textInput.sendKeys(keysToSend);
        return this;
    }

    /**
     * Clears the content of the text input.
     *
     * @return this
     */
    public SelectizeElement clear()
    {
        textInput.clear();
        return this;
    }

    /**
     * Delete selected items.
     *
     * @return this
     */
    public SelectizeElement deleteItems()
    {
        getAcceptedSuggestions().forEach(SelectizeElementItem::delete);
        return this;
    }

    /**
     * Clicks on the suggestion that contains the given text.
     *
     * @param userName user name
     * @return this
     */
    public SelectizeElement select(String userName)
    {
        getDriver().findElementByCssSelector(
            ".selectize-dropdown-content:not([style*='display: none']) [data-value='" + userName + "']").click();
        return this;
    }

    /**
     * Clicks on the first suggestion.
     *
     * @return this
     */
    public SelectizeElement selectFirst()
    {
        getDriver().findElementsByCssSelector(
            ".selectize-dropdown-content:not([style*='display: none']) .xwiki-selectize-option").get(0).click();
        return this;
    }

    /**
     * Clicks on the create select element.
     *
     * @return this
     */
    public SelectizeElement selectCreate()
    {
        getDriver().findElementsByCssSelector(
            ".selectize-dropdown-content:not([style*='display: none']) .create").get(0).click();
        return this;
    }

    /**
     * @return the list of suggested items based on the value of the text input
     */
    public List<SelectizeElementItem> getSuggestions()
    {
        return getDriver().findElementsByCssSelector(
            ".selectize-dropdown-content:not([style*='display: none']) .xwiki-selectize-option"
        ).stream().map(SelectizeElementItem::new).collect(Collectors.toList());
    }

    /**
     * @return the list of selected items.
     */
    public List<SelectizeElementItem> getAcceptedSuggestions()
    {
        return select.findElements(
            By.xpath("following-sibling::*[contains(@class, 'selectize-control')][1]" +
                "/descendant::*[@class = 'xwiki-selectize-option']")
        ).stream().map(SelectizeElementItem::new).collect(Collectors.toList());
    }

    /**
     * @return the value of the text input
     */
    public String getValue()
    {
        return textInput.getAttribute("value");
    }

    /**
     * Waits until the suggestions for the current value of the text input are retrieved.
     *
     * @return this
     */
    public SelectizeElement waitForSuggestions()
    {
        getDriver().waitUntilCondition(driver -> !textInput.getAttribute("class").contains("loading"));
        return this;
    }
}
