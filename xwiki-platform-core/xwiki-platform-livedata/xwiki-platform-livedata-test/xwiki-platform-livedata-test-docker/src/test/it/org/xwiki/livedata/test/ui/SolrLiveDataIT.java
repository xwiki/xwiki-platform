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
package org.xwiki.livedata.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the Live Data macro with the {@code solr} source.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@UITest(extraJARs = {
    // The "solr" query language (used by the Solr live data source) is only reliable when part of the WAR.
    // The Solr live data source connector itself is not added here: it is installed as a regular extension by the
    // docker test framework (it is declared as a runtime dependency of this test module's POM).
    "org.xwiki.platform:xwiki-platform-search-solr-query"
})
class SolrLiveDataIT
{
    // The columns are referenced by their *translated* header label (what the widget displays), not by the property
    // id: the Solr source localizes the headers from the "translationPrefix" source parameter, and these are the
    // labels the existing "platform.index.doc.*" translation keys resolve to.
    private static final String TITLE_COLUMN = "Title";

    private static final String PAGE_COLUMN = "Page";

    private static final String LAST_AUTHOR_COLUMN = "Last Author";

    private static final String DATE_COLUMN = "Date";

    private static final String LOCATION_COLUMN = "Location";

    // A property the Solr source does not support: it must be ignored gracefully (empty column, no JS error), not
    // crash the live data widget. Its header is not localized, so it falls back to the property id.
    private static final String UNSUPPORTED_COLUMN = "doc.bogus";

    // The translation prefix passed to the Solr source so that the column headers are localized. The matching
    // "platform.index.doc.*" keys are already bundled in the WAR (the document index uses the same prefix).
    private static final String TRANSLATION_PREFIX = "platform.index.";

    // The page size used by the main scenario. Six documents are created (see below), so they span two pages and the
    // pagination can be exercised.
    private static final int PAGE_SIZE = 5;

    // A made-up token, unlikely to collide with other indexed documents, shared by the six test page titles. Their
    // distinct (alphabetically ordered) fruit names make the row order deterministic once sorted by title.
    private static final String TOKEN = "Zorblax";

    private static final String APPLE_TITLE = TOKEN + " Apple";

    private static final String BANANA_TITLE = TOKEN + " Banana";

    private static final String CHERRY_TITLE = TOKEN + " Cherry";

    private static final String DRAGONFRUIT_TITLE = TOKEN + " Dragonfruit";

    private static final String ELDERBERRY_TITLE = TOKEN + " Elderberry";

    private static final String FIG_TITLE = TOKEN + " Fig";

    // A distinct token for the rights test, so it does not match the documents created by the other test (the Solr
    // query is not scoped to the test space).
    private static final String RIGHTS_TOKEN = "Qwizzle";

    private static final String PUBLIC_TITLE = RIGHTS_TOKEN + " Public";

    private static final String RESTRICTED_TITLE = RIGHTS_TOKEN + " Restricted";

