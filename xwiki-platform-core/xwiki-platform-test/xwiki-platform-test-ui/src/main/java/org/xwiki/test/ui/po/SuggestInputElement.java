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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/**
 * Represents the actions possible on the suggest input widget.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
public class SuggestInputElement extends BaseElement
{
    public class SuggestionElement extends BaseElement
    {
        private final WebElement suggestion;

        public SuggestionElement(WebElement suggestion)
        {
            this.suggestion = suggestion;
        }

        /**
         * @return the value of this suggestion
         */
        public String getValue()
        {
            return this.suggestion.getAttribute("data-value");
        }

        /**
         * @return the label of this suggestion
         */
        public String getLabel()
        {
            return this.suggestion.findElement(By.className("xwiki-selectize-option-label")).getText();
        }

        /**
         * @return the icon class or src attribute of this suggestion
         */
        public String getIcon()
        {
            WebElement icon = this.suggestion.findElement(By.className("xwiki-selectize-option-icon"));
            return "img".equals(icon.getTagName()) ? icon.getAttribute("src") : icon.getAttribute("class");
        }

        /**
         * @return the url of this suggestion
         */
        public String getURL()
        {
            return this.suggestion.findElement(By.className("xwiki-selectize-option-label")).getAttribute("href");
        }

        /**
         * @return the hint of this suggestion
         */
        public String getHint()
        {
            return this.suggestion.findElement(By.className("xwiki-selectize-option-hint")).getText();
        }

        /**
         * @return {@code true} if the hint is present, {@code false} otherwise
         */
        public boolean hasHint()
        {
            return !getDriver().findElementsWithoutWaiting(this.suggestion, By.className("xwiki-selectize-option-hint"))
                .isEmpty();
        }

        /**
         * @return the text displayed on hover
         */
        public String getTooltip()
        {
            return this.suggestion.getAttribute("title");
        }

        /**
         * Deletes this suggestion from the list of selected suggestions.
         */
        public void delete()
        {
            select();

            getTextInput().sendKeys(Keys.BACK_SPACE);
        }

        /**
         * Selects this suggestion.
         */
        public void select()
        {
            this.suggestion.click();
        }
    }

    private final WebElement originalInput;

    private WebElement container;

    public SuggestInputElement(WebElement originalInput)
    {
        this.originalInput = originalInput;

        // Wait for the original input element to be enhanced.
        getDriver().waitUntilCondition(driver -> this.originalInput.getAttribute("class").contains("selectized"));

        this.container = getDriver().findElementWithoutWaiting(this.originalInput,
            By.xpath("following-sibling::*[contains(@class, 'selectize-control')][1]"));

        // Wait for the selection to be initialized.
        getDriver().waitUntilCondition(driver -> !this.container.getAttribute("class").contains("loading"));
    }

    /**
     * @return the actual text input
     */
    private WebElement getTextInput()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector(".selectize-input > input"));
    }

    /**
     * Clicks on the text input.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement click()
    {
        // If we simply click on the container we risk highlighting one of the selected suggestions (because the default
        // click is performed in the center of the element) and this hides the text input when multiple selection is on.
        // Safest is to click on the top left corner of the suggest input, before the first selected suggestion.
        // And selenium only move to the center of the element, so we have to compute the offset for the top-left corner
        // and we click on (2,2) to avoid missing it.
        getDriver().moveToTopLeftCornerOfTargetWithOffset(this.container, 2, 2, null).click().build().perform();
        return this;
    }

    /**
     * Removes the typed text.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement clear()
    {
        // We cannot call WebElement#clear method because it does not fire the right keyboard events,
        // so better to rely on a key combination to remove the content.
        getTextInput().sendKeys(Keys.chord(Keys.CONTROL, "a", Keys.BACK_SPACE));
        return this;
    }

    /**
     * Removes all the selected elements.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement clearSelectedSuggestions()
    {
        getSelectedSuggestions().forEach(SuggestionElement::delete);
        return this;
    }

    /**
     * Sends the given sequence of keys to the input.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement sendKeys(CharSequence... keysToSend)
    {
        getTextInput().sendKeys(keysToSend);
        return this;
    }

    /**
     * Waits until the suggestions have been loaded.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement waitForSuggestions()
    {
        getDriver().waitUntilCondition(driver -> !this.container.getAttribute("class").contains("loading")
            && !driver.findElements(By.cssSelector(".selectize-dropdown.active")).isEmpty());
        return this;
    }

    /**
     * Waits until the suggestions have disappeared.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement waitForSuggestionsClearance()
    {
        getDriver().waitUntilCondition(driver ->
            getDriver().findElementsWithoutWaiting(this.container, By.xpath("//*[contains(@class, 'selectize-input') "
                + "and contains(@class, 'dropdown-active')]")).size() == 0);
        return this;
    }

    /**
     * @return a list of all the suggestion elements
     */
    public List<SuggestionElement> getSuggestions()
    {
        return getDriver()
            .findElementsWithoutWaiting(By.cssSelector(".selectize-dropdown.active .xwiki-selectize-option")).stream()
            .map(SuggestionElement::new).toList();
    }

    /**
     * Selects an element by clicking on the suggestion with the given position.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectByIndex(int index)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'selectize-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option')][" + (index + 1) + "]"))
            .click();

        return this;
    }

    /**
     * Selects an element by clicking on the suggestion with the given value.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectByValue(String value)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'selectize-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option') and @data-value = '" + value + "']"))
            .click();

        return this;
    }

    /**
     * Selects an element by clicking on the suggestion with the given label.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectByVisibleText(String text)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'selectize-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option-label') and . = '" + text + "']"))
            .click();

        return this;
    }

    /**
     * Selects and creates an element with the input text.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectTypedText()
    {
        getDriver().findElementWithoutWaiting(By.cssSelector(".selectize-dropdown.active .create")).click();

        return this;
    }

    /**
     * @return list of all the values of the selected elements.
     */
    public List<String> getValues()
    {
        if ("select".equals(this.originalInput.getTagName())) {
            return new Select(this.originalInput).getAllSelectedOptions().stream()
                .map(option -> option.getAttribute("value")).toList();
        } else {
            return Arrays.asList(this.originalInput.getAttribute("value").split(","));
        }
    }

    /**
     * @return list of suggestion elements found in the suggestion panel
     */
    public List<SuggestionElement> getSelectedSuggestions()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.className("xwiki-selectize-option")).stream()
            .map(SuggestionElement::new).toList();
    }

    /**
     * Hides the suggestions panel.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement hideSuggestions()
    {
        getTextInput().sendKeys(Keys.ESCAPE);
        waitForSuggestionsClearance();
        return this;
    }
}
