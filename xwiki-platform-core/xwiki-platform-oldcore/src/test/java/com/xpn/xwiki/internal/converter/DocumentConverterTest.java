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
package com.xpn.xwiki.internal.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentConverter}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentConverterTest
{
    @InjectMockComponents
    private DocumentConverter documentConverter;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private Document document;

    private DocumentReference documentReference;

    @BeforeEach
    public void setup() throws Exception
    {
        this.documentReference = new DocumentReference("xwiki", "Foo", "WebHome");
        when(document.getDocumentReference()).thenReturn(this.documentReference);
        when(document.getDocument()).thenReturn(xWikiDocument);
    }

    @Test
    public void convertToEntityReference()
    {
        assertEquals(this.documentReference, documentConverter.convert(EntityReference.class, document));
    }

    @Test
    public void convertToDocumentReference()
    {
        assertEquals(this.documentReference, documentConverter.convert(DocumentReference.class, document));
    }

    @Test
    public void convertToXWikiDocument()
    {
        assertSame(this.xWikiDocument, documentConverter.convert(XWikiDocument.class, document));
    }

    @Test
    public void convertToString()
    {
        when(serializer.serialize(document.getDocumentReference())).thenReturn("reference");

        assertSame("reference", documentConverter.convert(String.class, document));
    }
}
