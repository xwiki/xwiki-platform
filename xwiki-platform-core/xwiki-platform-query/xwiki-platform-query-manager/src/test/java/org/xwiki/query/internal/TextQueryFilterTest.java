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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TextQueryFilter}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class TextQueryFilterTest
{
    @Rule
    public MockitoComponentMockingRule<QueryFilter> mocker =
        new MockitoComponentMockingRule<QueryFilter>(TextQueryFilter.class);

    @Test
    public void filterStatement() throws Exception
    {
        String result = this.mocker.getComponentUnderTest()
            .filterStatement("seLEct disTinCT user.alias as alias, user.name, user.age as unfilterable_age "
                + "fROm Users user where user.age >= 18", Query.HQL);
        assertEquals(
            "seLEct disTinCT user.alias as alias, user.name, user.age as unfilterable_age " + "fROm Users user "
                + "where (lower(str(user.alias)) like lower(:text) or lower(str(user.name)) like lower(:text))"
                + " and (user.age >= 18)",
            result);
    }

    @Test
    public void filterStatementWithNoFilterableColumns() throws Exception
    {
        String statement = "select age as unfilterable_age from Users";
        assertEquals(statement, this.mocker.getComponentUnderTest().filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResults() throws Exception
    {
        assertEquals("Results should not be filtered.", Arrays.asList("one", "two"),
            this.mocker.getComponentUnderTest().filterResults(Arrays.asList("one", "two")));
    }
}