    @Test
    @Order(1)
    void solrSourceListsMatchingDocuments(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        SpaceReference spaceReference = testReference.getLastSpaceReference();
        // Start from a clean test space.
        setup.deletePage(testReference, true);

        // Create six documents sharing the token so that, with a page size of 5, they span two pages (pagination).
        // The distinct, alphabetically ordered fruit titles make the row order deterministic once sorted by title.
        DocumentReference applePage = new DocumentReference("ApplePage", spaceReference);
        DocumentReference bananaPage = new DocumentReference("BananaPage", spaceReference);
        setup.createPage(applePage, "Apple content", APPLE_TITLE);
        setup.createPage(bananaPage, "Banana content", BANANA_TITLE);
        setup.createPage(new DocumentReference("CherryPage", spaceReference), "Cherry content", CHERRY_TITLE);
        setup.createPage(new DocumentReference("DragonfruitPage", spaceReference), "Dragonfruit content",
            DRAGONFRUIT_TITLE);
        setup.createPage(new DocumentReference("ElderberryPage", spaceReference), "Elderberry content",
            ELDERBERRY_TITLE);
        setup.createPage(new DocumentReference("FigPage", spaceReference), "Fig content", FIG_TITLE);

        // Wait for the pages to be indexed in Solr before querying them.
        new SolrTestUtils(setup).waitEmptyQueue();

        // The shared token matches all six test pages (the title field is tokenized). The "type" parameter is set
        // explicitly to its default to exercise the entity-type extension point. Several columns of different kinds
        // (title/full name links, location breadcrumb, author link, formatted date) plus an unsupported column are
        // requested, a translation prefix is passed so the headers are localized, and a page size of 5 (the "limit"
        // macro parameter) is set so the six results span two pages.
        setup.createPage(testReference,
            "{{liveData id=\"test\""
                + " properties=\"doc.title,doc.location,doc.fullName,doc.author,doc.date," + UNSUPPORTED_COLUMN + "\""
                + " limit=\"" + PAGE_SIZE + "\""
                + " source=\"solr\" sourceParameters=\"type=document&query=title_:" + TOKEN + "&translationPrefix="
                + TRANSLATION_PREFIX + "\"/}}", "Solr Live Data");

        setup.gotoPage(testReference);
        LiveDataElement liveData = new LiveDataElement("test");
        liveData.waitUntilReady();
        TableLayoutElement tableLayout = liveData.getTableLayout();
        // Sort by title so that the order of the rows (and thus their distribution across the pages) is deterministic.
        tableLayout.sortBy(TITLE_COLUMN);

        // Pagination, first page: only the page size (5) rows are rendered, but the total count reflects all six
        // matches. The Solr source caps the result with setLimit while reporting numFound as the count, independently
        // of the page size.
        assertEquals(6, tableLayout.getTotalEntries());
        assertEquals(PAGE_SIZE, tableLayout.countRows());

        // The title column is rendered as a link to the document (under its localized "Title" header). Apple and
        // Banana sort first, so they are on this first page.
        tableLayout.assertCellWithLink(TITLE_COLUMN, APPLE_TITLE, setup.getURL(applePage, "view", ""));
        tableLayout.assertCellWithLink(TITLE_COLUMN, BANANA_TITLE, setup.getURL(bananaPage, "view", ""));
        // The other requested columns are rendered under their localized headers (looking the cells up by the
        // translated label both checks the columns are present and proves the headers are localized).
        assertEquals(PAGE_SIZE, tableLayout.getAllCells(PAGE_COLUMN).size());
        assertEquals(PAGE_SIZE, tableLayout.getAllCells(LAST_AUTHOR_COLUMN).size());
        assertEquals(PAGE_SIZE, tableLayout.getAllCells(DATE_COLUMN).size());
        assertEquals(PAGE_SIZE, tableLayout.getAllCells(LOCATION_COLUMN).size());

        // The full name, author and location columns are rendered as links (and not as raw text).
        assertFalse(tableLayout.getAllCells(PAGE_COLUMN).get(0).findElements(By.tagName("a")).isEmpty());
        assertFalse(tableLayout.getAllCells(LAST_AUTHOR_COLUMN).get(0).findElements(By.tagName("a")).isEmpty());
        assertFalse(tableLayout.getAllCells(LOCATION_COLUMN).get(0).findElements(By.tagName("a")).isEmpty());
        // The date is rendered with the wiki date format (e.g. 2026/06/17 12:34:56), not as a raw number.
        assertTrue(tableLayout.getAllCells(DATE_COLUMN).get(0).getText().matches("\\d{4}/\\d{2}/\\d{2}.*"),
            "The date column should display a formatted date");
        // The unsupported column is ignored gracefully: its (empty) column still renders and the table is not broken
        // (a missing descriptor used to crash the widget). It falls back to its property id as a header.
        assertEquals(PAGE_SIZE, tableLayout.getAllCells(UNSUPPORTED_COLUMN).size());

        // Pagination, second page: the remaining (6th) document is shown, proving the source applies the offset
        // (setOffset). The total count is unchanged across pages, and the last document in title order is Fig.
        tableLayout.goToNextPage();
        assertEquals(6, tableLayout.getTotalEntries());
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(TITLE_COLUMN, FIG_TITLE);

        // Filtering on the title column with a partial term narrows the result down to the single matching page (the
        // "contains" operator does a substring match, so "Ap" matches "Apple"). Applying a filter also resets the
        // pagination back to the first page.
        tableLayout.filterColumn(TITLE_COLUMN, "Ap", true);
        assertEquals(1, tableLayout.getTotalEntries());
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(TITLE_COLUMN, APPLE_TITLE);
        // Clear the title filter before exercising the other column filters: back to the full (page-limited) result.
        tableLayout.filterColumn(TITLE_COLUMN, "", true);
        assertEquals(6, tableLayout.getTotalEntries());
        assertEquals(PAGE_SIZE, tableLayout.countRows());

        // The Page (doc.fullName) column filter is case-insensitive on the page name: a lowercase partial term ("ba")
        // matches "BananaPage" (it is filtered against the tokenized "name" field -- as a plain analyzed term plus a
        // substring wildcard -- OR-ed with a substring wildcard on the "fullname" field).
        tableLayout.filterColumn(PAGE_COLUMN, "ba", true);
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(TITLE_COLUMN, BANANA_TITLE);
        tableLayout.filterColumn(PAGE_COLUMN, "", true);
        assertEquals(PAGE_SIZE, tableLayout.countRows());

        // The Date (doc.date) column filter applies a Solr range query: a range covering "now" keeps all six freshly
        // created pages (capped at the page size), while a range entirely in the past excludes them all.
        tableLayout.filterColumn(DATE_COLUMN, "2000/01/01 00:00 - 2100/01/01 00:00", true);
        assertEquals(6, tableLayout.getTotalEntries());
        assertEquals(PAGE_SIZE, tableLayout.countRows());
        tableLayout.filterColumn(DATE_COLUMN, "2000/01/01 00:00 - 2001/01/01 00:00", true);
        assertEquals(0, tableLayout.countRows());
    }

