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

import org.jmock.Expectations;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.query.Query;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link org.xwiki.query.internal.UniqueDocumentFilter}
 *
 * @version $Id$
 */
public class UniqueDocumentFilterTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private UniqueDocumentFilter filter;

    @Test
    public void filterSelectStatement() throws Exception
    {
        assertEquals("select distinct doc.fullName from XWikiDocument doc",
            filter.filterStatement("select doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Override
    public void configure() throws Exception
    {
        getMockery().checking(new Expectations() {{
            ignoring(any(Logger.class)).method("debug");
            ignoring(any(Logger.class)).method("warn");
        }});
    }

    @Test
    public void filterSelectWithAsStatement() throws Exception
    {
        assertEquals("select distinct doc.fullName from XWikiDocument as doc",
                filter.filterStatement("select doc.fullName from XWikiDocument as doc", Query.HQL));
    }

    @Test
    public void filterSelectStatementWithMismatchingDocAlias() throws Exception
    {
        assertEquals("select mydoc.fullName from XWikiDocument mydoc",
                filter.filterStatement("select mydoc.fullName from XWikiDocument mydoc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementAlreadyContainsDistinct() throws Exception
    {
        assertEquals("select distinct doc.fullName from XWikiDocument doc",
                filter.filterStatement("select distinct doc.fullName from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterStatementWhenStatementContainsAnotherOrderBy() throws Exception
    {
        assertEquals("select distinct doc.fullName, doc.name from XWikiDocument doc order by doc.name asc",
            filter.filterStatement("select doc.fullName from XWikiDocument doc order by doc.name asc", Query.HQL));
    }

    @Test
    public void filterStatementWhenTheFirstSelectColumnIsNotFullName() throws Exception
    {
        assertEquals("select distinct doc.fullName, doc.name from XWikiDocument doc order by doc.name asc",
            filter.filterStatement("select doc.name, doc.fullName from XWikiDocument doc order by doc.name asc",
                Query.HQL));
    }
}
