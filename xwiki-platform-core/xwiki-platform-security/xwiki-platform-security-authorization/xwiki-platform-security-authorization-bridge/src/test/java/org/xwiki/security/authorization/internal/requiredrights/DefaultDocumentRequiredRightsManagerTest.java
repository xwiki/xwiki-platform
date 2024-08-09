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
package org.xwiki.security.authorization.internal.requiredrights;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link DefaultDocumentRequiredRightsManager}.
 *
 * @version $Id$
 */
@OldcoreTest
class DefaultDocumentRequiredRightsManagerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @InjectMockComponents
    private DefaultDocumentRequiredRightsManager documentRequiredRightsManager;

    @MockComponent
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    @Test
    void existingDocument() throws XWikiException, AuthorizationException
    {
        XWikiDocument document =
            this.mockitoOldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.mockitoOldcore.getXWikiContext());
        this.mockitoOldcore.getSpyXWiki().saveDocument(document, this.mockitoOldcore.getXWikiContext());

        DocumentRequiredRights documentRequiredRights = mock();
        when(this.documentRequiredRightsReader.readRequiredRights(document)).thenReturn(documentRequiredRights);

        assertEquals(documentRequiredRights,
            this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());
    }

    @Test
    void missingDocument() throws AuthorizationException
    {
        assertTrue(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).isEmpty());
        verifyNoInteractions(this.documentRequiredRightsReader);
    }

    @Test
    void failedLoad() throws XWikiException
    {
        XWikiException expected = mock();
        when(this.mockitoOldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.mockitoOldcore.getXWikiContext()))
            .thenThrow(expected);

        AuthorizationException actual = assertThrows(AuthorizationException.class,
            () -> this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE));

        assertEquals("Failed to load the document", actual.getMessage());
        assertEquals(expected, actual.getCause());
    }
}
