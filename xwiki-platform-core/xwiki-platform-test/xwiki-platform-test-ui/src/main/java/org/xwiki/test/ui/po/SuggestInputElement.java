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
import java.util.stream.Collectors;

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
    public static class SuggestionElement extends BaseElement
    {
        private final WebElement suggestion;

        public SuggestionElement(WebElement suggestion)
        {
            this.suggestion = suggestion;
        }

        public String getValue()
        {
            return this.suggestion.getAttribute("data-value");
        }

        public String getLabel()
        {
            return this.suggestion.findElement(By.className("xwiki-selectize-option-label")).getText();
        }

        public String getIcon()
        {
            WebElement icon = this.suggestion.findElement(By.className("xwiki-selectize-option-icon"));
            return "img".equals(icon.getTagName()) ? icon.getAttribute("src") : icon.getAttribute("class");
        }

        public String getURL()
        {
            return this.suggestion.findElement(By.className("xwiki-selectize-option-label")).getAttribute("href");
        }

        public String getHint()
        {
            return this.suggestion.findElement(By.className("xwiki-selectize-option-hint")).getText();
        }

        /**
         * Deletes this suggestion from the list of selected suggestions.
         */
        public void delete()
        {
            select();

            // We don't send the keys directly to the text input because it can be hidden.(e.g. when multiple selection
            // is on and we click on a selected suggestion).
            getDriver().getKeyboard().sendKeys(Keys.BACK_SPACE);
        }

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

    private WebElement getTextInput()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector("input[type='text']"));
    }

    public SuggestInputElement click()
    {
        this.container.click();
        return this;
    }

    public SuggestInputElement clear()
    {
        getTextInput().clear();
        return this;
    }

    public SuggestInputElement clearSelectedSuggestions()
    {
        getSelectedSuggestions().forEach(SuggestionElement::delete);
        return this;
    }

    public SuggestInputElement sendKeys(CharSequence... keysToSend)
    {
        getTextInput().sendKeys(keysToSend);
        return this;
    }

    public SuggestInputElement waitForSuggestions()
    {
        // Wait for the suggestions to be fetched from the server and for the suggestion drop down list to be visible.
        getDriver().waitUntilCondition(driver -> !this.container.getAttribute("class").contains("loading")
            && !driver.findElements(By.cssSelector(".selectize-dropdown.active")).isEmpty());

        return this;
    }

    public List<SuggestionElement> getSuggestions()
    {
        return getDriver()
            .findElementsWithoutWaiting(By.cssSelector(".selectize-dropdown.active .xwiki-selectize-option")).stream()
            .map(SuggestionElement::new).collect(Collectors.toList());
    }

    public SuggestInputElement selectByIndex(int index)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'selectize-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option')][" + (index + 1) + "]"))
            .click();

        return this;
    }

    public SuggestInputElement selectByValue(String value)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'selectize-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option') and @data-value = '" + value + "']"))
            .click();

        return this;
    }

    public SuggestInputElement selectByVisibleText(String text)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//*[contains(@class, 'selectize-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option-label') and . = '" + text + "']"))
            .click();

        return this;
    }

    public SuggestInputElement selectTypedText()
    {
        getDriver().findElementWithoutWaiting(By.cssSelector(".selectize-dropdown.active .create")).click();

        return this;
    }

    public List<String> getValue()
    {
        if ("select".equals(this.originalInput.getTagName())) {
            return new Select(this.originalInput).getAllSelectedOptions().stream()
                .map(option -> option.getAttribute("value")).collect(Collectors.toList());
        } else {
            return Arrays.asList(this.originalInput.getAttribute("value").split(","));
        }
    }

    public List<SuggestionElement> getSelectedSuggestions()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.className("xwiki-selectize-option")).stream()
            .map(SuggestionElement::new).collect(Collectors.toList());
    }

    public SuggestInputElement hideSuggestions()
    {
        getTextInput().sendKeys(Keys.ESCAPE);

        return this;
    }
}
