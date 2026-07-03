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

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object for the Solr search page.
 *
 * @since 14.10.20
 * @since 15.10RC1
 * @since 15.5.4
 * @version $Id$
 */
public class SolrSearchPage extends ViewPage
{
    private static final String ROWS_PARAM = "rows=";

    private static final String SPACE_FACET_DROPDOWN_ID = "space_facet-dropdown";

    @FindBy(id = "search-page-bar-input")
    private WebElement searchInput;

    @FindBy(xpath = "//div[@class = 'search-ui']//button[@type = 'submit']")
    private WebElement searchButton;

    @FindBy(xpath = "//div[@class = 'search-ui']//button[@aria-controls = 'space_facet-dropdown']")
    private WebElement spaceFacetDropdownButton;

    @FindBy(id = SPACE_FACET_DROPDOWN_ID)
    private WebElement spaceFacetDropdownContent;

    @FindBy(css = "div.search-results > div.search-result")
    private List<WebElement> searchResultElements;

    /**
     * Opens the Solr search page.
     *
     * @return the Solr search page
     */
    public static SolrSearchPage gotoPage()
    {
        getUtil().gotoPage("Main", "SolrSearch", "view");
        return new SolrSearchPage();
    }

    /**
     * Searches for the given terms.
     *
     * @param terms the terms to search for
     * @return the (reloaded) Solr search page
     */
    public SolrSearchPage search(String terms)
    {
        this.searchInput.clear();
        this.searchInput.sendKeys(terms);
        this.searchButton.click();
        return new SolrSearchPage();
    }

    /**
     * Toggles the space facet.
     *
     * @since 17.10.3
     * @since 18.1.0RC1
     */
    public void toggleSpaceFacet()
    {
        this.spaceFacetDropdownButton.click();

        // The facet uses bootstrap collapse to expand/collapse.
        // Wait for the animation to complete by waiting for the "collapsing" class to be removed.
        getDriver().waitUntilCondition(
            driver -> !Strings.CS.contains(this.spaceFacetDropdownContent.getAttribute("class"), "collapsing"));
    }

    /**
     * @return the content of the space facet
     *
     * @since 17.10.3
     * @since 18.1.0RC1
     */
    public String getSpaceFacetContent()
    {
        return this.spaceFacetDropdownContent.getText();
    }

    /**
     * @return the search results that are displayed on the current page
     */
    public List<SolrSearchResult> getSearchResults()
    {
        return this.searchResultElements.stream()
            .map(SolrSearchResult::new)
            .toList();
    }

    /**
     * Sets the number of results to display per page by directly modifying the URL.
     *
     * @param resultsPerPage the number of results to display per page
     * @param wait if true, waits for the page to reload after setting the results per page
     * @return the reloaded page, or the current page if wait is false
     * @since 17.7.0RC1
     * @since 17.4.4
     * @since 16.10.11
     */
    public SolrSearchPage setResultsPerPage(int resultsPerPage, boolean wait)
    {
        String currentUrl = getDriver().getCurrentUrl();
        // Setting the number of results per page isn't supported in the UI, so we modify the URL directly.
        // The results per page parameter is called "rows" in Solr.
        String url;
        if (Strings.CS.contains(currentUrl, ROWS_PARAM)) {
            url = RegExUtils.replaceAll(currentUrl, "rows=\\d+", ROWS_PARAM + resultsPerPage);
        } else {
            url = currentUrl + (StringUtils.contains(currentUrl, '?') ? "&" : "?") + ROWS_PARAM + resultsPerPage;
        }

        getUtil().gotoPage(url);

        // Construct a new page to wait for the page to load.
        if (wait) {
            return new SolrSearchPage();
        } else {
            // Return the current page without waiting.
            return this;
        }
    }
}
