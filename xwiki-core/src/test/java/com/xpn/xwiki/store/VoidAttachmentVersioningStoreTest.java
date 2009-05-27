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

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.VoidAttachmentVersioningStore.VoidAttachmentArchive;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link VoidAttachmentVersioningStore} and {@link VoidAttachmentArchive}.
 * 
 * @version $Id$
 */
public class VoidAttachmentVersioningStoreTest extends AbstractBridgedXWikiComponentTestCase
{
    AttachmentVersioningStore store;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        XWiki xwiki = new XWiki();
        getContext().setWiki(xwiki);
        
        this.store = getComponentManager().lookup(AttachmentVersioningStore.class, "void");
        xwiki.setAttachmentVersioningStore(this.store);
    }

    public void testStore() throws XWikiException
    {
        // is store correctly inited?
        assertEquals(VoidAttachmentVersioningStore.class, this.store.getClass());
        // create doc, attachment & attachment archive
        XWikiDocument doc = new XWikiDocument("Main", "Test");
        XWikiAttachment attachment = new XWikiAttachment(doc, "filename");
        attachment.setContent(new byte[] {1});
        attachment.updateContentArchive(getContext());
        // is archive correctly inited and cloneable?
        this.store.saveArchive(attachment.getAttachment_archive(), this.getContext(), true);
        XWikiAttachmentArchive archive = this.store.loadArchive(attachment, this.getContext(), false);
        assertEquals(VoidAttachmentArchive.class, archive.getClass());
        assertEquals(VoidAttachmentArchive.class, archive.clone().getClass());
        assertEquals(archive, this.store.loadArchive(attachment, this.getContext(), true));

        this.store.deleteArchive(attachment, getContext(), true);
    }

    public void testHistory() throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument("Main", "Test");
        XWikiAttachment attachment = new XWikiAttachment(doc, "filename");
        // 1.1
        attachment.setContent(new byte[] {1});
        attachment.updateContentArchive(this.getContext());
        assertEquals(attachment, attachment.getAttachmentRevision("1.1", this.getContext()));
        // 1.2
        attachment.setContent(new byte[] {2});
        attachment.updateContentArchive(this.getContext());
        assertEquals(attachment, attachment.getAttachmentRevision("1.2", this.getContext()));
        // there should be only 1.2 version.
        assertNull(attachment.getAttachmentRevision("1.1", this.getContext()));
        assertNull(attachment.getAttachmentRevision("1.3", this.getContext()));
        assertEquals(1, attachment.getVersions().length);
        assertEquals(new Version(1, 2), attachment.getVersions()[0]);
    }
}
