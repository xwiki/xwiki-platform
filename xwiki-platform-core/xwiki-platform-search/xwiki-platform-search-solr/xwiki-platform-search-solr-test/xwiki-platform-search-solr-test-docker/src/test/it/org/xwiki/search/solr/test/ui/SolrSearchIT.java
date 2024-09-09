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
package org.xwiki.search.solr.test.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.LocalizationAdministrationSectionPage;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.search.solr.test.po.SolrSearchPage;
import org.xwiki.search.solr.test.po.SolrSearchResult;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;

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
    @Test
    void verifySpaceFaucetEscaping(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();

        String testDocumentLocation = "{{/html}}";
        setup.createPage(testDocumentLocation, "WebHome", "Test Document", testDocumentLocation);

        new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmptyQueue();

        SolrSearchPage searchPage = SolrSearchPage.gotoPage();
        searchPage.search("\"Test Document\"");
        searchPage.toggleSpaceFaucet();
        assertEquals(testDocumentLocation + "\n1", searchPage.getSpaceFaucetContent());
    }

    private String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
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
            String testContent = String.format("Unique%sContent", locale);
            setup.deletePage(testReference);
            setup.createPage(testReference, testContent, locale);

            new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmptyQueue();

            SolrSearchPage searchPage = SolrSearchPage.gotoPage();
            searchPage.search(testContent);
            List<SolrSearchResult> searchResults = searchPage.getSearchResults();
            assertEquals(1, searchResults.size());
            SolrSearchResult searchResult = searchResults.get(0);
            assertEquals(locale, searchResult.getTitle());
            Map<String, List<String>> highlights = searchResult.getHighlights();
            assertTrue(highlights.values().stream().flatMap(List::stream).anyMatch(testContent::equals),
                String.format("The snippets [%s] don't contain the expected content [%s]",
                    highlights.entrySet()
                        .stream()
                        .map(entry -> String.format("%s: %s", entry.getKey(), String.join(", ", entry.getValue())))
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
}
