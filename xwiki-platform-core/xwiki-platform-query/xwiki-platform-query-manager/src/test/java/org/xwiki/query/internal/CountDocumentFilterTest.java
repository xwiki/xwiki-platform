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
package org.xwiki.query.internal;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link CountDocumentFilter}
 *
 * @version $Id$
 */
@ComponentTest
class CountDocumentFilterTest
{
    @InjectMockComponents
    private CountDocumentFilter filter;

    @Test
    void filterSelectStatement()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc", Query.HQL));

        // Verify it works with keywords in uppercase
        assertEquals("select count(doc.fullName) FROM XWikiDocument doc",
            this.filter.filterStatement("SELECT doc.fullName FROM XWikiDocument doc", Query.HQL));
    }

    @Test
    void filterSelectStatementWithWhitespace()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument as doc",
            this.filter.filterStatement("  select doc.fullName from XWikiDocument as doc ", Query.HQL));
    }

    @Test
    void filterSelectDistinctStatement()
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    void filterSelectWithAsStatement()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument as doc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument as doc", Query.HQL));
    }

    @Test
    void filterSelectStatementWithMismatchingDocAlias()
    {
        assertEquals("select mydoc.fullName from XWikiDocument mydoc",
            this.filter.filterStatement("select mydoc.fullName from XWikiDocument mydoc", Query.HQL));
    }

    @Test
    void filterStatementWhenStatementAlreadyContainsCount()
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select count(distinct doc.fullName) from XWikiDocument doc", Query.HQL));
    }

    @Test
    void filterStatementWhenStatementContainsOrderBy()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc ",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name", Query.HQL));
    }

    @Test
    void filterStatementWhenStatementContainsOrderByAndGroupBy()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc group by doc.web",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name group by doc.web",
                Query.HQL)
        );
    }

    @Test
    void filterStatementWhenStatementContainsDistinct()
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    void filterStatementWhenStatementContainsMultipleColumns()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc group by doc.web",
            this.filter.filterStatement(
                "select doc.fullName, doc.name, doc.space from XWikiDocument doc order by doc.name group by doc.web",
                Query.HQL)
        );
    }

    @Test
    void getSelectColumns()
    {
        String[] columns = { "doc.fullName", "doc.name", "doc.space" };
        List<String> result =
            this.filter.getSelectColumns("select doc.fullName, doc.name, doc.space from XWikiDocument doc");

        assertEquals(List.of(columns), result);
    }

    @Test
    void getSelectColumnsWithAdditionalSpaces()
    {
        String[] columns = { "doc.fullName", "doc.name", "doc.space" };
        List<String> result =
            this.filter.getSelectColumns("select  doc.fullName  , doc.name ,   doc.space from XWikiDocument doc");

        assertEquals(List.of(columns), result);
    }

    @Test
    void getSelectColumnsWithDistinct()
    {
        String[] columns = { "distinct doc.fullName" };
        List<String> result = this.filter.getSelectColumns("select distinct doc.fullName from XWikiDocument doc");

        assertEquals(List.of(columns), result);
    }

    static Stream<Arguments> getOrderByColumnsSource()
    {
        return Stream.of(
            arguments(
                "getOrderByColumns",
                "select doc.fullName from XWikiDocument doc order by doc.name",
                List.of("doc.name")),
            arguments(
                "getOrderByColumnsWithoutOrderBy",
                "select doc.fullName from XWikiDocument doc",
                List.of()),
            arguments(
                "getOrderByColumnsWithMultipleColumns",
                "select doc.fullName from XWikiDocument doc order by doc.name, doc.web",
                List.of("doc.name", "doc.web")),
            arguments(
                "getOrderByColumnsWithGroupBy",
                "select doc.fullName from XWikiDocument doc order by doc.name, doc.web group by doc.web",
                List.of("doc.name", "doc.web")),
            arguments(
                "getOrderByColumnsWithAdditionalSpaces",
                "select doc.fullName from XWikiDocument doc order by doc.name  ,  doc.web  group by doc.web",
                List.of("doc.name", "doc.web"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getOrderByColumnsSource")
    void getOrderByColumns(String description, String statement, List<String> expected)
    {
        assertEquals(expected, this.filter.getOrderByColumns(statement));
    }
}
