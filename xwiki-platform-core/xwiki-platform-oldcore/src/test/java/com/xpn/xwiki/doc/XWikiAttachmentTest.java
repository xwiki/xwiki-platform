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

import java.util.Random;
import java.io.InputStream;

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.Assert;

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
        Assert.assertEquals("Version list was not one element long for version 1.1", 1,
                            attach.getVersionList().size());
        attach.setVersion("1.2");
        Assert.assertEquals("Version list was not two elements long for version 1.2.", 2,
                            attach.getVersionList().size());
        attach.setVersion("1.3");
        Assert.assertEquals("Version list was not two elements long for version 1.3.", 3,
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
        Assert.assertEquals("Not all of the stream was read", 0, ris.available());
        Assert.assertTrue(
            IOUtils.contentEquals(new RandomInputStream(attachLength, seed),
                                  attach.getAttachment_content().getContentInputStream()));
    }

    /** An InputStream which will return a stream of zeros of length given in the constructor. */
    private static class RandomInputStream extends InputStream
    {
        private int bytes;

        private int state;

        public RandomInputStream(final int bytes, final int seed)
        {
            this.bytes = bytes;
            this.state = seed;
        }

        public int available()
        {
            return this.bytes;
        }

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
