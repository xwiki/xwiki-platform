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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the Solr indexer, verifying re-indexing works as expected.
 *
 * @version $Id$
 */
@UITest(properties = {
    // Exclude the Groovy script below from the PR checker.
    // Increase the indexer batch size to speed up indexing.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*Test\\.Execute\\..*\n"
    + "solr.indexer.batch.maxLength=10000000\n"
    + "solr.indexer.batch.size=200"
})
class SolrIndexerIT
{
    private static final String TEST_SCRIPT = "{{groovy}}\n"
        + "import java.lang.reflect.ParameterizedType\n"
        + "\n"
        + "import org.apache.commons.lang3.tuple.Pair\n"
        + "import org.xwiki.component.util.DefaultParameterizedType\n"
        + "import org.xwiki.model.reference.DocumentReference\n"
        + "import org.xwiki.search.solr.internal.job.DatabaseDocumentIterator\n"
        + "import org.xwiki.search.solr.internal.job.DocumentIterator\n"
        + "import org.xwiki.velocity.tools.JSONTool\n"
        + "\n"
        + "if (xcontext.action == \"get\") {\n"
        + "    ParameterizedType documentIterator =\n"
        + "        new DefaultParameterizedType(null, DocumentIterator.class, String.class)\n"
        + "    DocumentIterator<String> databaseIterator = services.component.getInstance(documentIterator, "
                                              + "\"database\")\n"
        + "    DocumentIterator<String> solrIterator = services.component.getInstance(documentIterator, \"solr\")\n"
        + "\n"
        + "     // Convert both iterators to lists and return them as JSON.\n"
        + "     // Set the content type to text/plain to not trigger the JSON UI of the browser.\n"
        + "     response.setContentType(\"text/plain\")\n"
        + "     response.setCharacterEncoding(\"UTF-8\")\n"
        + "     def output = (new JSONTool()).serialize([\n"
        + "         \"database\": toList(databaseIterator),\n"
        + "         \"solr\": toList(solrIterator)\n"
        + "     ])\n"
        + "\n"
        + "     response.writer.print(output)\n"
        + "     response.setContentLength(output.getBytes(\"UTF-8\").size())\n"
        + "     response.flushBuffer()\n"
        + "     xcontext.setFinished(true)\n"
        + " }\n"
        + "\n"
        + "// Method to convert an iterator of pairs to a list of two-element lists.\n"
        + "static def toList(Iterator<Pair<DocumentReference, String>> iterator) {\n"
        + "    def list = []\n"
        + "    while (iterator.hasNext()) {\n"
        + "        def pair = iterator.next()\n"
        + "        list.add([pair.getLeft().toString(), pair.getRight()])\n"
        + "    }\n"
        + "    return list\n"
        + "}\n"
        + "// {{/groovy}}\n";

    @Test
    void sortOrder(TestReference testReference, TestUtils testUtils, TestConfiguration testConfiguration)
        throws Exception
    {
        testUtils.loginAsSuperAdmin();

        SpaceReference testSpace = testReference.getLastSpaceReference();

        // Create a couple of pages with interesting names that could have different ordering in Unicode.
        List<String> names =
            List.of("Abc", "Bac", "bAc", "aBc", "Äbc", "äBc", "bÄc", "Bäc", "Àbc", "àBc", "bÀc", "Bàc");

        for (String name : names) {
            DocumentReference pageReference = new DocumentReference(name, testSpace);
            testUtils.rest().savePage(pageReference, "Terminal page content", name);
            SpaceReference spaceReference = new SpaceReference(name, testSpace);
            DocumentReference nonTerminalPageReference = new DocumentReference("WebHome", spaceReference);
            testUtils.rest().savePage(nonTerminalPageReference, "Non-terminal page content", name);
        }

        new SolrTestUtils(testUtils, computedHostURL(testConfiguration)).waitEmptyQueue();

        // Get the output from the test script.
        String jsonContent = testUtils.executeWiki(TEST_SCRIPT, Syntax.XWIKI_2_1);
        assertThat(jsonContent, startsWith("{"));

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, List<List<String>>> iteratorOutput = objectMapper.readValue(jsonContent,
            new TypeReference<>()
            {
            });

        assertEquals(iteratorOutput.get("solr"), iteratorOutput.get("database"));
    }

    private String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
    }
}
