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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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
            return "img".equals(icon.getTagName()) ? icon.getAttribute("src") : icon.getAttribute(ATTRIBUTE_CLASS);
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

    /**
     * Whether to wait for remove suggestions to be fetched when the dropdown is opened. This is used by
     * {@link #waitForSuggestions()} when the caller doesn't specify whether the suggestions are loaded from a remote
     * source or not.
     */
    private boolean shouldWaitForRemoteSuggestions;

    /**
     * Checks if the suggest input widget is available on the given element.
     * 
     * @param executor the JavaScript executor used to check if the suggest input widget is available
     * @param targetElement the element on which to check if the suggest input widget is available
     * @return {@code true} if the suggest input widget is available on the given element, {@code false} otherwise
     * @since 18.2.0RC1
     */
    public static boolean isAvailable(JavascriptExecutor executor, WebElement targetElement)
    {
        return Boolean.TRUE.equals(executor.executeScript("return !!arguments[0].selectize", targetElement));
    }

    public SuggestInputElement(WebElement originalInput)
    {
        this.originalInput = originalInput;

        // Wait for the original input element to be enhanced.
        getDriver().waitUntilCondition(driver -> StringUtils
            .defaultString(this.originalInput.getAttribute(ATTRIBUTE_CLASS)).contains("tomselected"));

        this.container = getDriver().findElementWithoutWaiting(this.originalInput,
            By.xpath("following-sibling::*[contains(@class, 'ts-wrapper')][1]"));

        // Wait for the selection to be initialized.
        getDriver().waitUntilCondition(driver -> !isLoading());
    }

    private boolean isLoading()
    {
        return StringUtils.defaultString(this.container.getAttribute(ATTRIBUTE_CLASS)).contains("loading");
    }

    /**
     * @return the actual text input
     */
    private WebElement getTextInput()
    {
        return getDriver().findElementWithoutWaiting(this.container, By.cssSelector(".ts-control > input"));
    }

    /**
     * Clicks on the text input.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement click()
    {
        getDriver().executeScript("arguments[0].selectize.focus()", this.originalInput);
        this.shouldWaitForRemoteSuggestions = false;
        return this;
    }

    /**
     * Removes the typed text.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement clear()
    {
        WebElement textInput = getTextInput();
        if (!textInput.isDisplayed()) {
            // The text input is not visible when the widget is not focused and single selection is enabled. We need to
            // focus the widget first.
            click();
        }
        textInput.clear();
        this.shouldWaitForRemoteSuggestions = false;
        return this;
    }

    /**
     * Removes all the selected elements.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement clearSelectedSuggestions()
    {
        // When single selection is enabled we can't focus the selected item to delete it. The only way to clear the
        // selection is to clear the text input and then send the backspace key to delete the selected item. We apply
        // the same strategy when multiple selection is enabled, in order to use the same code and also because it
        // avoids the need to click on each of the selected items, which might fail if there is some overlaid element
        // (like the balloon context toolbar shown when hovering a live data cell, with the action to edit the cell).
        clear();
        sendKeys(Keys.BACK_SPACE.toString().repeat(getSelectedSuggestions().size()));
        this.shouldWaitForRemoteSuggestions = false;
        return this;
    }

    /**
     * Sends the given sequence of keys to the input.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement sendKeys(CharSequence... keysToSend)
    {
        if (keysToSend != null) {
            maybeInsertReloadMarker();
            getTextInput().sendKeys(keysToSend);
            this.shouldWaitForRemoteSuggestions = true;
        }
        return this;
    }

    private void maybeInsertReloadMarker()
    {
        getDriver().executeScript("""
            const reloadMarker = document.createElement('div');
            reloadMarker.hidden = true;
            reloadMarker.classList.add('xwiki-selectize-dropdown-reload-marker');
            const dropdownContent = document.querySelector('.ts-dropdown.active .ts-dropdown-content');
            dropdownContent?.append(reloadMarker);
            """);
    }

    /**
     * Unfortunately, there is no way to know whether the current suggestions correspond to the current input value or
     * not. TomSelect caches queries and it doesn't set the loading state when suggestions are loaded from the cache.
     * Moreover, it caches the suggestion rendering as well, so suggestions that are shared between different queries
     * are not re-rendered. As a consequence, if two different queries have the same results, the content of the
     * suggestions dropdown is identical. The workaround is to detect when the content of the dropdown is cleared and
     * reloaded (even if the new content is the same).
     * 
     * @param remote whether the suggestions are loaded from a remote source or not, which can be used to adjust the
     *            waiting
     */
    private void waitForDropdownReload(boolean remote)
    {
        if (remote) {
            // Wait for remote suggestions to be fetched.
            getDriver().waitUntilCondition(driver -> isLoading());
        } else {
            // Wait for cached suggestions.
            getDriver().waitUntilCondition(ExpectedConditions.numberOfElementsToBe(
                By.cssSelector(".ts-dropdown.active .xwiki-selectize-dropdown-reload-marker"), 0));
        }
        this.shouldWaitForRemoteSuggestions = false;
    }

    /**
     * Waits until the suggestions have been loaded.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement waitForSuggestions()
    {
        return waitForSuggestions(this.shouldWaitForRemoteSuggestions);
    }

    /**
     * Waits until the suggestions have been loaded.
     *
     * @param remote whether the suggestions are loaded from a remote source or not, which can be used to adjust the
     *            waiting
     * @return the current suggest input element
     * @since 18.2.0RC1
     */
    public SuggestInputElement waitForSuggestions(boolean remote)
    {
        waitForDropdownReload(remote);
        // Wait for suggestions to be displayed.
        getDriver().waitUntilCondition(
            driver -> !isLoading() && !driver.findElements(By.cssSelector(".ts-dropdown.active")).isEmpty());
        return this;
    }

    /**
     * Waits until suggestions beyond the option to choose the typed text are displayed.
     *
     * @return the current suggest input element
     * @since 17.8.0RC1
     * @since 17.4.5
     * @since 16.10.12
     */
    public SuggestInputElement waitForNonTypedSuggestions()
    {
        return waitForNonTypedSuggestions(this.shouldWaitForRemoteSuggestions);
    }

    /**
     * Waits until suggestions beyond the option to choose the typed text are displayed.
     *
     * @param remote whether the suggestions are loaded from a remote source or not, which can be used to adjust the
     *            waiting
     * @return the current suggest input element
     * @since 18.2.0RC1
     */
    public SuggestInputElement waitForNonTypedSuggestions(boolean remote)
    {
        waitForDropdownReload(remote);
        getDriver().waitUntilCondition(driver -> !isLoading()
            && !driver.findElements(By.cssSelector(".ts-dropdown.active .xwiki-selectize-option")).isEmpty());
        return this;
    }

    /**
     * Waits until the suggestions have disappeared.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement waitForSuggestionsClearance()
    {
        getDriver().waitUntilCondition(driver -> getDriver()
            .findElementsWithoutWaiting(this.container, By.cssSelector(".ts-wrapper.dropdown-active")).isEmpty());
        return this;
    }

    /**
     * @return a list of all the suggestion elements
     */
    public List<SuggestionElement> getSuggestions()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".ts-dropdown.active .xwiki-selectize-option"))
            .stream().map(SuggestionElement::new).toList();
    }

    /**
     * Selects an element by clicking on the suggestion with the given position.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectByIndex(int index)
    {
        getDriver().findElement(By.xpath("//*[contains(@class, 'ts-dropdown') and contains(@class, 'active')]"
            + "//*[contains(@class, 'xwiki-selectize-option')][" + (index + 1) + "]")).click();

        return this;
    }

    /**
     * Selects an element by clicking on the suggestion with the given value.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectByValue(String value)
    {
        getDriver().findElement(optionByValue(value, null)).click();

        return this;
    }

    @SuppressWarnings("null")
    private By optionByValue(String value, Boolean selected)
    {
        return By.xpath(String.format(
            "//*[contains(@class, 'ts-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option')%s and @data-value = '%s']",
            getSelectedConstraint(selected), StringUtils.defaultString(value)));
    }

    private String getSelectedConstraint(Boolean selected)
    {
        if (selected == null) {
            return "";
        } else if (selected) {
            return " and contains(@class, 'active')";
        } else {
            return " and not(contains(@class, 'active'))";
        }
    }

    /**
     * Selects an element by clicking on the suggestion with the given label.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectByVisibleText(String text)
    {
        getDriver().findElement(optionByLabel(text, null)).click();

        return this;
    }

    @SuppressWarnings("null")
    private By optionByLabel(String value, Boolean selected)
    {
        return By.xpath(String.format(
            "//*[contains(@class, 'ts-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'xwiki-selectize-option')%s]"
                + "/*[contains(@class, 'xwiki-selectize-option-label') and . = '%s']",
            getSelectedConstraint(selected), StringUtils.defaultString(value)));
    }

    /**
     * Selects and creates an element with the input text.
     *
     * @return the current suggest input element
     */
    public SuggestInputElement selectTypedText()
    {
        WebElement textInput = getTextInput();
        String typedText = StringUtils.defaultString(textInput.getAttribute(ATTRIBUTE_VALUE));
        getDriver().waitUntilElementsAreVisible(new By[] {
            // Option to create a new item with the typed text as value.
            By.xpath("//*[contains(@class, 'ts-dropdown') and contains(@class, 'active')]"
                + "//*[contains(@class, 'create') and contains(@class, 'active')]/em[. = '" + typedText + "']"),
            // Existing option with the same value as the typed text.
            optionByValue(typedText, true),
            // Existing option with the same label as the typed text.
            optionByLabel(typedText, true)}, false);

        // Pick the selected option.
        textInput.sendKeys(Keys.ENTER);

        // The typed text is kept when we select an existing option with that value, which is not what we want most of
        // the time.
        textInput.clear();

        return this;
    }

    /**
     * @return list of all the values of the selected elements.
     */
    public List<String> getValues()
    {
        if ("select".equals(this.originalInput.getTagName())) {
            return new Select(this.originalInput).getAllSelectedOptions().stream()
                .map(option -> option.getAttribute(ATTRIBUTE_VALUE)).toList();
        } else {
            return Arrays
                .asList(StringUtils.defaultString(this.originalInput.getAttribute(ATTRIBUTE_VALUE)).split(","));
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
        if (isDropDownOpened()) {
            getTextInput().sendKeys(Keys.ESCAPE);
            waitForSuggestionsClearance();
        }
        return this;
    }

    /**
     * @return {@code true} if the suggestions dropdown is opened, {@code false} otherwise
     */
    public boolean isDropDownOpened()
    {
        return StringUtils.defaultString(this.container.getAttribute(ATTRIBUTE_CLASS)).contains("dropdown-active");
    }
}
