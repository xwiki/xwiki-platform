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
package org.xwiki.store.serialization.xml.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xpn.xwiki.doc.XWikiAttachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AttachmentListMetadataSerializer
 *
 * @version $Id$
 * @since 3.0M2
 */
class AttachmentListMetadataSerializerTest
{
    private static final String TEST_CONTENT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<attachment-list serializer=\"attachment-list-meta/1.0\">\n"
      + " <attachment serializer=\"attachment-meta/1.0\">\n"
      + "  <filename>file1</filename>\n"
      + "  <filesize>10</filesize>\n"
      + "  <author>me</author>\n"
      + "  <version>1.1</version>\n"
      + "  <comment>something whitty</comment>\n"
      + "  <date>1293045632168</date>\n"
      + " </attachment>\n"
      + " <attachment serializer=\"attachment-meta/1.0\">\n"
      + "  <filename>file1</filename>\n"
      + "  <filesize>5</filesize>\n"
      + "  <author>you</author>\n"
      + "  <version>1.2</version>\n"
      + "  <comment>a comment</comment>\n"
      + "  <date>1293789456868</date>\n"
      + " </attachment>\n"
      + " <attachment serializer=\"attachment-meta/1.0\">\n"
      + "  <filename>file1</filename>\n"
      + "  <filesize>20</filesize>\n"
      + "  <author>them</author>\n"
      + "  <version>1.3</version>\n"
      + "  <comment>i saved it</comment>\n"
      + "  <date>1293012345668</date>\n"
      + " </attachment>\n"
      + "</attachment-list>";

    private AttachmentListMetadataSerializer serializer;

    @BeforeEach
    void setUp()
    {
        this.serializer = new AttachmentListMetadataSerializer(new AttachmentMetadataSerializer());
    }

    @Test
    void parse() throws Exception
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes("US-ASCII"));
        final List<XWikiAttachment> attachList = this.serializer.parse(bais);
        bais.close();
        assertTrue(3 == attachList.size(), "Attachment list was wrong size");

        assertEquals("file1", attachList.get(0).getFilename(), "Attachment1 had wrong name");
        assertEquals("file1", attachList.get(1).getFilename(), "Attachment2 had wrong name");
        assertEquals("file1", attachList.get(2).getFilename(), "Attachment3 had wrong name");

        assertEquals("me", attachList.get(0).getAuthor(), "Attachment1 had wrong author");
        assertEquals("you", attachList.get(1).getAuthor(), "Attachment2 had wrong author");
        assertEquals("them", attachList.get(2).getAuthor(), "Attachment3 had wrong author");

        assertEquals("1.1", attachList.get(0).getVersion(), "Attachment1 had wrong version");
        assertEquals("1.2", attachList.get(1).getVersion(), "Attachment2 had wrong version");
        assertEquals("1.3", attachList.get(2).getVersion(), "Attachment3 had wrong version");

        assertEquals("something whitty", attachList.get(0).getComment(), "Attachment1 had wrong comment");
        assertEquals("a comment", attachList.get(1).getComment(), "Attachment2 had wrong comment");
        assertEquals("i saved it", attachList.get(2).getComment(), "Attachment3 had wrong comment");

        // We drop milliseconds for consistency with the database so last 3 digits are 0.
        assertEquals("1293045632000", attachList.get(0).getDate().getTime() + "", "Attachment1 had wrong date.");
        assertEquals("1293789456000", attachList.get(1).getDate().getTime() + "", "Attachment2 had wrong date.");
        assertEquals("1293012345000", attachList.get(2).getDate().getTime() + "", "Attachment3 had wrong date.");
    }

    @Test
    void parseSerialize() throws Exception
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes("US-ASCII"));
        final List<XWikiAttachment> attachList = this.serializer.parse(bais);
        bais.close();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(this.serializer.serialize(attachList), baos);
        final String test = new String(baos.toByteArray(), "US-ASCII");
        final String control = TEST_CONTENT.replaceAll("[0-9][0-9][0-9]</date>", "000</date>");
        assertEquals(control, test, "Parsing and serializing yields a different output.");
    }
}
