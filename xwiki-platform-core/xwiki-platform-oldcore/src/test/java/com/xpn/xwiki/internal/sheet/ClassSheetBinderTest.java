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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ClassSheetBinder}.
 * 
 * @version $Id$
 */
@ComponentTest
class ClassSheetBinderTest
{
    /**
     * The query used to retrieve the list of all sheet bindings.
     */
    private Query sheetBindingsQuery;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @InjectMockComponents
    private ClassSheetBinder binder;

    /**
     * This is called when the component is initialized.
     */
    @BeforeComponent
    void setUp() throws Exception
    {
        this.sheetBindingsQuery = mock(Query.class);
        // This is called when the component is initialized.
        when(this.queryManager.createQuery(any(), any())).thenReturn(this.sheetBindingsQuery);
    }

    /**
     * Unit test for {@link ClassSheetBinder#getDocuments(DocumentReference)}.
     */
    @Test
    void getDocuments() throws Exception
    {
        List<Object[]> queryResult = new ArrayList<Object[]>();
        queryResult.add(new Object[] {"Alice", "AnotherSheet"});
        queryResult.add(new Object[] {"Bob", "Sheet"});
        // If several fields are selected then T=Object[].
        when(this.sheetBindingsQuery.<Object[]> execute()).thenReturn(queryResult);

        DocumentReference sheetReference = new DocumentReference("wiki", "Space", "Sheet");

        DocumentReference aliceReference = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference anotherSheetReference = new DocumentReference("wiki", "Space", "AnotherSheet");
        when(this.documentReferenceResolver.resolve("Alice", sheetReference)).thenReturn(aliceReference);
        when(this.documentReferenceResolver.resolve("AnotherSheet", aliceReference)).thenReturn(anotherSheetReference);

        DocumentReference bobReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.documentReferenceResolver.resolve("Bob", sheetReference)).thenReturn(bobReference);
        when(this.documentReferenceResolver.resolve("Sheet", bobReference)).thenReturn(sheetReference);

        assertEquals(List.of(bobReference), this.binder.getDocuments(sheetReference));

        verify(this.sheetBindingsQuery).setWiki(sheetReference.getWikiReference().getName());
    }
}
