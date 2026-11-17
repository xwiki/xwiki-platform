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
package org.xwiki.query.solr.internal;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link SolrUntrustedParametersQueryFilter}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(SafeSolrQueryValidator.class)
class SolrUntrustedParametersQueryFilterTest
{
    private static final String STATEMENT = "text";

    private static final String FQ = "fq";

    private static final String FL = "fl";

    private static final String SORT = "sort";

    private static final String TYPE_DOCUMENT = "type:DOCUMENT";

    private static final List<String> NO_RESULTS = List.of("-*:*");

    private static final String DEF_TYPE = "defType";

    private static final String XDISMAX = "xdismax";

    private static final String BQ = "bq";

    @InjectMockComponents
    private SolrUntrustedParametersQueryFilter filter;

    @Test
    void supportedParameters()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FQ, List.of(TYPE_DOCUMENT, "space:\"Main\""));
        query.bindValue(FL, "wiki,spaces name  title_ *_string property.My$20Space.Été.prop_string doc-id");
        query.bindValue(SORT, "date desc , name asc,property.My$20Space.Été.prop_sortString desc, title_sort ASC");
        Map<String, Object> scoringParameters = Map.ofEntries(
            Map.entry("qf", "title^2 name"),
            Map.entry("pf", "title^10"),
            Map.entry("pf2", "title^5"),
            Map.entry("pf3", "title^2.5"),
            Map.entry("ps", "2"),
            Map.entry("ps2", "1"),
            Map.entry("ps3", "3"),
            Map.entry("qs", "1"),
            Map.entry("mm", "2<-1 5<80%"),
            Map.entry("q.op", "AND"),
            Map.entry("tie", "0.1"),
            Map.entry("timeAllowed", "500"));
        query.bindValues(scoringParameters);
        query.bindValue(BQ, List.of("type:DOCUMENT^2", "+space:Main +hidden:false"));

        Query filteredQuery = this.filter.filterQuery(query);

        assertEquals(STATEMENT, filteredQuery.getStatement());
        Map<String, Object> parameters = filteredQuery.getNamedParameters();
        assertEquals(List.of(TYPE_DOCUMENT, "space:\"Main\""), parameters.get(FQ));
        assertEquals("wiki,spaces,name,title_,*_string,property.My$20Space.Été.prop_string,doc-id",
            parameters.get(FL));
        assertEquals("date desc,name asc,property.My$20Space.Été.prop_sortString desc,title_sort ASC",
            parameters.get(SORT));
        scoringParameters.forEach((name, value) -> assertEquals(value, parameters.get(name)));
        assertEquals(List.of("type:DOCUMENT^2", "+space:Main +hidden:false"), parameters.get(BQ));
        assertEquals(XDISMAX, parameters.get(DEF_TYPE));
        assertEquals(scoringParameters.size() + 5, parameters.size());
    }

    @Test
    void unsupportedParametersAreRemoved()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FQ, TYPE_DOCUMENT);
        for (String name : List.of("q", "bf", "boost", "df", "defType", "uf", "qt", "rows", "start", "facet",
            "hl", "debug", "expandMacros"))
        {
            query.bindValue(name, "value");
        }

        Map<String, Object> parameters = this.filter.filterQuery(query).getNamedParameters();

        assertEquals(Map.of(FQ, List.of(TYPE_DOCUMENT), DEF_TYPE, XDISMAX), parameters);
        // The original query isn't modified.
        assertEquals(14, query.getNamedParameters().size());
    }

    @Test
    void unsupportedFieldListAndSortItemsAreRemoved()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FL, "wiki name [docid] alias:name sum(1,2) $qf 1 title_*-en");
        query.bindValue(SORT,
            "date desc, sum(1,2) asc, name, $qf asc, 1 desc, -name asc, _val_ asc, name top, doc-id asc");

        Map<String, Object> parameters = this.filter.filterQuery(query).getNamedParameters();

        assertEquals("wiki,name", parameters.get(FL));
        assertEquals("date desc,doc-id asc", parameters.get(SORT));
    }

    @Test
    void fieldListAndSortWithoutSupportedItems()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FL, "[docid]");
        query.bindValue(SORT, "name");

        assertEquals(Map.of(DEF_TYPE, XDISMAX), this.filter.filterQuery(query).getNamedParameters());
    }

    @Test
    void supportedFilterQuery()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FQ, List.of(TYPE_DOCUMENT, "space:(Main OR Sandbox)"));

        assertEquals(List.of(TYPE_DOCUMENT, "space:(Main OR Sandbox)"),
            this.filter.filterQuery(query).getNamedParameters().get(FQ));
    }

    @Test
    void unsupportedFilterQuery()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FQ, List.of(TYPE_DOCUMENT, "{!join from=space to=space}type:DOCUMENT"));
        query.bindValue(FL, "name");

        Map<String, Object> parameters = this.filter.filterQuery(query).getNamedParameters();

        assertEquals(NO_RESULTS, parameters.get(FQ));
        assertEquals("name", parameters.get(FL));
    }

    @Test
    void unsupportedBoostQueriesAreRemoved()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(BQ, List.of("{!func}docfreq(doccontent,'text')", TYPE_DOCUMENT,
            "{!join from=space to=space}doccontent:text", "_query_:\"{!func}1\""));
        query.bindValue(FL, "name");

        Map<String, Object> parameters = this.filter.filterQuery(query).getNamedParameters();

        // Unlike unsupported filter queries, unsupported boost queries don't affect the matched documents.
        assertEquals(Map.of(BQ, List.of(TYPE_DOCUMENT), FL, "name", DEF_TYPE, XDISMAX), parameters);

        query.bindValue(BQ, "{!func}docfreq(doccontent,'text')");

        assertEquals(Map.of(FL, "name", DEF_TYPE, XDISMAX), this.filter.filterQuery(query).getNamedParameters());
    }

    @Test
    void statementWithSpecialSyntax()
    {
        // The statement is parsed with a query parser that treats this syntax as plain text.
        String statement = "{!tag=text}text _query_ _val_";
        DefaultQuery query = new DefaultQuery(statement, null);
        query.bindValue(FQ, TYPE_DOCUMENT);
        query.bindValue(DEF_TYPE, "lucene");

        Query filteredQuery = this.filter.filterQuery(query);

        assertEquals(statement, filteredQuery.getStatement());
        assertEquals(Map.of(FQ, List.of(TYPE_DOCUMENT), DEF_TYPE, XDISMAX), filteredQuery.getNamedParameters());
    }

    @Test
    void parametersBoundAfterFiltering()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.setLimit(5);
        query.setOffset(10);

        Query filteredQuery = this.filter.filterQuery(query);
        assertSame(filteredQuery, filteredQuery.bindValue(FQ, TYPE_DOCUMENT));

        assertEquals(Map.of(FQ, TYPE_DOCUMENT, DEF_TYPE, XDISMAX), filteredQuery.getNamedParameters());
        assertEquals(5, filteredQuery.getLimit());
        assertEquals(10, filteredQuery.getOffset());
    }

    @Test
    void arrayValues()
    {
        DefaultQuery query = new DefaultQuery(STATEMENT, null);
        query.bindValue(FQ, new String[] { TYPE_DOCUMENT, "{!join from=space to=space}type:DOCUMENT" });

        assertEquals(NO_RESULTS, this.filter.filterQuery(query).getNamedParameters().get(FQ));
    }
}
