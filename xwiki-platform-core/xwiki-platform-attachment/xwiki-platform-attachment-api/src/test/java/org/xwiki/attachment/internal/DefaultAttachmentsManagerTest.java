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
package org.xwiki.attachment.internal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.SOURCE_NAME_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_LOCATION_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_NAME_FIELD;

/**
 * Test of {@link DefaultAttachmentsManager}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
class DefaultAttachmentsManagerTest
{
    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    public static final AttachmentReference ATTACHMENT_LOCATION =
        new AttachmentReference("file.txt", DOCUMENT_REFERENCE);

    @InjectMockComponents
    private DefaultAttachmentsManager attachmentsManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void availableDocumentDoesNotExists() throws Exception
    {
        assertFalse(this.attachmentsManager.available(ATTACHMENT_LOCATION));
        verifyNoInteractions(this.document);
    }

    @Test
    void availableDocumentHasAttachment() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getExactAttachment("file.txt")).thenReturn(mock(XWikiAttachment.class));
        assertFalse(this.attachmentsManager.available(ATTACHMENT_LOCATION));
    }

    @Test
    void availableDocumentHasNotAttachment() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getAttachment("file.txt")).thenReturn(null);
        assertTrue(this.attachmentsManager.available(ATTACHMENT_LOCATION));
    }

    @Test
    void getRedirectionDocumentDoesNotExists() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(null);
        assertEquals(Optional.empty(), this.attachmentsManager.getRedirection(ATTACHMENT_LOCATION));
    }

    @Test
    void getRedirectionNotFound() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE)).thenReturn(emptyList());
        assertEquals(Optional.empty(), this.attachmentsManager.getRedirection(ATTACHMENT_LOCATION));
        verifyNoInteractions(this.documentReferenceResolver);
    }

    @ParameterizedTest
    @MethodSource("getRedirectionSource")
    void getRedirection(List<BaseObject> xObjects) throws Exception
    {
        DocumentReference targetReference = new DocumentReference("xwiki", "Space", "Target");
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE))
            .thenReturn(xObjects);
        when(this.documentReferenceResolver.resolve("xwiki:Space.Target"))
            .thenReturn(targetReference);
        assertEquals(Optional.of(new AttachmentReference("newName.txt", targetReference)),
            this.attachmentsManager.getRedirection(ATTACHMENT_LOCATION));
    }

    public static Stream<Arguments> getRedirectionSource()
    {
        BaseObject redirectObj = mock(BaseObject.class);
        when(redirectObj.getStringValue(SOURCE_NAME_FIELD)).thenReturn("file.txt");
        when(redirectObj.getStringValue(TARGET_NAME_FIELD)).thenReturn("newName.txt");
        when(redirectObj.getStringValue(TARGET_LOCATION_FIELD)).thenReturn("xwiki:Space.Target");
        List<BaseObject> xObjectWithNullValues = new java.util.ArrayList<>();
        xObjectWithNullValues.add(null);
        xObjectWithNullValues.add(redirectObj);
        xObjectWithNullValues.add(null);
        return Stream.of(
            Arguments.of(List.of(redirectObj)),
            Arguments.of(xObjectWithNullValues)
        );
    }

    @Test
    void removeExistingRedirectionNoRedirection()
    {
        when(this.document.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE))
            .thenReturn(emptyList());
        assertFalse(this.attachmentsManager.removeExistingRedirection("file.txt", this.document));
    }

    @Test
    void removeExistingRedirection()
    {
        BaseObject redirection1 = mock(BaseObject.class);
        BaseObject redirection2 = mock(BaseObject.class);
        when(redirection1.getStringValue(SOURCE_NAME_FIELD)).thenReturn("file.txt");
        when(redirection2.getStringValue(SOURCE_NAME_FIELD)).thenReturn("file2.txt");
        when(this.document.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE))
            .thenReturn(asList(redirection1, redirection2));
        assertTrue(this.attachmentsManager.removeExistingRedirection("file.txt", this.document));
        verify(this.document).removeXObject(redirection1);
        verify(this.document, never()).removeXObject(redirection2);
    }

    @Test
    void removeExistingRedirectionWithNullXObjects()
    {
        BaseObject redirection1 = mock(BaseObject.class);
        BaseObject redirection2 = mock(BaseObject.class);
        when(redirection1.getStringValue(SOURCE_NAME_FIELD)).thenReturn("file.txt");
        when(redirection2.getStringValue(SOURCE_NAME_FIELD)).thenReturn("file2.txt");
        when(this.document.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE))
            .thenReturn(asList(null, redirection1, redirection2));
        assertTrue(this.attachmentsManager.removeExistingRedirection("file.txt", this.document));
        verify(this.document).removeXObject(redirection1);
        verify(this.document, never()).removeXObject(redirection2);
    }
}
