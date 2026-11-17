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
package com.xpn.xwiki.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeletedAttachment}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class DeletedAttachmentTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private com.xpn.xwiki.doc.DeletedAttachment internalDeletedAttachment;

    private DeletedAttachment deletedAttachment;

    private XWikiContext context;

    private DocumentReference documentReference;

    @BeforeEach
    void setUp()
    {
        this.context = this.oldcore.getXWikiContext();
        this.documentReference = new DocumentReference("wiki", "space", "page");
        this.context.setUserReference(new DocumentReference("wiki", "XWiki", "Alice"));

        this.internalDeletedAttachment = mock(com.xpn.xwiki.doc.DeletedAttachment.class);
        when(this.internalDeletedAttachment.getAttachmentReference())
            .thenReturn(new AttachmentReference("file.txt", this.documentReference));
        when(this.internalDeletedAttachment.getDocName()).thenReturn("space.page");

        this.deletedAttachment = new DeletedAttachment(this.internalDeletedAttachment, this.context);
    }

    @Test
    void getAttachmentWhenNoViewRightOnOwningDocument() throws Exception
    {
        when(this.oldcore.getMockAuthorizationManager().hasAccess(Right.VIEW, this.context.getUserReference(),
            this.documentReference)).thenReturn(false);

        // Without view right on the document the attachment belonged to, nothing must be restored or returned.
        assertNull(this.deletedAttachment.getAttachment());
        verify(this.internalDeletedAttachment, never()).restoreAttachment();
    }

    @Test
    void getAttachmentWhenViewRightOnOwningDocument() throws Exception
    {
        when(this.oldcore.getMockAuthorizationManager().hasAccess(Right.VIEW, this.context.getUserReference(),
            this.documentReference)).thenReturn(true);

        XWikiAttachment restoredAttachment = new XWikiAttachment();
        restoredAttachment.setFilename("file.txt");
        when(this.internalDeletedAttachment.restoreAttachment()).thenReturn(restoredAttachment);

        // With view right on the document, the deleted attachment must still be restored and returned.
        Attachment attachment = this.deletedAttachment.getAttachment();
        assertNotNull(attachment);
        verify(this.internalDeletedAttachment).restoreAttachment();
    }
}
