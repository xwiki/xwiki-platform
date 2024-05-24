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

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.search.test.po.QuickSearchElement;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;

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
    void verifySearchSuggestTitles(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.loginAsSuperAdmin();

        String testDocumentLocation = "Main";
        setup.rest().savePage(testReference, "Hello World!", testDocumentLocation);
        setup.gotoPage(testReference);

        new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmptyQueue();

        QuickSearchElement quickSearchElement = new QuickSearchElement();
        quickSearchElement.search(testDocumentLocation);
        WebElement firstSuggestElement = quickSearchElement.getSuggestItemTitles(0);
        assertEquals(testDocumentLocation, firstSuggestElement.getText());
    }

    private String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
    }
}
