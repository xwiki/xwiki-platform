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
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ViewableQueryFilter}.
 * 
 * @version $Id$
 * @since 9.8
 */
public class ViewableQueryFilterTest
{
    @Rule
    public MockitoComponentMockingRule<QueryFilter> mocker =
        new MockitoComponentMockingRule<QueryFilter>(ViewableQueryFilter.class);

    private ContextualAuthorizationManager authorization;

    private DocumentReference authorized = new DocumentReference("wiki", "Users", "Alice");

    private AttachmentReference unauthorized = new AttachmentReference("bob.png", authorized);

    @Before
    public void configure() throws Exception
    {
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);
        when(this.authorization.hasAccess(Right.VIEW, authorized)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, unauthorized)).thenReturn(false);
    }

    @Test
    public void filterStatement() throws Exception
    {
        String statement = "select doc.fullName from XWikiDocument doc";
        assertSame(statement, this.mocker.getComponentUnderTest().filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResultsEmpty() throws Exception
    {
        List<Object> results = Collections.emptyList();
        assertEquals(results, this.mocker.getComponentUnderTest().filterResults(results));
    }

    @Test
    public void filterResultsWithOneEntityReferenceColumn() throws Exception
    {
        List<Object> results = Arrays.asList(this.unauthorized, this.authorized);
        assertEquals(Arrays.asList(this.authorized), this.mocker.getComponentUnderTest().filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumnsAndEntityReference() throws Exception
    {
        List<Object> results = Arrays.asList(new Object[] {this.unauthorized, 23}, new Object[] {this.authorized, 17});
        List<Object> filteredResults = this.mocker.getComponentUnderTest().filterResults(results);
        assertEquals(1, filteredResults.size());
        assertArrayEquals(new Object[] {this.authorized, 17}, (Object[]) filteredResults.get(0));
    }

    @Test
    public void filterResultsWithOneColumnNotEntityReference() throws Exception
    {
        List<Object> results = Collections.singletonList("Path.To.Page");
        assertEquals(Collections.emptyList(), this.mocker.getComponentUnderTest().filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumnsNotEntityReference() throws Exception
    {
        List<Object> results = Collections.singletonList(new Object[] {17, this.authorized});
        assertEquals(Collections.emptyList(), this.mocker.getComponentUnderTest().filterResults(results));
    }
}
