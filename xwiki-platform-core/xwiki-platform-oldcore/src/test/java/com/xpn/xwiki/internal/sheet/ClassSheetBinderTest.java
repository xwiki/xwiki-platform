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
package com.xpn.xwiki.internal.sheet;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link ClassSheetBinder}.
 * 
 * @version $Id$
 */
public class ClassSheetBinderTest
{
    /**
     * Mocks the dependencies of the component under test.
     */
    @Rule
    public final MockitoComponentMockingRule<SheetBinder> mocker = new MockitoComponentMockingRule<SheetBinder>(
        ClassSheetBinder.class);

    /**
     * The query used to retrieve the list of all sheet bindings.
     */
    private Query sheetBindingsQuery;

    @Before
    public void setUp() throws Exception
    {
        sheetBindingsQuery = mock(Query.class);
        QueryManager queryManager = mocker.getInstance(QueryManager.class);
        // This is called when the component is initialized.
        when(queryManager.createQuery(anyString(), anyString())).thenReturn(sheetBindingsQuery);
    }

    /**
     * Unit test for {@link ClassSheetBinder#getDocuments(DocumentReference)}.
     */
    @Test
    public void getDocuments() throws Exception
    {
        List<Object[]> queryResult = new ArrayList<Object[]>();
        queryResult.add(new Object[] {"Alice", "AnotherSheet"});
        queryResult.add(new Object[] {"Bob", "Sheet"});
        // If several fields are selected then T=Object[].
        when(sheetBindingsQuery.<Object[]> execute()).thenReturn(queryResult);

        DocumentReference sheetReference = new DocumentReference("wiki", "Space", "Sheet");
        DocumentReferenceResolver<String> documentReferenceResolver =
            mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference anotherSheetReference = new DocumentReference("wiki", "Space", "AnotherSheet");
        when(documentReferenceResolver.resolve("Alice", sheetReference)).thenReturn(aliceReference);
        when(documentReferenceResolver.resolve("AnotherSheet", aliceReference)).thenReturn(anotherSheetReference);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(documentReferenceResolver.resolve("Bob", sheetReference)).thenReturn(bobReference);
        when(documentReferenceResolver.resolve("Sheet", bobReference)).thenReturn(sheetReference);

        Assert.assertEquals(Arrays.asList(bobReference), mocker.getComponentUnderTest().getDocuments(sheetReference));

        verify(sheetBindingsQuery).setWiki(sheetReference.getWikiReference().getName());
    }
}