    @Test
    @Order(2)
    void solrSourceExcludesDocumentsTheUserCannotView(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        SpaceReference spaceReference = testReference.getLastSpaceReference();
        // Start from a clean test space.
        setup.deletePage(testReference, true);

        // One page everyone can view and one page hidden from the test viewer, both matching the query token.
        DocumentReference publicPage = new DocumentReference("PublicPage", spaceReference);
        DocumentReference restrictedPage = new DocumentReference("RestrictedPage", spaceReference);
        setup.createPage(publicPage, "Public content", PUBLIC_TITLE);
        setup.createPage(restrictedPage, "Restricted content", RESTRICTED_TITLE);

        // Create a regular (non-admin) user and deny it the right to view the restricted page.
        String viewer = spaceReference.getName() + "Viewer";
        setup.createUser(viewer, viewer, "");
        setup.setRights(restrictedPage, "", viewer, "view", false);

        // The live data page is authored by the superadmin but viewable by everyone.
        setup.createPage(testReference, "{{liveData id=\"test\" properties=\"doc.title\" source=\"solr\""
            + " sourceParameters=\"query=title_:" + RIGHTS_TOKEN + "&translationPrefix=" + TRANSLATION_PREFIX
            + "\"/}}", "Solr Live Data");

        // Wait for the pages (including the rights change) to be indexed in Solr.
        new SolrTestUtils(setup).waitEmptyQueue();

        // As the restricted user, the Solr source must filter out the document the user cannot view: only the public
        // page is listed, even though both documents match the Solr query.
        setup.login(viewer, viewer);
        setup.gotoPage(testReference);
        LiveDataElement liveData = new LiveDataElement("test");
        liveData.waitUntilReady();
        TableLayoutElement tableLayout = liveData.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(TITLE_COLUMN, PUBLIC_TITLE);
    }
}
