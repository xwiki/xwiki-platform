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
package com.xpn.xwiki.store.hibernate;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HibernateAttachmentRecycleBinStore}.
 *
 * @version $Id$
 */
@ComponentTest
class HibernateAttachmentRecycleBinStoreTest
{
    @InjectMockComponents
    private HibernateAttachmentRecycleBinStore store;

    @Test
    void restoreFromRecycleBinWhenNoAttachmentWithIndexFound() throws Exception
    {
        HibernateAttachmentRecycleBinStore spyStore = spy(this.store);
        doReturn(null).when(spyStore).getDeletedAttachment(anyLong(), any(XWikiContext.class), anyBoolean());

        // Verify we return null when no attachment is found.
        assertNull(spyStore.restoreFromRecycleBin(null, 0, null, false));
    }

    @Test
    void restoreFromRecycleBinWhenFilenameDoesNotMatch() throws Exception
    {
        DeletedAttachment deletedAttachment = mock(DeletedAttachment.class);
        when(deletedAttachment.getFilename()).thenReturn("otherfile.txt");

        HibernateAttachmentRecycleBinStore spyStore = spy(this.store);
        doReturn(deletedAttachment).when(spyStore).getDeletedAttachment(anyLong(), any(XWikiContext.class),
            anyBoolean());

        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiAttachment attachment = new XWikiAttachment(document, "file.txt");

        // The deleted attachment found at that index belongs to a different filename: it must not be restored.
        assertNull(spyStore.restoreFromRecycleBin(attachment, 0, null, false));
        verify(deletedAttachment, never()).restoreAttachment();
    }

    @Test
    void restoreFromRecycleBinWhenDocumentDoesNotMatch() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiDocument otherDocument = new XWikiDocument(new DocumentReference("wiki", "space", "otherpage"));

        DeletedAttachment deletedAttachment = mock(DeletedAttachment.class);
        when(deletedAttachment.getFilename()).thenReturn("file.txt");
        when(deletedAttachment.getDocId()).thenReturn(otherDocument.getId());

        HibernateAttachmentRecycleBinStore spyStore = spy(this.store);
        doReturn(deletedAttachment).when(spyStore).getDeletedAttachment(anyLong(), any(XWikiContext.class),
            anyBoolean());

        XWikiAttachment attachment = new XWikiAttachment(document, "file.txt");

        // The deleted attachment found at that index has the right filename but belonged to another document: it
        // must not be restored, so that a valid recycle bin id can't be used to access attachments of an
        // unrelated (e.g. more protected) document.
        assertNull(spyStore.restoreFromRecycleBin(attachment, 0, null, false));
        verify(deletedAttachment, never()).restoreAttachment();
    }

    @Test
    void restoreFromRecycleBinWhenFilenameAndDocumentMatch() throws Exception
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        XWikiAttachment attachment = new XWikiAttachment(document, "file.txt");
        XWikiAttachment restoredAttachment = new XWikiAttachment(document, "file.txt");

        DeletedAttachment deletedAttachment = mock(DeletedAttachment.class);
        when(deletedAttachment.getFilename()).thenReturn("file.txt");
        when(deletedAttachment.getDocId()).thenReturn(document.getId());
        when(deletedAttachment.restoreAttachment()).thenReturn(restoredAttachment);

        HibernateAttachmentRecycleBinStore spyStore = spy(this.store);
        doReturn(deletedAttachment).when(spyStore).getDeletedAttachment(anyLong(), any(XWikiContext.class),
            anyBoolean());

        // The deleted attachment found at that index matches both the filename and the owning document: it must
        // be restored.
        assertSame(restoredAttachment, spyStore.restoreFromRecycleBin(attachment, 0, null, false));
    }
}
