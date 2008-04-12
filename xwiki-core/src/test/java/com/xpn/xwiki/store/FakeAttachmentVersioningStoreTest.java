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
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.FakeAttachmentVersioningStore.FakeAttachmentArchive;

import junit.framework.TestCase;

/**
 * Unit tests for {@link FakeAttachmentVersioningStore} and {@link FakeAttachmentArchive}.
 * 
 * @version $Id: $
 */
public class FakeAttachmentVersioningStoreTest extends TestCase
{
    XWikiContext context = new XWikiContext();
    XWiki xwiki;

    @Override
    protected void setUp() throws Exception
    {
        XWikiConfig config = new XWikiConfig();
        config.setProperty("xwiki.store.attachment.versioning", "0");
        xwiki = new XWiki(config, context);
    }

    public void testStore() throws XWikiException
    {
        assertEquals(FakeAttachmentVersioningStore.class, xwiki.getAttachmentVersioningStore().getClass());

        XWikiDocument doc = new XWikiDocument("Main", "Test");
        XWikiAttachment attachment = new XWikiAttachment(doc, "filename");
        attachment.setContent(new byte[] { 1 });
        attachment.updateContentArchive(context);

        xwiki.getAttachmentVersioningStore().saveArchive(attachment.getAttachment_archive(),
            context, true);
        XWikiAttachmentArchive archive = xwiki.getAttachmentVersioningStore()
            .loadArchive(attachment, context, false);
        assertEquals(FakeAttachmentArchive.class, archive.getClass());
        assertEquals(FakeAttachmentArchive.class, archive.clone().getClass());
        xwiki.getAttachmentVersioningStore().deleteArchive(attachment, context, true);
    }

    public void testHistory() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Main", "Test");
        XWikiAttachment attachment = new XWikiAttachment(doc, "filename");
        attachment.setContent(new byte[] { 1 });
        attachment.updateContentArchive(context);
        assertEquals(attachment, attachment.getAttachmentRevision("1.1", context));

        attachment.setContent(new byte[] { 2 });
        attachment.updateContentArchive(context);
        assertEquals(attachment, attachment.getAttachmentRevision("1.2", context));
        
        // there is only 1.2 version should be.
        assertNull(attachment.getAttachmentRevision("1.1", context));
        assertNull(attachment.getAttachmentRevision("1.3", context));
        assertEquals(1, attachment.getVersions().length);
        assertEquals(new Version(1,2), attachment.getVersions()[0]);
    }
}
