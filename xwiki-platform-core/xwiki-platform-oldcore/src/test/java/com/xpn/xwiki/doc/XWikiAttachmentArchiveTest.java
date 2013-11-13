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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Unit tests for {@link XWikiAttachmentArchive}.
 * 
 * @version $Id$
 */
public class XWikiAttachmentArchiveTest
{
    /**
     * The object being tested.
     */
    private XWikiAttachmentArchive archive = new XWikiAttachmentArchive();

    private XWikiAttachment attachment = mock(XWikiAttachment.class);

    @Before
    public void setUp()
    {
        archive.setAttachment(attachment);
    }

    @Test
    public void getVersionsWhenThereIsNoHistory()
    {
        Version version = new Version(3, 4);
        when(attachment.getRCSVersion()).thenReturn(version);

        assertArrayEquals(new Version[] {version}, archive.getVersions());
    }

    @Test
    public void getVersions() throws Exception
    {
        Archive rcsArchive = new Archive(new Object[] {"line"}, "file.txt", "5.2");
        rcsArchive.addRevision(new Object[] {"line modified"}, "");
        archive.setRCSArchive(rcsArchive);

        assertArrayEquals(new Version[] {new Version(5, 2), new Version(5, 3)}, archive.getVersions());
    }

    @Test
    public void getCurrentRevisionWhenThereIsNoHistory() throws Exception
    {
        String version = "1.3";
        when(attachment.getVersion()).thenReturn(version);

        assertEquals(attachment, archive.getRevision(null, version, null));
    }

    @Test
    public void getRevisionWhenThereIsNoHistory() throws Exception
    {
        when(attachment.getVersion()).thenReturn("1.1");

        assertNull(archive.getRevision(null, "2.7", null));
    }

    @Test
    public void getRevisionWhichDoesNotExist() throws Exception
    {
        archive.setRCSArchive(new Archive(new Object[] {"text"}, "file.txt", "1.2"));

        assertNull(archive.getRevision(null, "7.2", null));
    }
}
