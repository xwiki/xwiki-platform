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
package org.xwiki.index.tree.internal.macro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

/**
 * Unit tests for {@link DocumentSortConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class DocumentSortConverterTest
{
    @InjectMockComponents
    private DocumentSortConverter documentSortConverter;

    @Test
    void verifyFromString()
    {
        DocumentSort documentSort = documentSortConverter.convert(DocumentSort.class, null);
        assertNull(documentSort);

        documentSort = documentSortConverter.convert(DocumentSort.class, "");
        assertNull(documentSort.getField());
        assertNull(documentSort.isAscending());

        documentSort = documentSortConverter.convert(DocumentSort.class, ":");
        assertNull(documentSort.getField());
        assertNull(documentSort.isAscending());

        documentSort = documentSortConverter.convert(DocumentSort.class, "field");
        assertEquals("field", documentSort.getField());
        assertNull(documentSort.isAscending());

        documentSort = documentSortConverter.convert(DocumentSort.class, "title:");
        assertEquals("title", documentSort.getField());
        assertNull(documentSort.isAscending());

        documentSort = documentSortConverter.convert(DocumentSort.class, ":desc");
        assertNull(documentSort.getField());
        assertEquals(false, documentSort.isAscending());

        documentSort = documentSortConverter.convert(DocumentSort.class, "date:asc");
        assertEquals("date", documentSort.getField());
        assertEquals(true, documentSort.isAscending());
    }

    @Test
    void verifyToString()
    {
        assertEquals("", documentSortConverter.convert(String.class, null));
        assertEquals("", documentSortConverter.convert(String.class, new DocumentSort(null, null)));
        assertEquals("field", documentSortConverter.convert(String.class, new DocumentSort("field", null)));
        assertEquals("field:asc", documentSortConverter.convert(String.class, new DocumentSort("field", true)));
        assertEquals("field:desc", documentSortConverter.convert(String.class, new DocumentSort("field", false)));
        assertEquals(":desc", documentSortConverter.convert(String.class, new DocumentSort(null, false)));
    }
}
