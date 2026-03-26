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
package com.xpn.xwiki.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.VoidAttachmentVersioningStore.VoidAttachmentArchive;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link VoidAttachmentVersioningStore} and {@link VoidAttachmentArchive}.
 *
 * @version $Id$
 */
@OldcoreTest
class VoidAttachmentVersioningStoreTest
{
    @InjectMockComponents
    private VoidAttachmentVersioningStore store;

    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @BeforeEach
    void setUp()
    {
        this.oldCore.getSpyXWiki().setDefaultAttachmentArchiveStore(this.store);
    }

    @Test
    void store() throws XWikiException
    {
        // is store correctly inited?
        assertEquals(VoidAttachmentVersioningStore.class, this.store.getClass());
        // create doc, attachment & attachment archive
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Main", "Test"));
        XWikiAttachment attachment = new XWikiAttachment(doc, "filename");
        attachment.setContent(new byte[] { 1 });

        attachment.updateContentArchive(this.oldCore.getXWikiContext());
        // is archive correctly inited and cloneable?
        this.store.saveArchive(attachment.getAttachment_archive(), this.oldCore.getXWikiContext(), true);
        XWikiAttachmentArchive archive = this.store.loadArchive(attachment, this.oldCore.getXWikiContext(), false);
        assertEquals(VoidAttachmentArchive.class, archive.getClass());
        assertEquals(VoidAttachmentArchive.class, archive.clone().getClass());
        assertEquals(archive, this.store.loadArchive(attachment, this.oldCore.getXWikiContext(), true));

        this.store.deleteArchive(attachment, this.oldCore.getXWikiContext(), true);
    }

    @Test
    void history() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Main", "Test"));
        XWikiAttachment attachment = new XWikiAttachment(doc, "filename");
        // 1.1
        attachment.setContent(new byte[] { 1 });
        attachment.updateContentArchive(this.oldCore.getXWikiContext());
        assertEquals(attachment, attachment.getAttachmentRevision("1.1", this.oldCore.getXWikiContext()));
        // 1.2
        attachment.setContent(new byte[] { 2 });
        attachment.updateContentArchive(this.oldCore.getXWikiContext());
        assertEquals(attachment, attachment.getAttachmentRevision("1.2", this.oldCore.getXWikiContext()));
        // there should be only 1.2 version.
        assertNull(attachment.getAttachmentRevision("1.1", this.oldCore.getXWikiContext()));
        assertNull(attachment.getAttachmentRevision("1.3", this.oldCore.getXWikiContext()));
        assertEquals(1, attachment.getVersions().length);
        assertEquals(new Version(1, 2), attachment.getVersions()[0]);
    }
}
