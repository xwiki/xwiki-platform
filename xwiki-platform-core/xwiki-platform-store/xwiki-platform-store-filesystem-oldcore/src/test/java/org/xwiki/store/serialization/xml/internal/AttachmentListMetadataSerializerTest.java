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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Tests for AttachmentListMetadataSerializer
 *
 * @version $Id$
 * @since 3.0M2
 */
public class AttachmentListMetadataSerializerTest
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

    @Before
    public void setUp()
    {
        this.serializer = new AttachmentListMetadataSerializer(new AttachmentMetadataSerializer());
    }

    @Test
    public void testParse() throws Exception
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes("US-ASCII"));
        final List<XWikiAttachment> attachList = this.serializer.parse(bais);
        bais.close();
        Assert.assertTrue("Attachment list was wrong size", 3 == attachList.size());

        Assert.assertEquals("Attachment1 had wrong name", "file1", attachList.get(0).getFilename());
        Assert.assertEquals("Attachment2 had wrong name", "file1", attachList.get(1).getFilename());
        Assert.assertEquals("Attachment3 had wrong name", "file1", attachList.get(2).getFilename());

        Assert.assertEquals("Attachment1 had wrong author", "me", attachList.get(0).getAuthor());
        Assert.assertEquals("Attachment2 had wrong author", "you", attachList.get(1).getAuthor());
        Assert.assertEquals("Attachment3 had wrong author", "them", attachList.get(2).getAuthor());

        Assert.assertEquals("Attachment1 had wrong version", "1.1", attachList.get(0).getVersion());
        Assert.assertEquals("Attachment2 had wrong version", "1.2", attachList.get(1).getVersion());
        Assert.assertEquals("Attachment3 had wrong version", "1.3", attachList.get(2).getVersion());

        Assert.assertEquals("Attachment1 had wrong comment",
            attachList.get(0).getComment(),
            "something whitty");
        Assert.assertEquals("Attachment2 had wrong comment", "a comment", attachList.get(1).getComment());
        Assert.assertEquals("Attachment3 had wrong comment", "i saved it", attachList.get(2).getComment());

        // We drop milliseconds for consistency with the database so last 3 digits are 0.
        Assert.assertEquals("Attachment1 had wrong date.",
            attachList.get(0).getDate().getTime() + "",
            "1293045632000");
        Assert.assertEquals("Attachment2 had wrong date.",
            attachList.get(1).getDate().getTime() + "",
            "1293789456000");
        Assert.assertEquals("Attachment3 had wrong date.",
            attachList.get(2).getDate().getTime() + "",
            "1293012345000");
    }

    @Test
    public void testParseSerialize() throws Exception
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes("US-ASCII"));
        final List<XWikiAttachment> attachList = this.serializer.parse(bais);
        bais.close();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(this.serializer.serialize(attachList), baos);
        final String test = new String(baos.toByteArray(), "US-ASCII");
        final String control = TEST_CONTENT.replaceAll("[0-9][0-9][0-9]</date>", "000</date>");
        Assert.assertEquals("Parsing and serializing yields a different output.", control, test);
    }
}
