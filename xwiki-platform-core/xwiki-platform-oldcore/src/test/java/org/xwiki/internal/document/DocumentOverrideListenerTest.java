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
package org.xwiki.internal.document;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DocumentOverrideListener}.
 *
 * @version $Id$
 */
@OldcoreTest(mockXWiki = false)
@ReferenceComponentList
@ComponentList({ DefaultObservationManager.class, DocumentOverrideListener.class })
class DocumentOverrideListenerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "XWiki", "user");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Test
    void savingNewDocument() throws Exception
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.oldcore.getSpyXWiki().checkSavingDocument(USER_REFERENCE, document, this.oldcore.getXWikiContext());
    }

    @Test
    void savingExistingDocument() throws Exception
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
        document = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        assertFalse(document.isNew());
        this.oldcore.getSpyXWiki().checkSavingDocument(USER_REFERENCE, document, this.oldcore.getXWikiContext());
    }

    @Test
    void savingOverridingExistingDocument() throws Exception
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
        XWikiDocument newDocument = new XWikiDocument(DOCUMENT_REFERENCE);
        XWikiException exception = assertThrows(XWikiException.class,
            () -> this.oldcore.getSpyXWiki()
                .checkSavingDocument(USER_REFERENCE, newDocument, this.oldcore.getXWikiContext()));
        assertEquals("Error number 9001 in 9: User [wiki:XWiki.user] has been denied the right to save the "
            + "document [wiki:space.page]. Reason: [The document already exists but the document to be saved is marked "
            + "as new.]", exception.getMessage());
    }
}
