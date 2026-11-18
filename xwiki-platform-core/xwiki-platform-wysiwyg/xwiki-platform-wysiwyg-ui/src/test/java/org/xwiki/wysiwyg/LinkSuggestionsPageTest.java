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
package org.xwiki.wysiwyg;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test for the Solr search of {@code XWiki.WYSIWYG.LinkSuggestions}.
 *
 * @version $Id$
 */
@ComponentList({ ModelScriptService.class, LocalStringEntityReferenceSerializer.class })
@HTML50ComponentList
@XWikiSyntax21ComponentList
class LinkSuggestionsPageTest extends PageTest
{
    private static final String WIKI = "xwiki";

    private static final String PARAM_LIST = "paramList";

    private static final String SOLR = "solr";

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
        loadPage(new DocumentReference(WIKI, List.of("XWiki", "WYSIWYG"), "LinkSuggestions"));

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
            return List.of(new ResultsStub());
        });

        QueryManagerScriptService queryManagerScriptService = mock();
        when(queryManagerScriptService.createQuery(anyString(), eq(SOLR))).then(
            invocation -> new ScriptQuery(new DefaultQuery(invocation.getArgument(0), SOLR, queryExecutor),
                this.componentManager));
        when(queryManagerScriptService.get(SOLR)).thenReturn(new SolrQueryScriptServiceStub());
        this.componentManager.registerComponent(ScriptService.class, "query", queryManagerScriptService);
    }

    @Test
    void searchWithSpecialCharactersInSpaceName() throws Exception
    {
        this.context.setUserReference(new DocumentReference(WIKI, "XWiki", "Some \"User\""));

        String result = renderSearch(new DocumentReference(WIKI, "Some \"Space\"\nfl=name:wiki", "Page"));

        assertEquals(2, this.executedQueries.size(), result);
        assertEquals(List.of(
            "fq=type:DOCUMENT",
            "qf=title^6 name^4 doccontent^2 doccontentraw",
            "fl=type wiki spaces name score",
            "bf=recip(ms(NOW,date),3.16e-11,1,1)",
            "bq=author:quoted(xwiki:XWiki.Some \"User\")",
            "bq=creator:quoted(xwiki:XWiki.Some \"User\")"), getParamList(this.executedQueries.get(0)));
        assertEquals(List.of(
            "fq=type:ATTACHMENT",
            "qf=filename^4 attcontent",
            "fl=type wiki spaces name filename score",
            "bq=space_exact:quoted(Some \"Space\"\nfl=name\\:wiki)",
            "bf=recip(ms(NOW,attdate_sort),3.16e-11,1,1)",
            "bq=attauthor:quoted(xwiki:XWiki.Some \"User\")"), getParamList(this.executedQueries.get(1)));
    }

    @Test
    void searchAsGuest() throws Exception
    {
        this.context.setUserReference(null);

        String result = renderSearch(new DocumentReference(WIKI, "Space", "Page"));

        assertEquals(2, this.executedQueries.size(), result);
        assertEquals(List.of(
            "fq=type:DOCUMENT",
            "qf=title^6 name^4 doccontent^2 doccontentraw",
            "fl=type wiki spaces name score",
            "bf=recip(ms(NOW,date),3.16e-11,1,1)"), getParamList(this.executedQueries.get(0)));
        assertEquals(List.of(
            "fq=type:ATTACHMENT",
            "qf=filename^4 attcontent",
            "fl=type wiki spaces name filename score",
            "bq=space_exact:quoted(Space)",
            "bf=recip(ms(NOW,attdate_sort),3.16e-11,1,1)"), getParamList(this.executedQueries.get(1)));
    }

    /**
     * The result of a Solr query, as used by the page.
     */
    public static final class ResultsStub
    {
        /**
         * @return the results
         */
        public List<Object> getResults()
        {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getParamList(Query query)
    {
        return (List<String>) query.getNamedParameters().get(PARAM_LIST);
    }

    private String renderSearch(DocumentReference documentReference) throws Exception
    {
        XWikiDocument testPage = this.xwiki.getDocument(documentReference, this.context);
        testPage.setSyntax(Syntax.XWIKI_2_1);
        testPage.setContent("""
            {{include reference="XWiki.WYSIWYG.LinkSuggestions" /}}

            {{velocity}}
            #set ($results = [])
            #searchDocumentsSolr('text' 5 $results)
            #searchAttachmentsSolr('text' 5 $results)
            {{/velocity}}
            """);
        this.xwiki.saveDocument(testPage, this.context);
        // The page uses the current document ($doc).
        this.context.setDoc(testPage);

        return renderHTMLPage(testPage).html();
    }
}
