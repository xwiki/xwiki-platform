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
package org.xwiki.eventstream.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultModelBridge}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@ComponentTest
class DefaultModelBridgeTest
{
    @InjectMockComponents
    private DefaultModelBridge defaultModelBridge;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Test
    void authorReference() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference authorReference = mock(DocumentReference.class);
        EntityReference entityReference = mock(EntityReference.class);

        when(this.contextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(entityReference, context)).thenReturn(document);
        when(document.getAuthorReference()).thenReturn(authorReference);

        DocumentReference result = this.defaultModelBridge.getAuthorReference(entityReference);

        assertEquals(authorReference, result);
    }

    @Test
    void checkXObjectPresenceWithEmptyList()
    {
        assertTrue(this.defaultModelBridge.checkXObjectPresence(List.of(), new Object()));
    }

    @Test
    void checkXObjectPresenceWithPresentXObject()
    {
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference resolvedType1 = mock(DocumentReference.class);
        DocumentReference documentReferenceFromDocumentXObjects = mock(DocumentReference.class);
        LocalDocumentReference localDocumentReferenceType1 = mock(LocalDocumentReference.class);
        DocumentReference resolvedType2 = mock(DocumentReference.class);
        LocalDocumentReference localDocumentReferenceType2 = mock(LocalDocumentReference.class);

        when(resolvedType1.getLocalDocumentReference()).thenReturn(localDocumentReferenceType1);
        when(this.documentReferenceResolver.resolve("type1")).thenReturn(resolvedType1);
        when(document.getXObjects()).thenReturn(Map.of(documentReferenceFromDocumentXObjects, List.of()));
        when(documentReferenceFromDocumentXObjects.getLocalDocumentReference()).thenReturn(localDocumentReferenceType1);
        when(resolvedType2.getLocalDocumentReference()).thenReturn(localDocumentReferenceType2);
        when(this.documentReferenceResolver.resolve("type2")).thenReturn(resolvedType2);

        assertTrue(this.defaultModelBridge.checkXObjectPresence(List.of("type1"), document));
        assertFalse(this.defaultModelBridge.checkXObjectPresence(List.of("type2"), document));
    }
}
