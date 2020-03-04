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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;
import org.xwiki.user.User;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserResolver;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link HiddenDocumentFilter}
 *
 * @version $Id$
 */
@MockingRequirement(HiddenDocumentFilter.class)
public class HiddenDocumentFilterTest extends AbstractMockingComponentTestCase
{
    private QueryFilter filter;

    private UserResolver<UserReference> userResolver;

    @Before
    public void configure() throws Exception
    {
        this.userResolver = getComponentManager().getInstance(UserResolver.TYPE_USER_REFERENCE);
        User user = mockery.mock(User.class);
        getMockery().checking(new Expectations()
        {{
                ignoring(any(Logger.class)).method("debug");
                oneOf(userResolver).resolve(UserReference.CURRENT_USER_REFERENCE);
                will(returnValue(user));
                oneOf(user).displayHiddenDocuments();
                will(returnValue(false));
            }});

        this.filter = getComponentManager().getInstance(QueryFilter.class, "hidden");
    }

    @Test
    public void filterHQLStatementWithDoNotDisplayHiddenDocumentsInTheUserPreferences() throws Exception
    {
        assertEquals(
            "select doc.fullName from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) and (1=1)",
            filter.filterStatement("select doc.fullName from XWikiDocument doc where 1=1", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithDisplayHiddenDocumentsInTheUserPreferences() throws Exception
    {
        // We need to do it that way since the expectation must be set in #configure() and the expectation sets the
        // displayHiddenDocuments property to true
        ReflectionUtils.setFieldValue(this.filter, "displayHiddenDocuments", true);

        // Insertions of distinct
        assertEquals("select doc.fullName from XWikiDocument doc where 1=1",
            filter.filterStatement("select doc.fullName from XWikiDocument doc where 1=1", Query.HQL));
    }

    @Test
    public void filterIncorrectHQLStatement() throws Exception
    {
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
    public void filterHQLStatementWithWhereAndOrderBy()
    {
        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) and "
            + "(1=1) order by doc.name",
            filter.filterStatement("select doc.name from XWikiDocument doc where 1=1 order by doc.name",
                Query.HQL));
    }

    @Test
    public void filterHQLStatementWithWhereAndGroupBy()
    {
        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) and "
            + "(1=1) group by doc.name",
            filter.filterStatement("select doc.name from XWikiDocument doc where 1=1 group by doc.name",
                Query.HQL));
    }

    @Test
    public void filterHQLStatementWithWhereAndOrderByAndGroupBy()
    {
        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) and "
            + "(1=1) order by doc.name group by doc.name",
            filter.filterStatement("select doc.name from XWikiDocument doc where 1=1 order by doc.name group by "
                + "doc.name", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhere()
    {
        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null)",
            filter.filterStatement("select doc.name from XWikiDocument doc", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhereWithOrderBy()
    {
        // Insertions of distinct
        assertEquals("select doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) order by "
            + "doc.name asc",
            filter.filterStatement("select doc.name from XWikiDocument doc order by doc.name asc", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhereWithGroupBy()
    {
        // Insertions of distinct
        assertEquals(
            "select doc.web, doc.name from XWikiDocument doc where (doc.hidden <> true or doc.hidden is null) " +
                "group by doc.web",
            filter.filterStatement("select doc.web, doc.name from XWikiDocument doc group by doc.web", Query.HQL));
    }
}
