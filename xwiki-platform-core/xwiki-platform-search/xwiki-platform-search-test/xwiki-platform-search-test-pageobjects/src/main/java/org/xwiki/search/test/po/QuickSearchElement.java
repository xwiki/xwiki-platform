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
package org.xwiki.search.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;

public class QuickSearchElement extends BaseElement
{
    private static final By SOURCE_NAME_SELECTOR = By.cssSelector(".searchSuggest .results .sourceName");

    @FindBy(id = "headerglobalsearchinput")
    private WebElement searchInput;

    @FindBy(css = "#globalsearch button")
    private WebElement searchButton;

    /**
     * Enter text in the quick search box.
     * @param terms text to search
     */
    public void search(String terms)
    {
        this.searchButton.click();
        this.searchInput.clear();
        this.searchInput.sendKeys(terms);
        waitForSuggestions();
    }

    /**
     * Reopens the suggestions panel for the terms already in the quick search box, by retyping their last character.
     * Note that neither clicking the search button nor clearing the input can be used here, since the former submits
     * the form and the latter takes the focus away from the input, which collapses the search box.
     *
     * @param terms the terms currently in the quick search box
     * @since 18.9.0RC1
     */
    public void reopenSuggestions(String terms)
    {
        this.searchInput.sendKeys(Keys.BACK_SPACE, terms.substring(terms.length() - 1));
        waitForSuggestions();
    }

    /**
     * @param source the search suggest source that provides the results, e.g. "Page titles"
     * @return the list of quick search results from the given source
     * @since 17.8.0RC1
     */
    public List<QuickSearchResult> getResults(String source)
    {
        By sourceContainerSelector =
            By.xpath("//div[contains(@class, 'searchSuggest')]//div[contains(@class, 'results')]"
                + "[descendant::div[contains(@class, 'sourceName') and text() = '" + source + "']]");
        getDriver().waitUntilElementIsVisible(sourceContainerSelector);
        return getDriver().findElement(sourceContainerSelector).findElements(By.cssSelector(".suggestList .xitem"))
            .stream().map(QuickSearchResult::new).toList();
    }

    /**
     * @return {@code true} if the "Go to search page..." row is the last suggestion item
     * @since 18.9.0RC1
     */
    public boolean isShowAllResultsLast()
    {
        // The showAllResults class is on the list item, not on its suggestItem container child.
        List<WebElement> suggestItems =
            getDriver().findElementsWithoutWaiting(By.cssSelector(".searchSuggest li.xitem"));
        return !suggestItems.isEmpty()
            && suggestItems.get(suggestItems.size() - 1).getAttribute("class").contains("showAllResults");
    }

    /**
     * Closes the suggestions panel by pressing the Escape key.
     *
     * @since 18.9.0RC1
     */
    public void closeSuggestions()
    {
        this.searchInput.sendKeys(Keys.ESCAPE);
        getDriver().waitUntilElementDisappears(By.cssSelector(".searchSuggest"));
    }

    private void waitForSuggestions()
    {
        // Wait for the suggestions to be requested, i.e. for the result containers of the sources to be prepared. Don't
        // wait for the "loading" class of the "Go to search page" item: the search suggest widget is created
        // asynchronously, when the search field is first reached (its sources are fetched from the server), so it can
        // be ready only after all the terms have been typed. It then requests the suggestions only once, and in that
        // case the source result containers are created and only them are marked as loading, never the "Go to search
        // page" item. Don't wait for all the sources to be loaded either: some sources may never complete (e.g. the
        // "Wikis" source stays loading in the test environment). When the widget already exists, each typed key
        // (re)marks the source result containers as loading and hides them synchronously, so getResults() can't match
        // stale results.
        getDriver().waitUntilCondition(driver -> !driver.findElements(SOURCE_NAME_SELECTOR).isEmpty());
    }
}
