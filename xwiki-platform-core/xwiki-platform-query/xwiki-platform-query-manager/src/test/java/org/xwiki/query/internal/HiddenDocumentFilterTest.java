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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.query.Query;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link HiddenDocumentFilter}
 *
 * @version $Id$
 */
public class HiddenDocumentFilterTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private HiddenDocumentFilter filter;

    private ConfigurationSource userConfiguration;

    @Override
    public void configure() throws Exception
    {
        userConfiguration = getComponentManager().getInstance(ConfigurationSource.class, "user");
        getMockery().checking(new Expectations() {{
            ignoring(any(Logger.class)).method("debug");
        }});
    }

    @Test
    public void filterHQLStatementWithDoNotDisplayHiddenDocumentsInTheUserPreferences() throws Exception
    {
        getMockery().checking(new Expectations() {{
            oneOf(userConfiguration).getProperty("displayHiddenDocuments", Integer.class);
            will(returnValue(0));
        }});
        assertEquals(
                "select doc.fullName from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) and 1=1",
                filter.filterStatement("select doc.fullName from XWikiDocument doc where 1=1", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithDisplayHiddenDocumentsInTheUserPreferences() throws Exception
    {
        getMockery().checking(new Expectations() {{
            oneOf(userConfiguration).getProperty("displayHiddenDocuments", Integer.class);
            will(returnValue(1));
        }});

        // Insertions of distinct
        assertEquals("select doc.fullName from XWikiDocument doc where 1=1",
                filter.filterStatement("select doc.fullName from XWikiDocument doc where 1=1", Query.HQL));
    }

    @Test
    public void filterIncorrectHQLStatement() throws Exception
    {
        getMockery().checking(new Expectations() {{
            oneOf(userConfiguration).getProperty("displayHiddenDocuments", Integer.class);
            will(returnValue(0));
        }});

        // Insertions of distinct
        assertEquals("select doc.fullName from XWikiDocument mydoc where 1=1",
                filter.filterStatement("select doc.fullName from XWikiDocument mydoc where 1=1", Query.HQL));
    }

    @Test
    public void filterXWQLStatement() throws Exception
    {
        assertEquals("select doc.fullName from XWikiDocument doc where 1=1",
                filter.filterStatement("select doc.fullName from XWikiDocument doc where 1=1", Query.XWQL));
    }

    @Test
    public void filterHQLStatementWithoutWhere()
    {
        getMockery().checking(new Expectations() {{
            oneOf(userConfiguration).getProperty("displayHiddenDocuments", Integer.class);
            will(returnValue(0));
        }});

        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null)",
                filter.filterStatement("select doc.name from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhereWithOrderBy()
    {
        getMockery().checking(new Expectations() {{
            oneOf(userConfiguration).getProperty("displayHiddenDocuments", Integer.class);
            will(returnValue(0));
        }});

        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where doc.hidden <> true or doc.hidden is null order by "
                + "doc.name asc",
                filter.filterStatement("select doc.name from XWikiDocument doc order by doc.name asc", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhereWithGroupBy()
    {
        getMockery().checking(new Expectations() {{
            oneOf(userConfiguration).getProperty("displayHiddenDocuments", Integer.class);
            will(returnValue(0));
        }});

        // Insertions of distinct
        assertEquals("select doc.web, doc.name from XWikiDocument doc where doc.hidden <> true or doc.hidden is null " +
                "group by doc.web",
                filter.filterStatement("select doc.web, doc.name from XWikiDocument doc group by doc.web", Query.HQL));
    }
}
