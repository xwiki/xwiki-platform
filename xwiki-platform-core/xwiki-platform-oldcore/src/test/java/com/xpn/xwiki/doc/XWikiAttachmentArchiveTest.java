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
package com.xpn.xwiki.doc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Unit tests for {@link XWikiAttachmentArchive}.
 * 
 * @version $Id$
 */
class XWikiAttachmentArchiveTest
{
    /**
     * The object being tested.
     */
    private XWikiAttachmentArchive archive = new XWikiAttachmentArchive();

    private XWikiAttachment attachment = mock(XWikiAttachment.class);

    @BeforeEach
    void setUp()
    {
        this.archive.setAttachment(this.attachment);
    }

    @Test
    void getVersionsWhenThereIsNoHistory()
    {
        Version version = new Version(3, 4);
        when(this.attachment.getRCSVersion()).thenReturn(version);

        assertArrayEquals(new Version[] {version}, this.archive.getVersions());
    }

    @Test
    void getVersions() throws Exception
    {
        Archive rcsArchive = new Archive(new Object[] {"line"}, "file.txt", "5.2");
        rcsArchive.addRevision(new Object[] {"line modified"}, "");
        this.archive.setRCSArchive(rcsArchive);

        assertArrayEquals(new Version[] {new Version(5, 2), new Version(5, 3)}, this.archive.getVersions());
    }

    @Test
    void getCurrentRevisionWhenThereIsNoHistory() throws Exception
    {
        String version = "1.3";
        when(this.attachment.getVersion()).thenReturn(version);

        assertEquals(this.attachment, this.archive.getRevision(null, version, null));
    }

    @Test
    void getRevisionWhenThereIsNoHistory() throws Exception
    {
        when(this.attachment.getVersion()).thenReturn("1.1");

        assertNull(this.archive.getRevision(null, "2.7", null));
    }

    @Test
    void getRevisionWhichDoesNotExist() throws Exception
    {
        this.archive.setRCSArchive(new Archive(new Object[] {"text"}, "file.txt", "1.2"));

        assertNull(this.archive.getRevision(null, "7.2", null));
    }
}
