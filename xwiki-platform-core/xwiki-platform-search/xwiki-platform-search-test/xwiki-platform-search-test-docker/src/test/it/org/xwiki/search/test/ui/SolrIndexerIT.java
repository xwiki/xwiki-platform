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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.search.solr.internal.job.DiffDocumentIterator;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Solr indexer, verifying re-indexing works as expected.
 *
 * @version $Id$
 */
@UITest(properties = {
    // Exclude the Groovy script below from the PR checker.
    // Increase the indexer batch size to speed up indexing.
    """
        xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*Test\\.Execute\\..*
        solr.indexer.batch.maxLength=10000000
        solr.indexer.batch.size=200"""
})
class SolrIndexerIT
{
    private static final String TEST_SCRIPT = """
        {{groovy}}
        import java.lang.reflect.ParameterizedType
        
        import org.apache.commons.lang3.tuple.Pair
        import org.xwiki.component.util.DefaultParameterizedType
        import org.xwiki.model.reference.DocumentReference
        import org.xwiki.search.solr.internal.job.DatabaseDocumentIterator
        import org.xwiki.search.solr.internal.job.DiffDocumentIterator
        import org.xwiki.search.solr.internal.job.DocumentIterator
        import org.xwiki.velocity.tools.JSONTool
        
        if (xcontext.action == "get") {
            ParameterizedType documentIterator =
                new DefaultParameterizedType(null, DocumentIterator.class, String.class)
            DocumentIterator<String> databaseIterator = services.component.getInstance(documentIterator, "database")
            DocumentIterator<String> solrIterator = services.component.getInstance(documentIterator, "solr")
        
            // Store both iterators converted to list for the output.
            def outputData = [
                 "database": toList(databaseIterator),
                 "solr": toList(solrIterator)
             ]
        
            // Re-create both iterators and use a diff iterator to verify that the diff computation works.
            databaseIterator = services.component.getInstance(documentIterator, "database")
            solrIterator = services.component.getInstance(documentIterator, "solr")
        
            DocumentIterator<DiffDocumentIterator.Action> diffIterator =
                new DiffDocumentIterator(solrIterator, databaseIterator)
            outputData.put("diff", toList(diffIterator))
        
            // Set the content type to text/plain to not trigger the JSON UI of the browser.
            response.setContentType("text/plain")
            response.setCharacterEncoding("UTF-8")
            def output = (new JSONTool()).serialize(outputData)
        
            response.writer.print(output)
            response.setContentLength(output.getBytes("UTF-8").size())
            response.flushBuffer()
            xcontext.setFinished(true)
        }
        
        // Method to convert an iterator of pairs to a list of two-element lists.
        static def toList(Iterator<Pair<DocumentReference, ?>> iterator) {
            def list = []
            Pair<DocumentReference, ?> previous = null
            def comparator = DiffDocumentIterator.getComparator()
            while (iterator.hasNext()) {
                def pair = iterator.next()
                int comparison = -1
                if (previous != null) {
                    comparison = comparator.compare(previous.getLeft(), pair.getLeft())
                }
                previous = pair
                list.add([pair.getLeft().toString(), pair.getRight().toString(), String.valueOf(comparison)])
            }
            return list
        }
        // {{/groovy}}
        """;

    private static final String WEB_HOME = "WebHome";

    @Test
    void sortOrder(TestReference testReference, TestUtils testUtils, TestConfiguration testConfiguration)
        throws Exception
    {
        testUtils.loginAsSuperAdmin();

        SpaceReference testSpace = testReference.getLastSpaceReference();

        // Create a couple of pages with interesting names that could have different ordering in Unicode.
        List<String> names =
            List.of("Abc", "Bac", "bAc", "aBc", "Äbc", "äBc", "bÄc", "Bäc", "Àbc", "àBc", "bÀc", "Bàc",
                "\uD83D\uDC4Dbc");

        for (String name : names) {
            DocumentReference pageReference = new DocumentReference(name, testSpace);
            testUtils.rest().savePage(pageReference, "Terminal page content", name);
            SpaceReference spaceReference = new SpaceReference(name, testSpace);
            DocumentReference nonTerminalPageReference = new DocumentReference(WEB_HOME, spaceReference);
            testUtils.rest().savePage(nonTerminalPageReference, "Non-terminal page content", name);
        }

        // Test confusion between space separator and similar strings inside the space.
        testUtils.rest().savePage(new DocumentReference(WEB_HOME, new SpaceReference("A.b", testSpace)));
        testUtils.rest().savePage(new DocumentReference(WEB_HOME,
            new SpaceReference("b", new SpaceReference("A", testSpace))));
        testUtils.rest().savePage(new DocumentReference(WEB_HOME, new SpaceReference("A-b", testSpace)));
        testUtils.rest().savePage(new DocumentReference(WEB_HOME, new SpaceReference("AAb", testSpace)));

        new SolrTestUtils(testUtils, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Get the output from the test script.
        String jsonContent = testUtils.executeWiki(TEST_SCRIPT, Syntax.XWIKI_2_1);
        assertThat(jsonContent, startsWith("{"));

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, List<List<String>>> iteratorOutput = objectMapper.readValue(jsonContent,
            new TypeReference<>()
            {
            });

        String skipAction = DiffDocumentIterator.Action.SKIP.toString();
        List<List<String>> nonSkipOperations = iteratorOutput.get("diff").stream()
            .filter(item -> !skipAction.equals(item.get(1)))
            .toList();
        List<List<String>> databaseDocuments = iteratorOutput.get("database");
        List<List<String>> solrDocuments = iteratorOutput.get("solr");

        assertTrue(nonSkipOperations.isEmpty(), """
            The list of operations contains non-skip operations: %s.
            Documents in Solr: %s
            Documents in the database: %s
            """.formatted(nonSkipOperations, solrDocuments, databaseDocuments)
        );

        // Just to be sure, also explicitly compare the two lists of documents even though the diff iterator should
        // already have done that job.
        assertEquals(solrDocuments, databaseDocuments);

        List<List<String>> wronglyOrderedItems =
            solrDocuments.stream().filter(item -> Integer.parseInt(item.get(2)) >= 0).toList();
        assertTrue(wronglyOrderedItems.isEmpty(), """
            Some documents aren't "larger" than the documents before them: %s
            All documents: %s
            """.formatted(wronglyOrderedItems, solrDocuments)
        );
    }
}
