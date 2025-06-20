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
package org.xwiki.search.test.ui;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.search.test.po.QuickSearchElement;
import org.xwiki.search.test.po.SearchSuggestAdministrationPage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UI tests for Search Suggest features.
 *
 * @version $Id$
 */
@UITest
class SearchSuggestIT
{
    @Test
    @Order(1)
    void verifySearchSuggestTitles(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.loginAsSuperAdmin();

        String testDocumentTitle = "Main";
        setup.rest().savePage(testReference, "Hello World!", testDocumentTitle);
        setup.gotoPage(testReference);

        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        QuickSearchElement quickSearchElement = new QuickSearchElement();
        quickSearchElement.search(testDocumentTitle);
        WebElement firstSuggestElement = quickSearchElement.getSuggestItemTitles(0);
        assertEquals(testDocumentTitle, firstSuggestElement.getText());
    }

    /**
     * Note: must be the last test since it de-activates the search suggest.
     */
    @Test
    @Order(2)
    void verifyDisablingSearchSuggest(TestUtils setup, TestReference testReference)
    {
        // Navigate to any page and verify that the page source loads the search suggest script.
        // Note: we test positives and negative outcomes below to make sure that the test is correct. Without this,
        // we could never be sure that if some content is not found in the source it's because XWiki didn't put it
        // (vs an error in the test itself).
        ViewPage vp = setup.gotoPage(testReference);
        Pattern expected = Pattern.compile(".*searchSuggest\\.min\\.js.*", Pattern.DOTALL);
        assertThat(setup.getDriver().getPageSource(), matchesPattern(expected));

        // Disable the search suggest
        SearchSuggestAdministrationPage ssaPage = SearchSuggestAdministrationPage.gotoPage();
        ssaPage.setActivated(false);
        ssaPage.clickSave();

        // Navigate to any page and verify that the page source doesn't load the search suggest script.
        // We could also wait for the search suggest modal to not appear (it would be closer to what a user would do)
        // but we would need to wait for a long timeout and that would slow down the test.
        vp = setup.gotoPage(testReference);
        assertThat(setup.getDriver().getPageSource(), not(matchesPattern(expected)));
    }
}
