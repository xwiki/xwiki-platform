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
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentConverter}.
 *
 * @version $Id$
 */
@OldcoreTest
class DocumentConverterTest
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
    void setup(MockitoOldcore mockitoOldcore)
    {
        this.documentReference = new DocumentReference("xwiki", "Foo", "WebHome");
        when(document.getDocumentReference()).thenReturn(this.documentReference);
        when(document.getDocument()).thenReturn(xWikiDocument);
        when(xWikiDocument.newDocument(any())).thenReturn(document);
        when(mockitoOldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);
    }

    @Test
    void convertToEntityReference()
    {
        assertEquals(this.documentReference, documentConverter.convert(EntityReference.class, document));
    }

    @Test
    void convertToDocumentReference()
    {
        assertEquals(this.documentReference, documentConverter.convert(DocumentReference.class, document));
    }

    @Test
    void convertToXWikiDocument()
    {
        assertSame(this.xWikiDocument, documentConverter.convert(XWikiDocument.class, document));
    }

    @Test
    void convertFromXWikiDocument()
    {
        Document convertedDocument = documentConverter.convert(Document.class, this.xWikiDocument);
        assertSame(this.xWikiDocument, convertedDocument.getDocument());
    }

    @Test
    void convertToString()
    {
        when(serializer.serialize(document.getDocumentReference())).thenReturn("reference");

        assertSame("reference", documentConverter.convert(String.class, document));
    }

    @Test
    void convertFromUnsupportedType()
    {
        ConversionException conversionException = assertThrows(ConversionException.class, () -> {
            documentConverter.convert(Document.class, this.documentReference);
        });

        assertEquals("Unsupported source type [class org.xwiki.model.reference.DocumentReference]",
            conversionException.getMessage());
    }

    @Test
    void convertToUnsupportedType()
    {
        ConversionException conversionException = assertThrows(ConversionException.class, () -> {
            documentConverter.convert(Integer.class, this.document);
        });

        assertEquals("Unsupported target type [class java.lang.Integer]", conversionException.getMessage());
    }
}
