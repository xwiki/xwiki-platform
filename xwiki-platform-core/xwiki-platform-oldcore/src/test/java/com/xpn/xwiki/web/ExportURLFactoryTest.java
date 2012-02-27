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
package com.xpn.xwiki.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jmock.Mock;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public class ExportURLFactoryTest extends AbstractBridgedXWikiComponentTestCase
{
    private Mock mockXWiki;

    /** Temporary directory where to put exported files. Will be deleted at the end of the test. */
    private File tmpDir;

    /** The tested instance. */
    private ExportURLFactory urlFactory = new ExportURLFactory();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("getWebAppPath").will(returnValue("/xwiki"));
        this.mockXWiki.stubs().method("Param").will(returnValue(null));
        getContext().setWiki((XWiki) this.mockXWiki.proxy());
        getContext().setURL(new URL("http://www.xwiki.org/"));

        // The URLFactory uses a request to determine the values for the context and servlet path.
        Mock mockXWikiResquest = mock(XWikiRequest.class, new Class[] {}, new Object[] {});
        mockXWikiResquest.stubs().method("getScheme").will(returnValue("http"));
        mockXWikiResquest.stubs().method("isSecure").will(returnValue(false));
        mockXWikiResquest.stubs().method("getServletPath").will(returnValue("/bin"));
        mockXWikiResquest.stubs().method("getContextPath").will(returnValue("/xwiki"));
        mockXWikiResquest.stubs().method("getHeader").will(returnValue(null));
        getContext().setRequest((XWikiRequest) mockXWikiResquest.proxy());

        // Since the ExportURLFactory saves requested attachments to the disk, create a temporary folder to hold these
        // files, which will be deleted after the test ends.
        this.tmpDir = new File(System.getProperty("java.io.tmpdir"), "xwikitests");
        this.tmpDir.mkdirs();
        new File(this.tmpDir, "attachment").mkdir();

        this.urlFactory.init(null, this.tmpDir, getContext());
    }

    /**
     * Test that
     * {@link ExportURLFactory#createAttachmentURL(String, String, String, String, String, com.xpn.xwiki.XWikiContext)}
     * correctly escapes spaces into %20 when the exported document contains spaces in its name.
     */
    public void testCreateAttachmentURL() throws Exception
    {
        // Prepare the exported document and attachment.
        XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", " Space ", "New  Page"));
        XWikiAttachment attachment = new XWikiAttachment(doc, "img .jpg");
        attachment.setContent(new ByteArrayInputStream("test".getBytes()));
        doc.getAttachmentList().add(attachment);
        this.mockXWiki.stubs().method("getDocument").will(returnValue(doc));

        URL url = this.urlFactory.createAttachmentURL("img .jpg", " Space ", "Pa ge", "view", "", "x", getContext());
        assertEquals(new URL("file://attachment/x.%20Space%20.Pa%20ge.img%20.jpg"), url);
    }

    /** When the test is over, delete the folder where the exported attachments were placed. */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        FileUtils.deleteDirectory(this.tmpDir);
    }
}
