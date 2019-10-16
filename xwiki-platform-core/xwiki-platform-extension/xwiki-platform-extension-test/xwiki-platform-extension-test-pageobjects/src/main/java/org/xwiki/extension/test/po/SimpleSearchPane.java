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
package org.xwiki.extension.test.po;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.BasePage;

/**
 * Represents the simple extension search form.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class SimpleSearchPane extends BaseElement
{
    /**
     * The text input used to specify the search keywords.
     */
    @FindBy(id = "extensionSearchInput")
    private WebElement searchInput;

    /**
     * The list box used to select the extension repository to search into.
     */
    @FindBy(id = "extensionSearchRepositoryList")
    private WebElement repositorySelect;

    /**
     * The link to open the advanced search pane.
     */
    @FindBy(linkText = "Advanced search")
    private WebElement advancedSearchLink;

    /**
     * @return the text input used to specify the search keywords
     */
    public WebElement getSearchInput()
    {
        return searchInput;
    }

    /**
     * @return the list box used to select the extension repository to search into
     */
    public Select getRepositorySelect()
    {
        return new Select(repositorySelect);
    }

    /**
     * Selects the specified extension repository and waits for the search results to update.
     * 
     * @param repositoryId the repository identifier
     * @return the search results pane
     */
    public SearchResultsPane selectRepository(String repositoryId)
    {
        getRepositorySelect().selectByValue(repositoryId);
        new BasePage().waitUntilPageIsLoaded();
        return new SearchResultsPane();
    }

    /**
     * Clicks on the 'Advanced search' link to open the advanced search pane.
     * 
     * @return the advanced search form
     */
    public AdvancedSearchPane clickAdvancedSearch()
    {
        advancedSearchLink.click();
        return new AdvancedSearchPane();
    }

    /**
     * Searches for the extensions matching the given keywords.
     * 
     * @param keywords the keywords to search for
     * @return the search results pane
     */
    public SearchResultsPane search(CharSequence keywords)
    {
        searchInput.clear();

        // FIXME: workaround for https://github.com/SeleniumHQ/selenium/issues/7691
        // Since sendKeys is not waiting anymore and bulletproof it
        getDriver().addPageNotYetReloadedMarker();
        searchInput.sendKeys(keywords, Keys.ENTER);
        getDriver().waitUntilPageIsReloaded();

        return new SearchResultsPane();
    }
}
