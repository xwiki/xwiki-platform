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
@MockingRequirement(HiddenSpaceFilter.class)
public class HiddenSpaceFilterTest extends AbstractMockingComponentTestCase
{
    private QueryFilter filter;

    private UserResolver<UserReference> userResolver;

    @Before
    public void configure() throws Exception
    {
        this.userResolver = getComponentManager().getInstance(UserResolver.TYPE_USER_REFERENCE);
        User user = mockery.mock(User.class);
        getMockery().checking(new Expectations()
        {
            {
                ignoring(any(Logger.class)).method("debug");
                oneOf(userResolver).resolve(UserReference.CURRENT_USER_REFERENCE);
                will(returnValue(user));
                oneOf(user).displayHiddenDocuments();
                will(returnValue(true));
            }
        });

        this.filter = getComponentManager().getInstance(QueryFilter.class, "hidden/space");
    }

    @Test
    public void filterHQLStatementWithDoNotDisplayHiddenDocumentsInTheUserPreferences() throws Exception
    {
        assertEquals("select space.reference from XWikiSpace space where space.hidden <> true and (1=1)",
            filter.filterStatement("select space.reference from XWikiSpace space where 1=1", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithDisplayHiddenDocumentsInTheUserPreferences() throws Exception
    {
        // We need to do it that way since the expectation must be set in #configure() and the expectation sets the
        // isActive property to true
        ReflectionUtils.setFieldValue(this.filter, "isActive", false);

        // Insertions of distinct
        assertEquals("select space.reference from XWikiSpace space where 1=1",
            filter.filterStatement("select space.reference from XWikiSpace space where 1=1", Query.HQL));
    }

    @Test
    public void filterIncorrectHQLStatement() throws Exception
    {
        // Insertions of distinct
        assertEquals("select space.reference from XWikiSpace mydoc where 1=1",
            filter.filterStatement("select space.reference from XWikiSpace mydoc where 1=1", Query.HQL));
    }

    @Test
    public void filterXWQLStatement() throws Exception
    {
        assertEquals("select space.reference from XWikiSpace space where 1=1",
            filter.filterStatement("select space.reference from XWikiSpace space where 1=1", Query.XWQL));
    }

    @Test
    public void filterHQLStatementWithWhereAndOrderBy()
    {
        // Insertions of distinct
        assertEquals("select space.name from XWikiSpace space where space.hidden <> true and "
            + "(1=1) order by space.name",
            filter.filterStatement("select space.name from XWikiSpace space where 1=1 order by space.name", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithWhereAndGroupBy()
    {
        // Insertions of distinct
        assertEquals("select space.name from XWikiSpace space where space.hidden <> true and "
            + "(1=1) group by space.name",
            filter.filterStatement("select space.name from XWikiSpace space where 1=1 group by space.name", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithWhereAndOrderByAndGroupBy()
    {
        // Insertions of distinct
        assertEquals("select space.name from XWikiSpace space where space.hidden <> true and "
            + "(1=1) order by space.name group by space.name",
            filter.filterStatement("select space.name from XWikiSpace space where 1=1 order by space.name group by "
                + "space.name", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhere()
    {
        // Insertions of distinct
        assertEquals("select space.name from XWikiSpace space where space.hidden <> true",
            filter.filterStatement("select space.name from XWikiSpace space", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhereWithOrderBy()
    {
        // Insertions of distinct
        assertEquals("select space.name from XWikiSpace space where space.hidden <> true order by " + "space.name asc",
            filter.filterStatement("select space.name from XWikiSpace space order by space.name asc", Query.HQL));
    }

    @Test
    public void filterHQLStatementWithoutWhereWithGroupBy()
    {
        // Insertions of distinct
        assertEquals("select space.web, space.name from XWikiSpace space where space.hidden <> true "
            + "group by space.web",
            filter.filterStatement("select space.web, space.name from XWikiSpace space group by space.web", Query.HQL));
    }
}
