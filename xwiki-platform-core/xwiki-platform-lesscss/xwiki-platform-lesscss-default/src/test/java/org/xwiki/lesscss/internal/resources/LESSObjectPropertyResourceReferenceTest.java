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
package org.xwiki.lesscss.internal.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LESSObjectPropertyResourceReference}.
 *
 * @version $Id$
 * @since 7.0RC1
 */
@ComponentTest
class LESSObjectPropertyResourceReferenceTest
{
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private DocumentAccessBridge bridge;

    @BeforeEach
    void setUp()
    {
        this.entityReferenceSerializer = mock(EntityReferenceSerializer.class);
        this.bridge = mock(DocumentAccessBridge.class);
    }

    @Test
    void getContent() throws Exception
    {
        ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference("property",
            new ObjectReference("class", new DocumentReference("wiki", "Space", "Document")));
        LESSObjectPropertyResourceReference lessObjectPropertyResourceReference
            = new LESSObjectPropertyResourceReference(objectPropertyReference, this.entityReferenceSerializer,
            this.bridge, null);

        // Mock
        when(this.bridge.getProperty(objectPropertyReference)).thenReturn("content");

        // Test
        assertEquals("content", lessObjectPropertyResourceReference.getContent("skin"));
    }

    @Test
    void serialize()
    {
        ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference("property",
            new ObjectReference("class", new DocumentReference("wiki", "Space", "Document")));
        LESSObjectPropertyResourceReference lessObjectPropertyResourceReference
            = new LESSObjectPropertyResourceReference(objectPropertyReference, this.entityReferenceSerializer,
            this.bridge, null);

        // Mock
        when(this.entityReferenceSerializer.serialize(objectPropertyReference)).thenReturn("objPropertyRef");

        // Test
        assertEquals("LessXObjectProperty[objPropertyRef]", lessObjectPropertyResourceReference.serialize());
    }

    @Test
    void getDocumentReference()
    {
        LESSObjectPropertyResourceReference lessObjectPropertyResourceReference =
            new LESSObjectPropertyResourceReference(mock(ObjectPropertyReference.class),
                this.entityReferenceSerializer, this.bridge, null);

        DocumentReference pageDocumentReference = new DocumentReference("xwiki", "Space", "Page");
        when(this.bridge.getCurrentDocumentReference()).thenReturn(pageDocumentReference);

        DocumentReference documentReference = lessObjectPropertyResourceReference.getDocumentReference();
        verify(this.bridge).getCurrentDocumentReference();
        assertSame(pageDocumentReference, documentReference);
    }

    @Test
    void getAuthorReference() throws Exception
    {
        var pageDocumentReference = new DocumentReference("xwiki", "Space", "Page");
        var authorDocumentReference = new DocumentReference("xwiki", "Space", "Page");

        var documentModelBridge = mock(DocumentModelBridge.class);
        var documentAuthors = mock(DocumentAuthors.class);
        var userReference = mock(UserReference.class);
        var userReferenceSerializer = mock(UserReferenceSerializer.class);

        when(this.bridge.getCurrentDocumentReference()).thenReturn(pageDocumentReference);
        when(this.bridge.getDocumentInstance(pageDocumentReference)).thenReturn(documentModelBridge);
        when(documentModelBridge.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getEffectiveMetadataAuthor()).thenReturn(userReference);
        when(userReferenceSerializer.serialize(userReference)).thenReturn(authorDocumentReference);

        var lessObjectPropertyResourceReference =
            new LESSObjectPropertyResourceReference(mock(ObjectPropertyReference.class),
                this.entityReferenceSerializer, this.bridge, userReferenceSerializer);

        DocumentReference authorReference = lessObjectPropertyResourceReference.getAuthorReference();
        // This wouldn't return the right document author, we need to use the original metadata author.
        verify(this.bridge, never()).getCurrentAuthorReference();
        assertSame(authorDocumentReference, authorReference);
    }

    @Test
    void getAuthorReferenceException() throws Exception
    {
        var pageDocumentReference = new DocumentReference("xwiki", "Space", "Page");

        var userReferenceSerializer = mock(UserReferenceSerializer.class);

        when(this.bridge.getCurrentDocumentReference()).thenReturn(pageDocumentReference);
        when(this.bridge.getDocumentInstance(pageDocumentReference)).thenThrow(Exception.class);

        var lessObjectPropertyResourceReference =
            new LESSObjectPropertyResourceReference(mock(ObjectPropertyReference.class),
                this.entityReferenceSerializer, this.bridge, userReferenceSerializer);

        var exception = assertThrows(LESSCompilerException.class,
            lessObjectPropertyResourceReference::getAuthorReference);
        // This wouldn't return the right document author, we need to use the original metadata author.
        verify(this.bridge, never()).getCurrentAuthorReference();
        assertEquals("Failed to get the document from document reference [xwiki:Space.Page]", exception.getMessage());
    }
}
