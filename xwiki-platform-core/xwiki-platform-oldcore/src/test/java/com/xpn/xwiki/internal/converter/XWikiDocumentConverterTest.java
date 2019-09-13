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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiDocumentConverter}.
 *
 * @version $Id$
 */
@OldcoreTest
public class XWikiDocumentConverterTest
{
    @InjectMockComponents
    private XWikiDocumentConverter documentConverter;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private Document document;

    private DocumentReference documentReference;

    @BeforeEach
    public void setup(MockitoOldcore mockitoOldcore) throws Exception
    {
        this.documentReference = new DocumentReference("xwiki", "Foo", "WebHome");
        when(xWikiDocument.getDocumentReference()).thenReturn(this.documentReference);
        when(document.getDocument()).thenReturn(xWikiDocument);

        when(contextProvider.get()).thenReturn(mockitoOldcore.getXWikiContext());
        when(mockitoOldcore.getMockRightService().hasProgrammingRights(mockitoOldcore.getXWikiContext()))
            .thenReturn(true);
    }

    @Test
    public void convertToEntityReference()
    {
        assertEquals(this.documentReference, documentConverter.convert(EntityReference.class, xWikiDocument));
    }

    @Test
    public void convertToDocumentReference()
    {
        assertEquals(this.documentReference, documentConverter.convert(DocumentReference.class, xWikiDocument));
    }

    @Test
    public void convertToDocument()
    {
        Document result = documentConverter.convert(Document.class, xWikiDocument);
        assertSame(this.xWikiDocument, result.getDocument());
        verify(this.contextProvider, times(1)).get();
    }

    @Test
    public void convertFromDocument()
    {
        assertSame(this.xWikiDocument, documentConverter.convert(XWikiDocument.class, document));
    }
}
