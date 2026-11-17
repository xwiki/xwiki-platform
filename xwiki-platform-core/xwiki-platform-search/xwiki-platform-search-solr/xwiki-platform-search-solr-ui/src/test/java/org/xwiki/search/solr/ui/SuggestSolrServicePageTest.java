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
package org.xwiki.search.solr.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.query.solr.internal.SafeSolrQueryValidator;
import org.xwiki.query.solr.internal.SolrUntrustedParametersQueryFilter;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.AllUserPropertiesResolver;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code XWiki.SuggestSolrService}.
 *
 * @version $Id$
 */
@ComponentList({
    TemplateMacro.class,
    SafeSolrQueryValidator.class,
    SolrUntrustedParametersQueryFilter.class,
    AllUserPropertiesResolver.class
})
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@DefaultUserComponentList
class SuggestSolrServicePageTest extends PageTest
{
    private static final DocumentReference SUGGEST_SOLR_SERVICE =
        new DocumentReference("xwiki", "XWiki", "SuggestSolrService");

    private static final DocumentReference SUGGEST_SOLR_MACROS =
        new DocumentReference("xwiki", "XWiki", "SuggestSolrMacros");

    private static final String QUERY = "query";

    private static final String INPUT = "input";

    private static final String FQ = "fq";

    private static final String TYPE_DOCUMENT = "type:DOCUMENT";

    private final List<Query> executedQueries = new ArrayList<>();

    private QueryExecutor queryExecutor;

    @BeforeEach
    void setUp() throws Exception
    {
        loadPage(SUGGEST_SOLR_MACROS);

        this.queryExecutor = mock();
        QueryResponse queryResponse = mock();
        when(queryResponse.getResults()).thenReturn(new SolrDocumentList());
        when(this.queryExecutor.execute(any())).then(invocation -> {
            // Apply the query filters, like the Solr query executor does.
            Query query = invocation.getArgument(0);
            for (QueryFilter filter : query.getFilters()) {
                query = filter.filterQuery(query);
            }
            this.executedQueries.add(query);
            return List.of(queryResponse);
        });

        QueryManagerScriptService queryManagerScriptService = mock();
        when(queryManagerScriptService.createQuery(anyString(), eq("solr"))).then(
            invocation -> new ScriptQuery(new DefaultQuery(invocation.getArgument(0), "solr", this.queryExecutor),
                this.componentManager));
        this.componentManager.registerComponent(ScriptService.class, QUERY, queryManagerScriptService);
        QueryFilter searchExclusionsFilter =
            this.componentManager.registerMockComponent(QueryFilter.class, "searchExclusions/solr");
        when(searchExclusionsFilter.filterQuery(any())).then(invocation -> invocation.getArgument(0));

        this.context.setAction("get");
        this.request.put("outputSyntax", "plain");
    }

    @Test
    void supportedParameters() throws Exception
    {
        this.request.put(QUERY, String.join("\n",
            "q=__INPUT__*",
            "fq=type:DOCUMENT",
            "fq=space:\"Main\"",
            "fq=space_exact:(Main OR Sandbox)",
            "fq=+type:DOCUMENT +hidden:false",
            "bq=type:DOCUMENT^2",
            "qf=title^2 name^1.5",
            "pf=title^10",
            "mm=2<-1 5<80%",
            "timeAllowed=500",
            "fl=wiki,spaces,name title_",
            "sort=date desc, name asc"));
        this.request.put(INPUT, "text");

        Query query = executeSuggestSolrService();

        assertEquals("text*", query.getStatement());
        Map<String, Object> parameters = query.getNamedParameters();
        List<?> filterQueries = (List<?>) parameters.get(FQ);
        assertTrue(filterQueries.containsAll(List.of(TYPE_DOCUMENT, "space:\"Main\"", "space_exact:(Main OR Sandbox)",
            "+type:DOCUMENT +hidden:false")));
        assertEquals("title^2 name^1.5", parameters.get("qf"));
        assertEquals("title^10", parameters.get("pf"));
        assertEquals("2<-1 5<80%", parameters.get("mm"));
        assertEquals(List.of("type:DOCUMENT^2"), parameters.get("bq"));
        assertEquals("500", parameters.get("timeAllowed"));
        assertEquals("wiki,spaces,name,title_", parameters.get("fl"));
        assertEquals("date desc,name asc", parameters.get("sort"));
    }

    @Test
    void unsupportedParametersAreIgnored() throws Exception
    {
        this.request.put(QUERY, String.join("\n",
            "fq=type:DOCUMENT",
            "bq={!func}sum(1,2)",
            "bf=sum(1,2)",
            "df=title",
            "rows=50",
            "defType=lucene",
            "facet=true",
            "hl=true"));
        this.request.put(INPUT, "text");

        Query query = executeSuggestSolrService();

        assertEquals("text", query.getStatement());
        assertEquals(Set.of(FQ, "defType"), query.getNamedParameters().keySet());
        assertTrue(((List<?>) query.getNamedParameters().get(FQ)).contains(TYPE_DOCUMENT));
    }

    @Test
    void inputWithSpecialSyntax() throws Exception
    {
        this.request.put(QUERY, "fq=type:DOCUMENT");
        this.request.put(INPUT, "{!text");

        Query query = executeSuggestSolrService();

        // The input is used as plain text.
        assertEquals("{!text", query.getStatement());
        assertEquals("xdismax", query.getNamedParameters().get("defType"));
        assertTrue(((List<?>) query.getNamedParameters().get(FQ)).contains(TYPE_DOCUMENT));
    }

    @Test
    void unsupportedFilterQuery() throws Exception
    {
        this.request.put(QUERY, "fq={!join from=space to=space}type:DOCUMENT");
        this.request.put(INPUT, "text");

        Query query = executeSuggestSolrService();

        // The query doesn't return any results.
        assertEquals(List.of("-*:*"), query.getNamedParameters().get(FQ));
    }

    @Test
    void createQueryFromParameterList() throws Exception
    {
        XWikiDocument testPage = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        testPage.setSyntax(Syntax.XWIKI_2_1);
        testPage.setContent("""
            {{include reference="XWiki.SuggestSolrMacros" /}}

            {{velocity}}
            #set ($paramList = [
              'q=__INPUT__*',
              'fq=type:DOCUMENT',
              $stringtool.join(['fq=name_exact:"first', 'second"'], $util.newline),
              'qf=title'
            ])
            #createSearchSuggestQueryFromParameterList($paramList 'text' $query)
            #set ($discard = $query.execute())
            {{/velocity}}
            """);
        this.xwiki.saveDocument(testPage, this.context);

        renderHTMLPage(testPage);

        assertEquals(1, this.executedQueries.size());
        Query query = this.executedQueries.get(0);
        assertEquals("text*", query.getStatement());
        assertEquals("title", query.getNamedParameters().get("qf"));
        // The line break is kept in the value.
        assertTrue(((List<?>) query.getNamedParameters().get(FQ))
            .containsAll(List.of(TYPE_DOCUMENT, "name_exact:\"first\nsecond\"")));
    }

    private Query executeSuggestSolrService() throws Exception
    {
        renderPage(SUGGEST_SOLR_SERVICE);

        assertEquals(1, this.executedQueries.size());
        return this.executedQueries.get(0);
    }
}
