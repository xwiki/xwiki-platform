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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.query.solr.SolrQueryScriptService;
import org.xwiki.query.solr.internal.SafeSolrQueryValidator;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.text.StringUtils;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.AllUserPropertiesResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.feed.FeedPlugin;
import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test for {@code Main.SolrSearch}.
 *
 * @version $Id$
 */
@ComponentList({
    TemplateMacro.class,
    TestNoScriptMacro.class,
    AllUserPropertiesResolver.class,
    SolrQueryScriptService.class,
    SafeSolrQueryValidator.class,
    DefaultSolrUtils.class
})
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
@DefaultUserComponentList
class SolrSearchPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String MAIN_SPACE = "Main";

    private static final DocumentReference SOLR_SEARCH_REFERENCE =
        new DocumentReference(WIKI_NAME, MAIN_SPACE, "SolrSearch");

    private static final DocumentReference SOLR_SEARCH_MACROS_REFERENCE =
        new DocumentReference(WIKI_NAME, MAIN_SPACE, "SolrSearchMacros");

    @BeforeEach
    void setUp() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);

        this.xwiki.getPluginManager().addPlugin("feed", FeedPlugin.class.getName(), this.context);

        loadPage(SOLR_SEARCH_MACROS_REFERENCE);
    }

    @Test
    void noEmptyParagraph() throws Exception
    {
        // Prevent that the search UI triggers a redirect.
        this.request.put("r", "1");
        Document searchDocument = renderHTMLPage(SOLR_SEARCH_REFERENCE);
        Element firstElement = searchDocument.body().firstElementChild();
        assertNotNull(firstElement);
        // When there is whitespace inserted in the Velocity macro output, the first tag is a paragraph instead.
        assertEquals("div", firstElement.tagName());
        assertEquals("search-ui", firstElement.className());
    }

    @Test
    void checkRSSFeedContent() throws Exception
    {
        String unescapedText = "<b>}}}{{noscript}}</b>";
        String escapedText = "&lt;b&gt;}}}{{noscript}}&lt;/b&gt;";

        this.request.put("text", unescapedText);

        String rssFeed = getRSSFeed();

        assertTrue(rssFeed.contains("<title>search.rss [[" + escapedText + "]]</title>"));
        assertTrue(rssFeed.contains("<description>search.rss [[" + escapedText + "]]</description>"));
    }

    @Test
    void supportedRequestParameters() throws Exception
    {
        List<Query> executedQueries = mockQueryExecution();

        this.request.put("text", "text");
        this.request.put("f_type", "DOCUMENT");
        this.request.put("f_property.My$20Space.Été.prop_string", "(value* OR other*)");
        this.request.put("f_property.My-App.Code.TagClass.tags_string", "tag");
        this.request.put("f_date", "[NOW-1DAY TO NOW]");
        this.request.put("f_creationdate", "[2024-01-01T00:00:00.000Z TO NOW/DAY+1DAY}");
        this.request.put("f_attsize", "{1024 TO *]");
        this.request.put("sort", "property.My$20Space.Été.prop_sortString");
        this.request.put("sortOrder", "asc");
        this.request.put("boost", "title^2 name^1.5 doccontent");

        getRSSFeed();

        assertEquals(1, executedQueries.size());
        Map<String, Object> parameters = executedQueries.get(0).getNamedParameters();
        List<?> filterQueries = (List<?>) parameters.get("fq");
        assertTrue(filterQueries.containsAll(List.of(
            "{!tag=type}type:(\"DOCUMENT\")",
            "{!tag=property.My$20Space.Été.prop_string}property.My$20Space.Été.prop_string:((value* OR other*))",
            "{!tag=property.My-App.Code.TagClass.tags_string}property.My-App.Code.TagClass.tags_string:(\"tag\")",
            "{!tag=date}date:([NOW-1DAY TO NOW])",
            "{!tag=creationdate}creationdate:([2024-01-01T00:00:00.000Z TO NOW/DAY+1DAY})",
            "{!tag=attsize}attsize:({1024 TO *])")),
            filterQueries.toString());
        assertEquals("property.My$20Space.Été.prop_sortString asc", parameters.get("sort"));
        assertEquals("title^2 name^1.5 doccontent", parameters.get("qf"));
    }

    @Test
    void unsupportedRequestParametersAreIgnored() throws Exception
    {
        List<Query> executedQueries = mockQueryExecution();

        this.request.put("text", "text");
        this.request.put("f_type", "DOCUMENT");
        this.request.put("f_some field", "value");
        this.request.put("f_$other", "value");
        this.request.put("f__query_", "{!term f=name}value");
        this.request.put("f_space", "{!lucene TO v=$text}");
        this.request.put("f_date", "[NOW-1DAY TO NOW] {!tag=date}");
        this.request.put("f_attsize", "[1 TO 2] OR name:value");
        this.request.put("f_creationdate", "[\"2024-01-01T00:00:00Z\" TO *]");
        this.request.put("f_name", "(name:value*)");
        this.request.put("f_title", "${text}");
        this.request.put("sort", "sum(1,2)");

        getRSSFeed();

        assertEquals(1, executedQueries.size());
        Map<String, Object> parameters = executedQueries.get(0).getNamedParameters();
        List<?> filterQueries = (List<?>) parameters.get("fq");
        assertTrue(filterQueries.stream().map(Object::toString).noneMatch(filterQuery -> filterQuery.contains("some field")
            || filterQuery.contains("other") || filterQuery.contains("_query_")), filterQueries.toString());
        // The unsupported values are used as literal values.
        assertTrue(filterQueries.containsAll(List.of(
            "{!tag=date}date:(\"\\[NOW\\-1DAY\\ TO\\ NOW\\]\\ \\{\\!tag=date\\}\")",
            "{!tag=attsize}attsize:(\"\\[1\\ TO\\ 2\\]\\ \\OR\\ name\\:value\")",
            "{!tag=creationdate}creationdate:(\"\\[\\\"2024\\-01\\-01T00\\:00\\:00Z\\\"\\ TO\\ \\*\\]\")",
            "{!tag=name}name:(\"\\(name\\:value\\*\\)\")",
            "{!tag=title}title:(\"$\\{text\\}\")",
            "{!tag=space}space:(\"\\{\\!lucene\\ TO\\ v=$text\\}\")")), filterQueries.toString());
        assertEquals("score desc", parameters.get("sort"));
    }

    private List<Query> mockQueryExecution() throws Exception
    {
        List<Query> executedQueries = new ArrayList<>();
        QueryExecutor queryExecutor = mock();
        QueryResponse queryResponse = mock();
        when(queryResponse.getResults()).thenReturn(new SolrDocumentList());
        when(queryExecutor.execute(any())).then(invocation -> {
            executedQueries.add(invocation.getArgument(0));
            return List.of(queryResponse);
        });

        QueryManagerScriptService queryManagerScriptService = mock();
        when(queryManagerScriptService.createQuery(anyString(), eq("solr"))).then(
            invocation -> new ScriptQuery(new DefaultQuery(invocation.getArgument(0), "solr", queryExecutor),
                this.componentManager));
        when(queryManagerScriptService.get("solr"))
            .thenReturn(this.componentManager.getInstance(ScriptService.class, "query.solr"));
        this.componentManager.registerComponent(ScriptService.class, "query", queryManagerScriptService);
        this.componentManager.registerMockComponent(QueryFilter.class, "searchExclusions/solr");

        return executedQueries;
    }

    private String getRSSFeed() throws Exception
    {
        this.request.put("media", "rss");
        this.context.setAction("get");

        XWikiDocument solrSearchDocument = loadPage(SOLR_SEARCH_REFERENCE);
        this.context.setDoc(solrSearchDocument);

        // Get directly the writer to check the RSS feed.
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        this.response = new XWikiServletResponseStub() {
            @Override
            public PrintWriter getWriter()
            {
                return writer;
            }
        };
        this.context.setResponse(this.response);

        String rssFeed = solrSearchDocument.displayDocument(Syntax.PLAIN_1_0, this.context);
        assertTrue(StringUtils.isAllBlank(rssFeed));

        return out.toString();
    }
}
