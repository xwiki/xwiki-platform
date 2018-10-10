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
package org.xwiki.store.legacy.doc.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link ListAttachmentArchive}.
 * 
 * @version $Id$
 */
public class ListAttachmentArchiveTest
{
    @Test
    public void testSort()
    {
        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));

        XWikiAttachment attachment11 = new XWikiAttachment(document, "file1");
        attachment11.setVersion("1.1");
        XWikiAttachment attachment12 = new XWikiAttachment(document, "file1");
        attachment12.setVersion("1.2");
        XWikiAttachment attachment31 = new XWikiAttachment(document, "file1");
        attachment31.setVersion("3.1");
        XWikiAttachment attachment41 = new XWikiAttachment(document, "file1");
        attachment41.setVersion("4.1");

        List<XWikiAttachment> attachments = new ArrayList<>();
        attachments.add(attachment41);
        attachments.add(attachment11);
        attachments.add(attachment12);
        attachments.add(attachment31);

        ListAttachmentArchive archive = new ListAttachmentArchive(attachments);

        Version[] versions = archive.getVersions();

        assertEquals("1.1", versions[0].toString());
        assertEquals("1.2", versions[1].toString());
        assertEquals("3.1", versions[2].toString());
        assertEquals("4.1", versions[3].toString());
    }
}
