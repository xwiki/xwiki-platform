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
package org.xwiki.repository.test;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.TestUtils;

/**
 * @version $Id$
 * @since 7.0RC1
 */
public class SolrTestUtils
{
    public static final String PROPERTY_KEY = "solrutils";

    private static final String SOLRSERVICE_SPACE = "TestService";

    private static final String SOLRSERVICE_PAGE = "Solr";

    private static final LocalDocumentReference SOLRSERVICE_REFERENCE =
        new LocalDocumentReference(SOLRSERVICE_SPACE, SOLRSERVICE_PAGE);

    private final TestUtils testUtils;

    public SolrTestUtils(TestUtils testUtils) throws Exception
    {
        this.testUtils = testUtils;

        initService();
    }

    private void initService() throws Exception
    {
        if (!this.testUtils.pageExists(SOLRSERVICE_SPACE, SOLRSERVICE_PAGE)) {
            // Create the utility page.
            this.testUtils.rest().savePage(SOLRSERVICE_REFERENCE, "{{velocity}}$services.solr.queueSize{{/velocity}}",
                null);
        }
    }

    /**
     * Wait until the Solr does not have anything left to index.
     */
    public void waitEmpyQueue() throws Exception
    {
        while (getSolrQueueSize() > 0) {
            Thread.sleep(100);
        }
    }

    public long getSolrQueueSize() throws Exception
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("outputSyntax", "plain");

        return Long
            .valueOf(this.testUtils.getString("/bin/get/" + SOLRSERVICE_SPACE + '/' + SOLRSERVICE_PAGE, parameters));
    }
}
