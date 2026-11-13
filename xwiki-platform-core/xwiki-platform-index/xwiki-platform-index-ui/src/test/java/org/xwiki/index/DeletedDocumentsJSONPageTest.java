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
package org.xwiki.index;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryExecutor;
import org.xwiki.query.hql.HqlQueryScriptService;
import org.xwiki.query.hql.internal.DefaultHQLStatementValidator;
import org.xwiki.query.hql.internal.HQLCompleteStatementValidator;
import org.xwiki.query.internal.AnyCharsParameterPart;
import org.xwiki.query.internal.DefaultQuery;
import org.xwiki.query.internal.DefaultQueryParameter;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Tests of the {@code XWiki.DeletedDocumentsJSON} document, which provides the JSON data of the deleted documents
 * live table.
 *
 * @version $Id$
 */
@XWikiSyntax20ComponentList
@ComponentList({
    HqlQueryScriptService.class,
    DefaultHQLStatementValidator.class
})
class DeletedDocumentsJSONPageTest extends PageTest
{
    private static final DocumentReference DELETED_DOCUMENTS_JSON =
        new DocumentReference("xwiki", "XWiki", "DeletedDocumentsJSON");

    @Mock
    private QueryManagerScriptService queryService;

    @Mock
    private QueryExecutor queryExecutor;

    private final List<Query> executedQueries = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.PLAIN_1_0);
        registerVelocityTool("numbertool", new NumberTool());
        registerVelocityTool("mathtool", new MathTool());

        // The real HQL script service is used so that the order by validation is the one used in production.
        this.componentManager.registerMockComponent(HQLCompleteStatementValidator.class, "standard");
        ScriptService hqlScriptService = this.componentManager.getInstance(ScriptService.class, "query.hql");
        doReturn(hqlScriptService).when(this.queryService).get("hql");
        when(this.queryService.hql(anyString()))
            .thenAnswer(invocation -> new DefaultQuery(invocation.getArgument(0), "hql", this.queryExecutor));
        when(this.queryExecutor.execute(any())).thenAnswer(invocation -> {
            Query query = invocation.getArgument(0);
            this.executedQueries.add(query);
            return query.getStatement().startsWith("select count(") ? List.of(0L) : List.of();
        });
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryService);

        this.request.put("list", "1");
        this.request.put("xpage", "plain");
        this.request.put("reqNo", "1");
    }

    @Test
    void listWithoutParameters() throws Exception
    {
        String output = renderPage(DELETED_DOCUMENTS_JSON);

        assertTrue(StringUtils.normalizeSpace(output).startsWith("{ \"totalrows\": 0, \"returnedrows\": 0,"), output);
        assertEquals(2, this.executedQueries.size());
        assertStatement("select ddoc.id from XWikiDeletedDocument as ddoc where 1=1 order by ddoc.id asc",
            this.executedQueries.get(0));
        assertStatement("select count(*) from XWikiDeletedDocument as ddoc where 1=1", this.executedQueries.get(1));
    }

    @Test
    void filterByNameDeleterAndBatch() throws Exception
    {
        this.request.put("ddoc.fullName", " Space.Foo'Bar_% ");
        this.request.put("ddoc.deleter", "XWiki.O'Neil");
        this.request.put("ddoc.batchId", "Batch'1");

        renderPage(DELETED_DOCUMENTS_JSON);

        assertEquals(2, this.executedQueries.size());
        String filters = "lower(ddoc.fullName) like :docNameFilter and lower(ddoc.deleter) like :deleterFilter"
            + " and lower(ddoc.batchId) like :batchIdFilter";
        assertStatement("select ddoc.id from XWikiDeletedDocument as ddoc where 1=1 and " + filters
            + " order by ddoc.id asc", this.executedQueries.get(0));
        assertStatement("select count(*) from XWikiDeletedDocument as ddoc where 1=1 and " + filters,
            this.executedQueries.get(1));
        for (Query query : this.executedQueries) {
            assertContainsParameter("space.foo'bar_%", query, "docNameFilter");
            assertContainsParameter("xwiki.o'neil", query, "deleterFilter");
            assertContainsParameter("batch'1", query, "batchIdFilter");
        }
    }

    @Test
    void sortByColumn() throws Exception
    {
        this.request.put("sort", "ddoc.fullName");
        this.request.put("dir", "DESC");

        renderPage(DELETED_DOCUMENTS_JSON);

        assertStatement("select ddoc.id from XWikiDeletedDocument as ddoc where 1=1 order by ddoc.fullName desc",
            this.executedQueries.get(0));
    }

    @Test
    void sortWithUnknownDirection() throws Exception
    {
        this.request.put("sort", "ddoc.date");
        this.request.put("dir", "descending");

        renderPage(DELETED_DOCUMENTS_JSON);

        assertStatement("select ddoc.id from XWikiDeletedDocument as ddoc where 1=1 order by ddoc.date asc",
            this.executedQueries.get(0));
    }

    @Test
    void sortByExpressionIsRejected() throws Exception
    {
        this.request.put("sort", "lower(ddoc.fullName)");

        String output = renderPage(DELETED_DOCUMENTS_JSON);

        assertTrue(output.contains("Unsafe ORDER BY value [lower(ddoc.fullName)]"), output);
        assertTrue(this.executedQueries.isEmpty());
    }

    @Test
    void sortByPropertyOfAnotherEntityIsRejected() throws Exception
    {
        this.request.put("sort", "doc.fullName");

        String output = renderPage(DELETED_DOCUMENTS_JSON);

        assertTrue(output.contains("Unsafe ORDER BY value [doc.fullName]"), output);
        assertTrue(this.executedQueries.isEmpty());
    }

    private static void assertStatement(String expected, Query query)
    {
        assertEquals(expected, StringUtils.normalizeSpace(query.getStatement()));
    }

    /**
     * Asserts that the given parameter is bound as a "contains" pattern, i.e. any characters, the given value matched
     * literally and any characters again.
     */
    private static void assertContainsParameter(String expectedValue, Query query, String parameterName)
    {
        DefaultQueryParameter parameter = (DefaultQueryParameter) query.getNamedParameters().get(parameterName);
        String actual = parameter.getParts().stream()
            .map(part -> part instanceof AnyCharsParameterPart ? "*" : String.format("[%s]", part.getValue()))
            .collect(Collectors.joining());
        assertEquals(String.format("*[%s]*", expectedValue), actual);
    }
}
