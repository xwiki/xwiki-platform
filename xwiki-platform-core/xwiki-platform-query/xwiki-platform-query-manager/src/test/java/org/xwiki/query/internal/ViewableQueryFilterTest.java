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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ViewableQueryFilter}.
 *
 * @version $Id$
 * @since 9.8
 */
@ComponentTest
public class ViewableQueryFilterTest
{
    @InjectMockComponents
    private ViewableQueryFilter filter;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    private DocumentReference authorized = new DocumentReference("wiki", "Users", "Alice");

    private AttachmentReference unauthorized = new AttachmentReference("bob.png", authorized);

    @BeforeEach
    public void configure() throws Exception
    {
        when(this.authorization.hasAccess(Right.VIEW, authorized)).thenReturn(true);
        when(this.authorization.hasAccess(Right.VIEW, unauthorized)).thenReturn(false);
    }

    @Test
    public void filterStatement()
    {
        String statement = "select doc.fullName from XWikiDocument doc";
        assertSame(statement, this.filter.filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResultsEmpty()
    {
        List<Object> results = Collections.emptyList();
        assertEquals(results, this.filter.filterResults(results));
    }

    @Test
    public void filterResultsWithOneEntityReferenceColumn()
    {
        List<Object> results = Arrays.asList(this.unauthorized, this.authorized);
        assertEquals(Arrays.asList(this.authorized), this.filter.filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumnsAndEntityReference()
    {
        List<Object> results =
            Arrays.asList(new Object[]{ this.unauthorized, 23 }, new Object[]{ this.authorized, 17 });
        List<Object> filteredResults = this.filter.filterResults(results);
        assertEquals(1, filteredResults.size());
        assertArrayEquals(new Object[]{ this.authorized, 17 }, (Object[]) filteredResults.get(0));
    }

    @Test
    public void filterResultsWithOneColumnNotEntityReference()
    {
        List<Object> results = Collections.singletonList("Path.To.Page");
        assertEquals(Collections.emptyList(), this.filter.filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumnsNotEntityReference()
    {
        List<Object> results = Collections.singletonList(new Object[]{ 17, this.authorized });
        assertEquals(Collections.emptyList(), this.filter.filterResults(results));
    }
}
