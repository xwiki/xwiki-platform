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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentQueryFilter}.
 * 
 * @version $Id$
 * @since 9.8
 */
public class DocumentQueryFilterTest
{
    @Rule
    public MockitoComponentMockingRule<QueryFilter> mocker =
        new MockitoComponentMockingRule<QueryFilter>(DocumentQueryFilter.class);

    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Before
    public void configure() throws Exception
    {
        this.currentDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
    }

    @Test
    public void filterStatement() throws Exception
    {
        String statement = "select doc.fullName from XWikiDocument doc";
        assertSame(statement, this.mocker.getComponentUnderTest().filterStatement(statement, Query.HQL));
    }

    @Test
    public void filterResultsWithOneColumn() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.currentDocumentReferenceResolver.resolve("Path.To.Page")).thenReturn(documentReference);

        List<Object> results = Arrays.asList("Path.To.Page");
        assertEquals(Arrays.asList(documentReference), this.mocker.getComponentUnderTest().filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumns() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        when(this.currentDocumentReferenceResolver.resolve("Path.To.Page")).thenReturn(documentReference);

        List<Object> results = Arrays.asList((Object) new Object[] {"Path.To.Page", "red"});
        List<Object> filteredResults = this.mocker.getComponentUnderTest().filterResults(results);
        assertEquals(1, filteredResults.size());
        assertArrayEquals(new Object[] {documentReference, "red"}, (Object[]) filteredResults.get(0));
    }

    @Test
    public void filterResultsWithOneColumnButNotString() throws Exception
    {
        List<Object> results = Collections.singletonList(23);
        assertSame(results, this.mocker.getComponentUnderTest().filterResults(results));
    }

    @Test
    public void filterResultsWithTwoColumnsButNotString() throws Exception
    {
        List<Object> results = Collections.singletonList(new Object[] {23, "Path.To.Page"});
        assertSame(results, this.mocker.getComponentUnderTest().filterResults(results));
    }

    @Test
    public void filterResultsEmpty() throws Exception
    {
        List<Object> results = Collections.emptyList();
        assertSame(results, this.mocker.getComponentUnderTest().filterResults(results));
    }
}
