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

import org.junit.jupiter.api.Test;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link org.xwiki.query.internal.UniqueDocumentFilter}
 *
 * @version $Id$
 */
@ComponentTest
public class UniqueDocumentFilterTest
{
    @InjectMockComponents
    private UniqueDocumentFilter filter;

    @Test
    public void filterSelectStatement()
    {
        assertEquals("select distinct doc.fullName from XWikiDocument doc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc", Query.HQL));

        List<String> items = Arrays.asList("doc1", "doc2");
        assertThat((List<String>) this.filter.filterResults(items), is(items));

        // Verify it works with keywords in uppercase
        assertEquals("select distinct doc.fullName FROM XWikiDocument doc",
            this.filter.filterStatement("SELECT doc.fullName FROM XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterSelectWithAsStatement()
    {
        assertEquals("select distinct doc.fullName from XWikiDocument as doc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument as doc", Query.HQL));
    }

    @Test
    public void filterSelectStatementWithMismatchingDocAlias()
    {
        assertEquals("select mydoc.fullName from XWikiDocument mydoc",
            this.filter.filterStatement("select mydoc.fullName from XWikiDocument mydoc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementAlreadyContainsDistinct()
    {
        assertEquals("select distinct doc.fullName from XWikiDocument doc",
            this.filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsAnotherOrderBy()
    {
        assertEquals("select distinct doc.fullName, doc.name from XWikiDocument doc order by doc.name asc",
            this.filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name asc", Query.HQL));

        List<Object[]> results = this.filter.filterResults(
            Arrays.asList(new Object[]{ "full1", "name1" }, new Object[]{ "full2", "name2" }));
        assertEquals(2, results.size());
        assertEquals("full1", results.get(0));
        assertEquals("full2", results.get(1));
    }

    @Test
    public void filterStatementWhenTheFirstSelectColumnIsNotFullName()
    {
        assertEquals("select distinct doc.fullName, doc.name from XWikiDocument doc order by doc.name asc",
            this.filter.filterStatement("select doc.name, doc.fullName from XWikiDocument doc order by doc.name asc",
                Query.HQL));

        List<Object[]> results = this.filter.filterResults(
            Arrays.asList(new Object[]{ "full1", "name1" }, new Object[]{ "full2", "name2" }));
        assertEquals(2, results.size());
        assertEquals(2, results.get(0).length);
        assertEquals("full1", results.get(0)[0]);
        assertEquals("name1", results.get(0)[1]);
        assertEquals(2, results.get(1).length);
        assertEquals("full2", results.get(1)[0]);
        assertEquals("name2", results.get(1)[1]);
    }
}
