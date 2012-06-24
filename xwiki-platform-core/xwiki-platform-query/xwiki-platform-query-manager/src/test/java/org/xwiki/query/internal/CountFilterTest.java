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
import org.jmock.Expectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.query.Query;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link CountFilter}
 *
 * @version $Id$
 */
public class CountFilterTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private CountFilter filter;

    @Override
    public void configure() throws Exception
    {
        getMockery().checking(new Expectations() {{
            ignoring(any(Logger.class)).method("debug");
        }});
    }

    @Test
    public void filterSelectStatement() throws Exception
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc",
                filter.filterStatement("select doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterSelectDistinctStatement() throws Exception
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
                filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterSelectWithAsStatement() throws Exception
    {
        assertEquals("select count(doc.fullName) from XWikiDocument as doc",
                filter.filterStatement("select doc.fullName from XWikiDocument as doc", Query.HQL));
    }

    @Test
    public void filterSelectStatementWithMismatchingDocAlias() throws Exception
    {
        assertEquals("select mydoc.fullName from XWikiDocument mydoc",
                filter.filterStatement("select mydoc.fullName from XWikiDocument mydoc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementAlreadyContainsCount() throws Exception
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
                filter.filterStatement("select count(distinct doc.fullName) from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsOrderBy() throws Exception
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc ",
                filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsOrderByAndGroupBy() throws Exception
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc group by doc.web",
                filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name group by doc.web",
                    Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsDistinct() throws Exception
    {
        assertEquals("select count(distinct doc.fullName) from XWikiDocument doc",
                filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsMultipleColumns() throws Exception
    {
        assertEquals("select count(doc.fullName) from XWikiDocument doc group by doc.web",
                filter.filterStatement("select doc.fullName, doc.name, doc.space from XWikiDocument doc order by doc.name group by doc.web",
                    Query.HQL));
    }

    @Test
    public void getSelectColumns()
    {
        String[] columns = { "doc.fullName", "doc.name", "doc.space" };
        List<String> result = filter.getSelectColumns("select doc.fullName, doc.name, doc.space from XWikiDocument doc");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getSelectColumnsWithAdditionalSpaces()
    {
        String[] columns = { "doc.fullName", "doc.name", "doc.space" };
        List<String> result = filter.getSelectColumns("select  doc.fullName  , doc.name ,   doc.space from XWikiDocument doc");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getSelectColumnsWithDistinct()
    {
        String[] columns = { "distinct doc.fullName" };
        List<String> result = filter.getSelectColumns("select distinct doc.fullName from XWikiDocument doc");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumns() throws Exception
    {
        String[] columns = { "doc.name" };
        List<String> result = filter.getOrderByColumns("select doc.fullName from XWikiDocument doc order by doc.name");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumnsWithoutOrderBy() throws Exception
    {
        List<String> result = filter.getOrderByColumns("select doc.fullName from XWikiDocument doc");

        assertEquals(ListUtils.EMPTY_LIST, result);
    }

    @Test
    public void getOrderByColumnsWithMultipleColumns() throws Exception
    {
        String[] columns = { "doc.name", "doc.web" };
        List<String> result = filter.getOrderByColumns(
            "select doc.fullName from XWikiDocument doc order by doc.name, doc.web");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumnsWithGroupBy() throws Exception
    {
        String[] columns = { "doc.name", "doc.web" };
        List<String> result = filter.getOrderByColumns(
            "select doc.fullName from XWikiDocument doc order by doc.name, doc.web group by doc.web");

        assertEquals(Arrays.asList(columns), result);
    }

    @Test
    public void getOrderByColumnsWithAdditionalSpaces() throws Exception
    {
        String[] columns = { "doc.name", "doc.web" };
        List<String> result = filter.getOrderByColumns(
            "select doc.fullName from XWikiDocument doc order by doc.name  ,  doc.web  group by doc.web");

        assertEquals(Arrays.asList(columns), result);
    }

}
