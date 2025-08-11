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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.search.test.po.SolrSearchPage;
import org.xwiki.search.test.po.SolrSearchResult;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Solr search features.
 *
 * @version $Id$
 */
@UITest
class SolrSearchIT
{
    private static final Pattern EXPECTED_ERROR_CODE =
        Pattern.compile(".*HTTP\\s(ERROR|Status)\\s400.*", Pattern.DOTALL);
    private static final String EXPECTED_ERROR =
        "Invalid parameter value 2000 for %s: Must be a positive integer and less than or equal to 1000";

    private static final String NB_PARAMETER = "nb";

    private static final String GET_ACTION = "get";

    @Test
    void verifySpaceFaucetEscaping(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        String testDocumentLocation = "{{/html}}";
        setup.createPage(testDocumentLocation, "WebHome", "Test Document", testDocumentLocation);

        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        SolrSearchPage searchPage = SolrSearchPage.gotoPage();
        searchPage.search("\"Test Document\"");
        searchPage.toggleSpaceFaucet();
        assertEquals(testDocumentLocation + "\n1", searchPage.getSpaceFaucetContent());
    }

    @ParameterizedTest
    @ValueSource(strings = { "de_DE", "fr_FR", "fr_BE" })
    void verifySearchInLocale(String locale, TestUtils setup, TestConfiguration testConfiguration,
        TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        AdministrationPage adminPage = AdministrationPage.gotoPage();
        LocalizationAdministrationSectionPage sectionPage = adminPage.clickLocalizationSection();
        sectionPage.setDefaultLanguage(locale);
        sectionPage.clickSave();

        try {
            String testContent = "Unique%sContent".formatted(locale);
            setup.deletePage(testReference);
            setup.createPage(testReference, testContent, locale);

            new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

            SolrSearchPage searchPage = SolrSearchPage.gotoPage();
            searchPage.search(testContent);
            List<SolrSearchResult> searchResults = searchPage.getSearchResults();
            assertEquals(1, searchResults.size());
            SolrSearchResult searchResult = searchResults.get(0);
            assertEquals(locale, searchResult.getTitle());
            Map<String, List<String>> highlights = searchResult.getHighlights();
            assertTrue(highlights.values().stream().flatMap(List::stream).anyMatch(testContent::equals),
                "The snippets [%s] don't contain the expected content [%s]".formatted(
                    highlights.entrySet()
                        .stream()
                        .map(entry -> "%s: %s".formatted(entry.getKey(), String.join(", ", entry.getValue())))
                        .collect(Collectors.joining("\n")),
                    testContent));
        } finally {
            // Reset the locale to be English.
            adminPage = AdministrationPage.gotoPage();
            sectionPage = adminPage.clickLocalizationSection();
            sectionPage.setDefaultLanguage("en");
            sectionPage.clickSave();
        }
    }

    @Test
    void searchLimit(TestUtils setup, TestConfiguration testConfiguration, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        setup.rest().savePage(testReference, "TestSearchLimit", "Test Search Limit Page");

        // Create 20 pages.
        for (int i = 0; i < 20; i++) {
            DocumentReference pageReference = new DocumentReference("Page" + i, testReference.getLastSpaceReference());
            setup.rest().savePage(pageReference, "Content of Page " + i, "Title " + i);
        }

        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        SolrSearchPage searchPage = SolrSearchPage.gotoPage();
        searchPage.search("Content of Page");
        assertEquals(10, searchPage.getSearchResults().size(), "The search should return only 10 results by default.");

        searchPage = searchPage.setResultsPerPage(20, true);
        assertEquals(20, searchPage.getSearchResults().size(),
            "The search should return 20 results when the results per page is set to 20.");

        searchPage = searchPage.setResultsPerPage(2000, false);
        String pageSource = setup.getDriver().getPageSource();
        String formattedExpectedError = EXPECTED_ERROR.formatted("rows");
        // Depending on the servlet engine the error might be in the title or in the page body.
        assertThat(pageSource, matchesRegex(EXPECTED_ERROR_CODE));
        assertThat(pageSource, containsString(formattedExpectedError));
    }

    @Test
    void suggestService(TestUtils setup, TestConfiguration testConfiguration, TestReference testReference)
        throws Exception
    {
        setup.loginAsSuperAdmin();

        String testDocumentTitle = "SuggestServiceTestTitle";
        setup.rest().savePage(testReference, "Hello World!", testDocumentTitle);

        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("outputSyntax", "plain");
        parameters.put("query", "__INPUT__");
        parameters.put("input", testDocumentTitle);

        DocumentReference suggestSolrService = new DocumentReference("xwiki", "XWiki", "SuggestSolrService");
        setup.gotoPage(suggestSolrService, GET_ACTION, parameters);

        String pageSource = setup.getDriver().getPageSource();
        assertThat(pageSource, containsString(testDocumentTitle));

        parameters.put(NB_PARAMETER, "2000");
        setup.gotoPage(suggestSolrService, GET_ACTION, parameters);

        pageSource = setup.getDriver().getPageSource();
        String formattedExpectedError = EXPECTED_ERROR.formatted(NB_PARAMETER);
        // Depending on the servlet engine the error might be in the title or in the page body.
        assertThat(pageSource, matchesRegex(EXPECTED_ERROR_CODE));
        assertThat(pageSource, containsString(formattedExpectedError));
    }
}
