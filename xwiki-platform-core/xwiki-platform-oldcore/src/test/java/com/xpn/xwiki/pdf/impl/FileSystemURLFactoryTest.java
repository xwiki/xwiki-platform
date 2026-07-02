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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.DefaultLegacySpaceResolver;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
@ComponentList({DefaultLegacySpaceResolver.class})
public class FileSystemURLFactoryTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "Alice");

    private static final String DOCUMENT_NAME = "document";

    private static final String SPACE_NAME = "space";

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", SPACE_NAME, DOCUMENT_NAME);

    private static final AttachmentReference ATTACHMENT_REFERENCE =
        new AttachmentReference("test.txt", DOCUMENT_REFERENCE);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiRequest mockXWikiRequest;

    @BeforeEach
    public void beforeEach() throws XWikiException, IOException
    {
        this.oldcore.getXWikiContext().setUserReference(USER_REFERENCE);

        // Request
        this.mockXWikiRequest = mock(XWikiRequest.class);
        when(this.mockXWikiRequest.getContextPath()).thenReturn("/xwiki");
        this.oldcore.getXWikiContext().setRequest(mockXWikiRequest);

        // Document with attachment
        XWikiDocument doc = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        doc.setAttachment(ATTACHMENT_REFERENCE.getName(),
            new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), this.oldcore.getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(doc, this.oldcore.getXWikiContext());
    }

    @Test
    public void createAttachmentURLWhenAttachmentDoesntExist() throws Exception
    {
        Map<String, File> usedFiles = new HashMap<>();
        this.oldcore.getXWikiContext().put("pdfexport-file-mapping", usedFiles);
        File tempDir = new File("target/FileSystemURLFactoryTest");
        tempDir.mkdirs();
        this.oldcore.getXWikiContext().put("pdfexportdir", tempDir);

        FileSystemURLFactory urlFactory = new FileSystemURLFactory();
        urlFactory.init(this.oldcore.getXWikiContext());

        assertNull(urlFactory.createAttachmentURL("nonexisting.png", SPACE_NAME, DOCUMENT_NAME, "view", null,
            this.oldcore.getXWikiContext()));

        assertEquals("Attachment [nonexisting.png] doesn't exist in [xwiki:space.document]. Generated content will have "
            + "invalid content (empty image or broken link)", this.logCapture.getMessage(0));
    }

    @Test
    void createAttachmentURLWhenAccessCheckIsDisabled(@TempDir File exportDir) throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.VIEW, ATTACHMENT_REFERENCE))
            .thenReturn(false);

        this.oldcore.getXWikiContext().put("pdfexport-file-mapping", new HashMap<>());
        this.oldcore.getXWikiContext().put("pdfexportdir", exportDir);

        FileSystemURLFactory urlFactory = new FileSystemURLFactory();
        urlFactory.init(this.oldcore.getXWikiContext());

        assertNotNull(urlFactory.createAttachmentURL(ATTACHMENT_REFERENCE.getName(), SPACE_NAME, DOCUMENT_NAME, "view",
            null, this.oldcore.getXWikiContext()));
    }

    @Test
    void createAttachmentURLWhenUserHasViewRight(@TempDir File exportDir) throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.VIEW, ATTACHMENT_REFERENCE))
            .thenReturn(true);

        this.oldcore.getXWikiContext().put("pdfexport-file-mapping", new HashMap<>());
        this.oldcore.getXWikiContext().put("pdfexportdir", exportDir);

        FileSystemURLFactory accessFactory = new FileSystemURLFactory(true);
        accessFactory.init(this.oldcore.getXWikiContext());

        assertNotNull(accessFactory.createAttachmentURL(ATTACHMENT_REFERENCE.getName(), SPACE_NAME, DOCUMENT_NAME,
            "view", null, this.oldcore.getXWikiContext()));
    }

    @Test
    void createAttachmentURLWhenUserHasNoViewRight(@TempDir File exportDir) throws Exception
    {
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.VIEW, ATTACHMENT_REFERENCE))
            .thenReturn(false);

        this.oldcore.getXWikiContext().put("pdfexport-file-mapping", new HashMap<>());
        this.oldcore.getXWikiContext().put("pdfexportdir", exportDir);

        FileSystemURLFactory accessFactory = new FileSystemURLFactory(true);
        accessFactory.init(this.oldcore.getXWikiContext());

        assertNull(accessFactory.createAttachmentURL(ATTACHMENT_REFERENCE.getName(), SPACE_NAME, DOCUMENT_NAME, "view",
            null, this.oldcore.getXWikiContext()));

        assertEquals("User [xwiki:XWiki.Alice] doesn't have access to attachment "
            + "[Attachment xwiki:space.document@test.txt] so it won't be exported", this.logCapture.getMessage(0));
    }
}
