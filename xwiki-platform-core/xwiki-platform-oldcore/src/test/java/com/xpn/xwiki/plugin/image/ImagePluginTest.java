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
package com.xpn.xwiki.plugin.image;

import java.io.File;

import org.jmock.Mock;
import org.xwiki.cache.CacheFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.image.ImagePlugin} class.
 * 
 * @version $Id$
 */
public class ImagePluginTest extends AbstractBridgedXWikiComponentTestCase
{
    private ImagePlugin plugin;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        Mock mockXWiki = mock(XWiki.class);
        mockXWiki.stubs().method("getTempDirectory").will(returnValue(new File(System.getProperty("java.io.tmpdir"))));
        mockXWiki.stubs().method("Param").will(returnValue("10"));
        Mock mockCacheFactory = mock(CacheFactory.class);
        mockCacheFactory.expects(once()).method("newCache");
        mockXWiki.stubs().method("getLocalCacheFactory").will(returnValue(mockCacheFactory.proxy()));
        getContext().setWiki((XWiki) mockXWiki.proxy());
        this.plugin = new ImagePlugin("image", ImagePlugin.class.getName(), getContext());
    }

    public void testDownloadAttachmentWithUnsupportedFileType()
    {
        Mock attachmentMock = mock(XWikiAttachment.class);
        attachmentMock.stubs().method("getMimeType").will(returnValue("image/notsupported"));
        XWikiAttachment attachment = (XWikiAttachment) attachmentMock.proxy();
        assertSame(attachment, plugin.downloadAttachment(attachment, new XWikiContext()));
    }
}
