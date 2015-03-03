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
package org.xwiki.test.ui.po.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the actions possible on the user picker.
 * 
 * @version $Id$
 * @since 4.5
 */
public class UserPicker extends BaseElement
{
    public static class UserElement extends BaseElement
    {
        /**
         * The element that wraps the user display.
         */
        private WebElement container;

        /**
         * Creates a new user element that wraps the given {@link WebElement}.
         * 
         * @param container the element that wraps the user display
         */
        public UserElement(WebElement container)
        {
            this.container = container;
        }

        /**
         * @return the avatar image
         */
        public WebElement getAvatar()
        {
            return getDriver().findElementWithoutWaiting(container, By.className("icon"));
        }

        /**
         * @return the full user name
         */
        public String getName()
        {
            String name = getDriver().findElementWithoutWaiting(container, By.className("user-name")).getText();
            // Remove the "x" coming from the delete icon.
            List<WebElement> delete = getDriver().findElementsWithoutWaiting(container, By.className("delete-tool"));
            if (delete.size() > 0) {
                name = StringUtils.removeEnd(name, delete.get(0).getText());
            }
            return name;
        }

        /**
         * @return the user alias
         */
        public String getAlias()
        {
            return getDriver().findElementWithoutWaiting(container, By.className("user-alias")).getText();
        }

        /**
         * @return the text displayed by this element; this is useful when this element is not actually an user but a
         *         message like "User not found"
         */
        public String getText()
        {
            return container.getText();
        }

        /**
         * Remove this user from the current selection.
         */
        public void delete()
        {
            new Actions(getDriver()).moveToElement(container).click(getDriver().findElementWithoutWaiting(container,
                By.className("delete-tool"))).perform();
        }

        /**
         * Moves the mouse over this element.
         */
        public void moveMouseOver()
        {
            new Actions(getDriver()).moveToElement(container).perform();
        }

        /**
         * Moves this user before the given user using drag and drop.
         * 
         * @param user the reference user
         */
        public void moveBefore(UserElement user)
        {
            new Actions(getDriver()).clickAndHold(container).moveToElement(user.container, 0, 0).release().perform();
        }
    }

    /**
     * The text input that is enhanced with a user picker.
     */
    private final WebElement textInput;

    /**
     * Exposes the user picker bound to the given text input;
     * 
     * @param textInput the text input that is enhanced with a user picker
     */
    public UserPicker(WebElement textInput)
    {
        this.textInput = textInput;
    }

    /**
     * Types into the text input.
     * 
     * @param keysToSend the keys to type into the text input
     * @return this
     */
    public UserPicker sendKeys(CharSequence... keysToSend)
    {
        textInput.sendKeys(keysToSend);
        return this;
    }

    /**
     * Clears the content of the text input.
     * 
     * @return this
     */
    public UserPicker clear()
    {
        textInput.clear();
        return this;
    }

    /**
     * @return the value of the text input
     */
    public String getValue()
    {
        return textInput.getAttribute("value");
    }

    /**
     * @return the link element used to clear the list of selected users
     */
    public WebElement getClearSelectionLink()
    {
        String xpath = "preceding-sibling::a[@class = 'clear-tool' and position() = last()]";
        return getDriver().findElementWithoutWaiting(textInput, By.xpath(xpath));
    }

    /**
     * Waits until the suggestions for the current value of the text input are retrieved.
     * 
     * @return this
     */
    public UserPicker waitForSuggestions()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                return !textInput.getAttribute("class").contains("loading") && isSuggestListDisplayed(driver);
            }

            private boolean isSuggestListDisplayed(WebDriver driver)
            {
                WebElement suggestItems = driver.findElement(By.className("suggestItems"));
                // div.suggestItems is added to the document before the suggestions are retrieved. We need to wait for
                // the actual list of suggestions (ul.suggestList).
                WebElement suggestList = suggestItems.findElement(By.className("suggestList"));
                // div.suggestItems fades when closing.
                return suggestList.isDisplayed() && Double.parseDouble(suggestItems.getCssValue("opacity")) == 1;
            }
        });
        return this;
    }

    /**
     * Waits until the list of suggestions disappears.
     * 
     * @param timeout how long to wait, in seconds
     * @return this
     */
    private UserPicker waitForSuggestionsToDisappear(int timeout)
    {
        int previousTimeout = getDriver().getTimeout();
        getDriver().setTimeout(timeout);
        try {
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                public Boolean apply(WebDriver driver)
                {
                    return driver.findElements(By.className("suggestItems")).size() == 0;
                }
            });
        } finally {
            getDriver().setTimeout(previousTimeout);
        }
        return this;
    }

    /**
     * Waits for the list of suggestions to fade out.
     * 
     * @return this
     */
    public UserPicker waitForSuggestionsToFadeOut()
    {
        return waitForSuggestionsToDisappear(2);
    }

    /**
     * Waits for the list of suggestions to disappear.
     * 
     * @return this
     */
    public UserPicker waitForSuggestionsToDisappear()
    {
        return waitForSuggestionsToDisappear(35);
    }

    /**
     * Clicks on the suggestion that contains the given text.
     * 
     * @param userNameOrAlias user name or alias
     * @return this
     */
    public UserPicker select(String userNameOrAlias)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//ul[@class = 'xlist suggestList']/li[contains(., '" + userNameOrAlias + "')]")).click();
        return this;
    }

    /**
     * @return the list of suggested users based on the value of the text input
     */
    public List<UserElement> getSuggestions()
    {
        List<UserElement> suggestions = new ArrayList<UserElement>();
        for (WebElement item : getDriver().findElementsWithoutWaiting(
            By.xpath("//ul[@class = 'xlist suggestList']/li")))
        {
            suggestions.add(new UserElement(item));
        }
        return suggestions;
    }

    /**
     * @return the list of selected users
     */
    public List<UserElement> getAcceptedSuggestions()
    {
        List<UserElement> acceptedSuggestions = new ArrayList<UserElement>();
        for (WebElement item : getDriver().findElementsWithoutWaiting(textInput,
            By.xpath("preceding-sibling::ul[contains(@class, 'accepted-suggestions') and position() = last()]/li")))
        {
            acceptedSuggestions.add(new UserElement(item));
        }
        return acceptedSuggestions;
    }

    /**
     * Moves the mouse over the text input.
     * 
     * @return this
     */
    public UserPicker moveMouseOver()
    {
        new Actions(getDriver()).moveToElement(textInput).perform();
        return this;
    }

    /**
     * Waits for the user picker to load. You need to call this when the initial selection is not empty (because the
     * user picker makes separate requests to retrieve information about the selected users).
     * 
     * @return this
     */
    public UserPicker waitToLoad()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            public Boolean apply(WebDriver driver)
            {
                String classNames = textInput.getAttribute("class");
                return classNames.contains("initialized") && !classNames.contains("loading");
            }
        });
        return this;
    }
}
