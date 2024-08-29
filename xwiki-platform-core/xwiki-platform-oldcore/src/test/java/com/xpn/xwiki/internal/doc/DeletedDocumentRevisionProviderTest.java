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
package com.xpn.xwiki.internal.doc;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DeletedDocumentRevisionProvider}.
 *
 * @version $Id$
 */
@ComponentTest
class DeletedDocumentRevisionProviderTest
{
    @InjectMockComponents
    private DeletedDocumentRevisionProvider revisionProvider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private PageReferenceResolver<EntityReference> pageReferenceResolver;

    private XWikiContext context;
    private XWiki xwiki;

    @BeforeEach
    void setup()
    {
        this.context = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);

        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void getRevision() throws XWikiException
    {
        DocumentReference documentReference = mock(DocumentReference.class, "originalRef");
        String revision = "145";

        assertNull(this.revisionProvider.getRevision(documentReference, revision));

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(this.xwiki.getDeletedDocument(145L, this.context)).thenReturn(deletedDocument);

        DocumentReference deletedDocRef = mock(DocumentReference.class, "originalRef");
        when(deletedDocument.getDocumentReference()).thenReturn(deletedDocRef);
        PageReference originalRef = mock(PageReference.class, "originalRef");
        PageReference obtainedDocRef = mock(PageReference.class, "otherRef");
        when(this.pageReferenceResolver.resolve(documentReference)).thenReturn(originalRef);
        when(this.pageReferenceResolver.resolve(deletedDocRef)).thenReturn(obtainedDocRef);

        assertNull(this.revisionProvider.getRevision(documentReference, revision));
        verify(deletedDocument, never()).restoreDocument(any());

        XWikiDocument revisionDoc = mock(XWikiDocument.class);
        when(deletedDocument.restoreDocument(context)).thenReturn(revisionDoc);

        when(this.pageReferenceResolver.resolve(deletedDocRef)).thenReturn(originalRef);
        assertEquals(revisionDoc, this.revisionProvider.getRevision(documentReference, revision));
    }
}