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

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Tests for AttachmentMetadataSerializer
 *
 * @version $Id$
 * @since 3.0M2
 */
public class AttachmentMetadataSerializerTest
{
    private static final String TEST_CONTENT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
      + "<attachment serializer=\"attachment-meta/1.0\">\n"
      + " <filename>file1</filename>\n"
      + " <filesize>10</filesize>\n"
      + " <author>me</author>\n"
      + " <version>1.1</version>\n"
      + " <comment>something whitty</comment>\n"
      + " <date>1293045632168</date>\n"
      + "</attachment>";

    private AttachmentMetadataSerializer serializer;

    @Before
    public void setUp()
    {
        this.serializer = new AttachmentMetadataSerializer();
    }

    @Test
    public void testParse() throws Exception
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes("US-ASCII"));
        final XWikiAttachment attach = this.serializer.parse(bais);
        bais.close();

        Assert.assertEquals("Attachment1 had wrong name", "file1", attach.getFilename());
        Assert.assertEquals("Attachment1 had wrong author", "me", attach.getAuthor());
        Assert.assertEquals("Attachment1 had wrong version", "1.1", attach.getVersion());
        Assert.assertEquals("Attachment1 had wrong comment",
            attach.getComment(),
            "something whitty");
        // We drop milliseconds for consistency with the database so last 3 digits are 0.
        Assert.assertEquals("Attachment1 had wrong date.",
            attach.getDate().getTime() + "",
            "1293045632000");
    }

    @Test
    public void testParseSerialize() throws Exception
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(TEST_CONTENT.getBytes("US-ASCII"));
        final XWikiAttachment attach = this.serializer.parse(bais);
        bais.close();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(this.serializer.serialize(attach), baos);
        final String test = new String(baos.toByteArray(), "US-ASCII");
        final String control = TEST_CONTENT.replaceAll("[0-9][0-9][0-9]</date>", "000</date>");
        Assert.assertEquals("Parsing and serializing yields a different output.", control, test);
    }
}
