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

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the Live Data macro with the {@code solr} source.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest(extraJARs = {
    // The "solr" query language (used by the Solr live data source) is only reliable when part of the WAR.
    "org.xwiki.platform:xwiki-platform-search-solr-query",
    // The Solr live data source connector itself (not part of the default distribution).
    "org.xwiki.platform:xwiki-platform-livedata-solr"
})
class SolrLiveDataIT
{
    private static final String TITLE_COLUMN = "doc.title";

    // A made-up token, unlikely to collide with other indexed documents, shared by the test page titles.
    private static final String TOKEN = "Zorblax";

    private static final String APPLE_TITLE = TOKEN + " Apple";

    private static final String BANANA_TITLE = TOKEN + " Banana";

    // A distinct token for the rights test, so it does not match the documents created by the other test (the Solr
    // query is not scoped to the test space).
    private static final String RIGHTS_TOKEN = "Qwizzle";

    private static final String PUBLIC_TITLE = RIGHTS_TOKEN + " Public";

    private static final String RESTRICTED_TITLE = RIGHTS_TOKEN + " Restricted";

    @Test
    void solrSourceListsMatchingDocuments(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        SpaceReference spaceReference = testReference.getLastSpaceReference();
        // Start from a clean test space.
        setup.deletePage(testReference, true);

        DocumentReference applePage = new DocumentReference("ApplePage", spaceReference);
        DocumentReference bananaPage = new DocumentReference("BananaPage", spaceReference);
        setup.createPage(applePage, "Apple content", APPLE_TITLE);
        setup.createPage(bananaPage, "Banana content", BANANA_TITLE);

        // Wait for the two pages to be indexed in Solr before querying them.
        new SolrTestUtils(setup).waitEmptyQueue();

        // The shared token matches both test pages (the title field is tokenized).
        setup.createPage(testReference, "{{liveData id=\"test\" properties=\"doc.title\" source=\"solr\""
            + " sourceParameters=\"query=title_:" + TOKEN + "\"/}}", "Solr Live Data");

        setup.gotoPage(testReference);
        LiveDataElement liveData = new LiveDataElement("test");
        liveData.waitUntilReady();
        TableLayoutElement tableLayout = liveData.getTableLayout();
        assertEquals(2, tableLayout.countRows());
        tableLayout.assertCellWithLink(TITLE_COLUMN, APPLE_TITLE, setup.getURL(applePage, "view", ""));
        tableLayout.assertCellWithLink(TITLE_COLUMN, BANANA_TITLE, setup.getURL(bananaPage, "view", ""));

        // Filtering on the title column narrows the result down to the matching page.
        tableLayout.filterColumn(TITLE_COLUMN, "Apple", true);
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow(TITLE_COLUMN, APPLE_TITLE);
    }

    @Test
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
            + " sourceParameters=\"query=title_:" + RIGHTS_TOKEN + "\"/}}", "Solr Live Data");

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
