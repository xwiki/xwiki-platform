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
package com.xpn.xwiki.pdf.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.internal.model.DefaultLegacySpaceResolver;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileSystemURLFactory}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({
    DefaultLegacySpaceResolver.class
})
public class FileSystemURLFactoryTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private FileSystemURLFactory urlFactory;

    private XWikiRequest mockXWikiRequest;

    @BeforeEach
    public void beforeEach()
    {
        // Request
        this.mockXWikiRequest = mock(XWikiRequest.class);
        when(this.mockXWikiRequest.getContextPath()).thenReturn("/xwiki");
        this.oldcore.getXWikiContext().setRequest(mockXWikiRequest);

        this.urlFactory = new FileSystemURLFactory();
        this.urlFactory.init(this.oldcore.getXWikiContext());
    }

    @Test
    public void createAttachmentURLWhenAttachmentDoesntExist() throws Exception
    {
        Map<String, File> usedFiles = new HashMap<>();
        this.oldcore.getXWikiContext().put("pdfexport-file-mapping", usedFiles);
        File tempDir = new File("target/FileSystemURLFactoryTest");
        tempDir.mkdirs();
        this.oldcore.getXWikiContext().put("pdfexportdir", tempDir);

        assertNull(this.urlFactory.createAttachmentURL("nonexisting.png", "space", "page", "view", null,
            this.oldcore.getXWikiContext()));

        assertEquals("Attachment [nonexisting.png] doesn't exist in [xwiki:space.page]. Generated content will have "
            + "invalid content (empty image or broken link)", this.logCapture.getMessage(0));
    }
}
