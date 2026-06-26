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
import java.util.Arrays;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.url.filesystem.FilesystemExportContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ExportURLFactory}.
 *
 * @version $Id$
 */
@AllComponents
@OldcoreTest
class ExportURLFactoryTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @XWikiTempDir
    private File baseTmpDir;

    private ExportURLFactory urlFactory;

    private File tmpDir;

    private FilesystemExportContext exportContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.urlFactory = new ExportURLFactory();
        this.tmpDir = new File(this.baseTmpDir, "attachment");
        this.tmpDir.mkdirs();

        Provider<FilesystemExportContext> exportContextProvider = this.oldCore.getMocker().getInstance(
            new DefaultParameterizedType(null, Provider.class, FilesystemExportContext.class));
        this.exportContext = exportContextProvider.get();

        doReturn("/xwiki").when(this.oldCore.getSpyXWiki()).getWebAppPath(any(XWikiContext.class));

        XWikiContext context = this.oldCore.getXWikiContext();
        XWikiRequest request = mock(XWikiRequest.class);
        context.setRequest(request);
        context.setURL(new URL("http://www.xwiki.org/"));

        this.urlFactory.init(null, this.tmpDir, this.exportContext, context);
    }

    @Test
    void createAttachmentURLWithWhitespacesInSpaceAndPageNames() throws Exception
    {
        // Prepare the exported document and attachment.
        XWikiDocument doc = new XWikiDocument(
            new DocumentReference("Wiki", Arrays.asList(" Space1 ", "Space2"), "Pa ge"));

        // Simulate the doc depth (there are 4 levels since the doc will be placed in
        // pages/Wiki/Space1/Space2/Pa%20Page).
        this.exportContext.setDocParentLevels(4);

        XWikiAttachment attachment = new XWikiAttachment(doc, "img .jpg");
        attachment.setContent(new ByteArrayInputStream("test".getBytes()));
        doc.getAttachmentList().add(attachment);
        this.oldCore.getSpyXWiki().saveDocument(doc, this.oldCore.getXWikiContext());

        URL url = this.urlFactory.createAttachmentURL("img .jpg", " Space1 .Space2", "Pa ge", "view", "", "Wiki",
            this.oldCore.getXWikiContext());

        // Verify generated URL
        assertEquals(new URL("file://../../../../attachment/Wiki/+Space1+/Space2/Pa+ge/img+.jpg"), url);

        // Verify that the file on the FS exists and that the name is ok
        File generatedFile = new File(this.tmpDir, "attachment/Wiki/+Space1+/Space2/Pa+ge/img+.jpg");
        assertTrue(generatedFile.exists());
        assertEquals("img+.jpg", generatedFile.getName());
    }
}

