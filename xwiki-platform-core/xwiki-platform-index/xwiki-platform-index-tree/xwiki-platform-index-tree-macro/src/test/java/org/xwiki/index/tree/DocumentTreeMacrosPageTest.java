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
package org.xwiki.index.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test for the Velocity macros of {@code XWiki.DocumentTreeMacros}.
 *
 * @version $Id$
 */
@ComponentList(ModelScriptService.class)
@HTML50ComponentList
@XWikiSyntax21ComponentList
class DocumentTreeMacrosPageTest extends PageTest
{
    private static final String WIKI = "xwiki";

    private static final String PARAM_LIST = "paramList";

    private final List<Query> executedQueries = new ArrayList<>();

    /**
     * Stands for {@code $services.query.solr}, which is provided by the Solr modules, to check which values are quoted.
     */
    public static final class SolrQueryScriptServiceStub implements ScriptService
    {
        /**
         * @param value the value to quote
         * @return a marker of the quoted value
         */
        public String quote(String value)
        {
            return "quoted(" + value + ")";
        }
    }

    @BeforeEach
    void setUp() throws Exception
    {
        loadPage(new DocumentReference(WIKI, "XWiki", "DocumentTreeMacros"));

        // XWiki.SuggestSolrMacros is provided by another module, which parses the parameters. Only record the
        // parameters that it receives.
        XWikiDocument suggestSolrMacros =
            this.xwiki.getDocument(new DocumentReference(WIKI, "XWiki", "SuggestSolrMacros"), this.context);
        suggestSolrMacros.setSyntax(Syntax.XWIKI_2_1);
        suggestSolrMacros.setContent("""
            {{velocity output="false"}}
            #macro (createSearchSuggestQueryFromParameterList $paramList $input $query)
              #set ($suggestQuery = $services.query.createQuery($input, 'solr'))
              #set ($discard = $suggestQuery.bindValue('%s', $paramList))
              #set ($query = $NULL)
              #setVariable("$query" $suggestQuery)
            #end
            {{/velocity}}
            """.formatted(PARAM_LIST));
        this.xwiki.saveDocument(suggestSolrMacros, this.context);

        QueryExecutor queryExecutor = mock();
        when(queryExecutor.execute(any())).then(invocation -> {
            this.executedQueries.add(invocation.getArgument(0));
            return List.of(Map.of("results", List.of()));
        });

        QueryManagerScriptService queryManagerScriptService = mock();
        when(queryManagerScriptService.createQuery(anyString(), eq("solr"))).then(
            invocation -> new ScriptQuery(new DefaultQuery(invocation.getArgument(0), "solr", queryExecutor),
                this.componentManager));
        when(queryManagerScriptService.get("solr")).thenReturn(new SolrQueryScriptServiceStub());
        this.componentManager.registerComponent(ScriptService.class, "query", queryManagerScriptService);
    }

    @Test
    void searchDocumentsSolrWithSpecialCharactersInRoot() throws Exception
    {
        renderMacroCall("""
            #set ($docTreeConfig = {
              'showTerminalDocuments': true,
              'filterHiddenDocuments': true,
              'showSpaces': true,
              'hierarchyMode': 'reference'
            })
            #set ($rootType = 'document')
            #set ($rootReference = $services.model.createDocumentReference('xwiki', 'Some "Space"',
              "Name ""with"" a${util.newline}line break"))
            #searchDocumentsSolr('text' 5 $results)
            """);

        assertEquals(1, this.executedQueries.size());
        Query query = this.executedQueries.get(0);
        assertEquals("text", query.getStatement());
        assertEquals(5, query.getLimit());
        List<String> paramList = getParamList(query);
        assertTrue(paramList.contains("q=__INPUT__ __INPUT__*"), paramList.toString());
        // The parameters are passed as a list so that the line break in the document name is kept in the value.
        assertEquals(List.of(
            "fq=type:DOCUMENT",
            "fq=doclocale:\"\"",
            "fq=wiki:quoted(xwiki)",
            "fq=space_exact:quoted(Some \"Space\")",
            "fq=name_exact:quoted(Name \"with\" a\nline break)"), getFilterQueries(paramList));
    }

    @Test
    void searchAttachmentsSolrInSpace() throws Exception
    {
        renderMacroCall("""
            #set ($docTreeConfig = {
              'showTerminalDocuments': false,
              'filterHiddenDocuments': false,
              'showSpaces': true,
              'hierarchyMode': 'reference'
            })
            #set ($rootType = 'space')
            #set ($wikiReference = $services.model.createWikiReference('xwiki'))
            #set ($rootReference = $services.model.createSpaceReference('Space\\Name', $wikiReference))
            #searchAttachmentsSolr('text' 3 $results)
            """);

        assertEquals(1, this.executedQueries.size());
        List<String> filterQueries = getFilterQueries(getParamList(this.executedQueries.get(0)));
        assertTrue(filterQueries.containsAll(List.of(
            "fq=type:ATTACHMENT",
            "fq=wiki:quoted(xwiki)",
            "fq=space_prefix:quoted(Space\\\\Name)",
            "fq=name:WebHome",
            "fq=hidden:*")), filterQueries.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> getParamList(Query query)
    {
        return (List<String>) query.getNamedParameters().get(PARAM_LIST);
    }

    private List<String> getFilterQueries(List<String> paramList)
    {
        return paramList.stream().filter(param -> param.startsWith("fq=")).toList();
    }

    private void renderMacroCall(String velocityCode) throws Exception
    {
        XWikiDocument testPage = this.xwiki.getDocument(new DocumentReference(WIKI, "Test", "Page"), this.context);
        testPage.setSyntax(Syntax.XWIKI_2_1);
        testPage.setContent("""
            {{include reference="XWiki.DocumentTreeMacros" /}}

            {{velocity}}
            %s
            {{/velocity}}
            """.formatted(velocityCode));
        this.xwiki.saveDocument(testPage, this.context);

        renderHTMLPage(testPage);
    }
}
