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

import java.io.InputStream;
import java.io.OutputStream;

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link XWikiAttachment}.
 *
 * @version $Id$
 */
public class XWikiAttachmentTest extends AbstractBridgedComponentTestCase
{
    @Test
    public void testGetVersionList() throws Exception
    {
        final XWikiAttachment attach = new XWikiAttachment();
        attach.setVersion("1.1");
        assertEquals("Version list was not one element long for version 1.1", 1,
                            attach.getVersionList().size());
        attach.setVersion("1.2");
        assertEquals("Version list was not two elements long for version 1.2.", 2,
                            attach.getVersionList().size());
        attach.setVersion("1.3");
        assertEquals("Version list was not two elements long for version 1.3.", 3,
                            attach.getVersionList().size());
    }

    /**
     * Create an attachment, populate it with enough data to make it flush to disk cache,
     * read back data and make sure it's the same.
     */
    @Test
    public void testStoreContentInDiskCache() throws Exception
    {
        int attachLength = 20000;
        // Check for data dependent errors.
        int seed = (int) System.currentTimeMillis();
        final XWikiAttachment attach = new XWikiAttachment();
        final InputStream ris = new RandomInputStream(attachLength, seed);
        attach.setContent(ris);
        assertEquals("Not all of the stream was read", 0, ris.available());
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed),
            attach.getAttachment_content().getContentInputStream()));
    }

    @Test
    public void testSetContentViaOutputStream() throws Exception
    {
        int attachLength = 20;
        int seed = (int) System.currentTimeMillis();
        final XWikiAttachment attach = new XWikiAttachment();
        final InputStream ris = new RandomInputStream(attachLength, seed);
        attach.setContent(ris);
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed),
            attach.getAttachment_content().getContentInputStream()));
        // Now write to the attachment via an OutputStream.
        final XWikiAttachmentContent xac = attach.getAttachment_content();
        xac.setContentDirty(false);
        final OutputStream os = xac.getContentOutputStream();

        // Adding content with seed+1 will make a radically different set of content.
        IOUtils.copy(new RandomInputStream(attachLength, seed + 1), os);

        // It should still be the old content.
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed), xac.getContentInputStream()));
        assertFalse(xac.isContentDirty());

        os.close();

        // Now it should be the new content.
        assertTrue(IOUtils.contentEquals(new RandomInputStream(attachLength, seed + 1), xac.getContentInputStream()));
        assertTrue(xac.isContentDirty());
    }

    /**
     * Unit test for <a href="http://jira.xwiki.org/browse/XWIKI-9075">XWIKI-9075</a> to prove that calling
     * {@code fromXML} doesn't set the metadata dirty flag.
     * <p/>
     * Note: I think there's a bug in that fromXML should return a new instance of XWikiAttachment and not modify the
     * current one as this would mean changing its identity...
     */
    @Test
    public void fromXMLShouldntSetMetaDataAsDirty() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.fromXML("<attachment>\n"
            + "<filename>XWikiLogo.png</filename>\n"
            + "<filesize>1390</filesize>\n"
            + "<author>xwiki:XWiki.Admin</author>\n"
            + "<date>1252454400000</date>\n"
            + "<version>1.1</version>\n"
            + "<comment/>\n"
            + "<content>content</content>\n"
            + "</attachment>");
        assertFalse(attachment.isMetaDataDirty());
    }

    /** An InputStream which will return a stream of random bytes of length given in the constructor. */
    private static class RandomInputStream extends InputStream
    {
        private int bytes;

        private int state;

        public RandomInputStream(final int bytes, final int seed)
        {
            this.bytes = bytes;
            this.state = seed;
        }

        @Override
        public int available()
        {
            return this.bytes;
        }

        @Override
        public int read()
        {
            if (this.bytes == 0) {
                return -1;
            }
            this.bytes--;
            this.state = this.state << 13 | this.state >>> 19;
            return ++this.state & 0xff;
        }
    }
}
