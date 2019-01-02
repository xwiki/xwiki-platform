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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CountDocumentFilter}
 *
 * @version $Id$
 */
@ComponentTest
public class CountDocumentFilterTest
{
    @InjectMockComponents
    private CountDocumentFilter filter;

    @Test
    public void filterSelectStatement()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc", Query.HQL));

        // Verify it works with keywords in uppercase
        assertEquals("select count(doc.fullName) FROM XWikiDocument doc",
            this.filter.filterStatement("SELECT doc.fullName FROM XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterSelectStatementWithWhitespace()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument as doc",
            this.filter.filterStatement("  select doc.fullName from XWikiDocument as doc ", Query.HQL));
    }

    @Test
    public void filterSelectDistinctStatement()
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterSelectWithAsStatement()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument as doc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument as doc", Query.HQL));
    }

    @Test
    public void filterSelectStatementWithMismatchingDocAlias()
    {
        assertEquals("select mydoc.fullName from XWikiDocument mydoc",
            this.filter.filterStatement("select mydoc.fullName from XWikiDocument mydoc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementAlreadyContainsCount()
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select count(distinct doc.fullName) from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsOrderBy()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc ",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsOrderByAndGroupBy()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc group by doc.web",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name group by doc.web",
                Query.HQL)
        );
    }

    @Test
    public void filterStatementWhenStatementContainsDistinct()
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
            this.filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsMultipleColumns()
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc group by doc.web",
            this.filter.filterStatement(
                "select doc.fullName, doc.name, doc.space from XWikiDocument doc order by doc.name group by doc.web",
                Query.HQL)
        );
    }

    @Test
    public void getSelectColumns()
    {
        String[] columns = { "doc.fullName", "doc.name", "doc.space" };
        List<String> result =
            this.filter.getSelectColumns("select doc.fullName, doc.name, doc.space from XWikiDocument doc");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getSelectColumnsWithAdditionalSpaces()
    {
        String[] columns = { "doc.fullName", "doc.name", "doc.space" };
        List<String> result =
            this.filter.getSelectColumns("select  doc.fullName  , doc.name ,   doc.space from XWikiDocument doc");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getSelectColumnsWithDistinct()
    {
        String[] columns = { "distinct doc.fullName" };
        List<String> result = this.filter.getSelectColumns("select distinct doc.fullName from XWikiDocument doc");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumns()
    {
        String[] columns = { "doc.name" };
        List<String> result =
            this.filter.getOrderByColumns("select doc.fullName from XWikiDocument doc order by doc.name");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumnsWithoutOrderBy()
    {
        List<String> result = this.filter.getOrderByColumns("select doc.fullName from XWikiDocument doc");

        assertEquals(ListUtils.EMPTY_LIST, result);
    }

    @Test
    public void getOrderByColumnsWithMultipleColumns()
    {
        String[] columns = { "doc.name", "doc.web" };
        List<String> result = this.filter.getOrderByColumns(
            "select doc.fullName from XWikiDocument doc order by doc.name, doc.web");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumnsWithGroupBy()
    {
        String[] columns = { "doc.name", "doc.web" };
        List<String> result = this.filter.getOrderByColumns(
            "select doc.fullName from XWikiDocument doc order by doc.name, doc.web group by doc.web");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumnsWithAdditionalSpaces()
    {
        String[] columns = { "doc.name", "doc.web" };
        List<String> result = this.filter.getOrderByColumns(
            "select doc.fullName from XWikiDocument doc order by doc.name  ,  doc.web  group by doc.web");

        assertEquals(Arrays.asList(columns), result);
    }
}
