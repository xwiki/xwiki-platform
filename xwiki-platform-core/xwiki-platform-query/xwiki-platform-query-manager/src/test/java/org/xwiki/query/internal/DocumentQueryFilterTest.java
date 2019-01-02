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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentQueryFilter}.
 *
 * @version $Id$
 * @since 9.8
 */
@ComponentTest
public class DocumentQueryFilterTest
{
    @InjectMockComponents
    private DocumentQueryFilter filter;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    @Test
    public void filterStatement()
    {
        String statement = "select doc.fullName from XWikiDocument doc";
        assertSame(statement, this.filter.filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResultsWithOneColumn()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.resolver.resolve("Path.To.Page")).thenReturn(documentReference);

        List<Object> results = Arrays.asList("Path.To.Page");
        assertEquals(Arrays.asList(documentReference), this.filter.filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumns()
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.resolver.resolve("Path.To.Page")).thenReturn(documentReference);

        List<Object> results = Arrays.asList((Object) new Object[]{ "Path.To.Page", "red" });
        List<Object> filteredResults = this.filter.filterResults(results);
        assertEquals(1, filteredResults.size());
        assertArrayEquals(new Object[]{ documentReference, "red" }, (Object[]) filteredResults.get(0));
    }

    @Test
    public void filterResultsWithOneColumnButNotString()
    {
        List<Object> results = Collections.singletonList(23);
        assertSame(results, this.filter.filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumnsButNotString()
    {
        List<Object> results = Collections.singletonList(new Object[]{ 23, "Path.To.Page" });
        assertSame(results, this.filter.filterResults(results));
    }

    @Test
    public void filterResultsEmpty()
    {
        List<Object> results = Collections.emptyList();
        assertSame(results, this.filter.filterResults(results));
    }
}
