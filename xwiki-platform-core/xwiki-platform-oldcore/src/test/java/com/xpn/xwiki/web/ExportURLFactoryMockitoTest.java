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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.filesystem.FilesystemExportContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExportURLFactory}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class ExportURLFactoryMockitoTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "Alice");

    private static final String DOCUMENT_NAME = "document";

    private static final String SPACE_NAME = "space";

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", SPACE_NAME, DOCUMENT_NAME);

    private static final AttachmentReference ATTACHMENT_REFERENCE =
        new AttachmentReference("test.txt", DOCUMENT_REFERENCE);

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private ExportURLFactory factory;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @BeforeEach
    void setUp() throws Exception
    {
        XWikiContext xcontext = this.oldCore.getXWikiContext();
        xcontext.setUserReference(USER_REFERENCE);
        xcontext.setURL(new URL("http://localhost:8080/xwiki/bin/export/Main/WebHome?format=html"));
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getHeader("x-forwarded-host")).thenReturn(null);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/xwiki");
        when(request.getServletPath()).thenReturn("/bin/");
        xcontext.setRequest(request);
        XWikiResponse response = mock(XWikiResponse.class);
        when(response.encodeURL(anyString())).thenAnswer((Answer) invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            return args[0];
        });
        xcontext.setResponse(response);

        XWiki xwiki = xcontext.getWiki();
        when(xwiki.Param("xwiki.url.protocol", "http")).thenReturn("http");
        when(xwiki.getServerURL("xwiki", xcontext)).thenReturn(new URL("http://localhost:8080"));

        // Setup component mocks required by XWikiServletURLFactory that ExportURLFactory inherits from
        this.componentManager.registerMockComponent(EntityResourceActionLister.class);
        Utils.setComponentManager(this.componentManager);

        // Document with attachment
        XWikiDocument doc = this.oldCore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext);
        doc.setAttachment(ATTACHMENT_REFERENCE.getName(),
            new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), xcontext);
        this.oldCore.getSpyXWiki().saveDocument(doc, this.oldCore.getXWikiContext());

        this.factory = new ExportURLFactory();
    }

    @Test
    void createURLWhenNotInExportedPages() throws Exception
    {
        this.factory.init(Arrays.asList(DOCUMENT_REFERENCE), null, new FilesystemExportContext(),
            this.oldCore.getXWikiContext());

        assertEquals("http://localhost:8080/xwiki/bin/view/space/SomeOtherPage",
            this.factory
                .createURL("space", "SomeOtherPage", "view", null, null, "xwiki", this.oldCore.getXWikiContext())
                .toString());
    }

    @Test
    void createURLWhenInExportedPages() throws Exception
    {
        FilesystemExportContext exportContext = new FilesystemExportContext();
        // Simulate locating the doc in pages/xwiki/Main/WebHome (ie 3 levels deep)
        exportContext.setDocParentLevels(3);

        this.factory.init(Arrays.asList(DOCUMENT_REFERENCE), null, exportContext, this.oldCore.getXWikiContext());

        assertEquals("file://../../../pages/xwiki/space/document.html",
            this.factory
                .createURL(SPACE_NAME, DOCUMENT_NAME, "view", null, null, "xwiki", this.oldCore.getXWikiContext())
                .toString());
    }

    @Test
    void createAttachmentURLWhenUserHasViewRight(@TempDir File exportDir) throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ATTACHMENT_REFERENCE)).thenReturn(true);

        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(exportDir);

        ExportURLFactory accessFactory = new ExportURLFactory(true);
        accessFactory.init(Arrays.asList(DOCUMENT_REFERENCE), null, exportContext, this.oldCore.getXWikiContext());

        URL url = accessFactory.createAttachmentURL(ATTACHMENT_REFERENCE.getName(), SPACE_NAME, DOCUMENT_NAME,
            "download", null, "xwiki", this.oldCore.getXWikiContext());

        assertEquals("file://attachment/xwiki/space/document/test.txt", url.toString());
        assertTrue(new File(exportDir, "attachment/xwiki/space/document/test.txt").exists());
    }

    @Test
    void createAttachmentURLWhenAccessCheckIsDisabled(@TempDir File exportDir) throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ATTACHMENT_REFERENCE)).thenReturn(false);

        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(exportDir);

        this.factory.init(Arrays.asList(DOCUMENT_REFERENCE), null, exportContext, this.oldCore.getXWikiContext());

        URL url = this.factory.createAttachmentURL(ATTACHMENT_REFERENCE.getName(), SPACE_NAME, DOCUMENT_NAME,
            "download", null, "xwiki", this.oldCore.getXWikiContext());

        assertEquals("file://attachment/xwiki/space/document/test.txt", url.toString());
        assertTrue(new File(exportDir, "attachment/xwiki/space/document/test.txt").exists());
    }

    @Test
    void createAttachmentURLWhenUserHasNoViewRight(@TempDir File exportDir) throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, ATTACHMENT_REFERENCE)).thenReturn(false);

        FilesystemExportContext exportContext = new FilesystemExportContext();
        exportContext.setExportDir(exportDir);

        ExportURLFactory accessFactory = new ExportURLFactory(true);
        accessFactory.init(Arrays.asList(DOCUMENT_REFERENCE), null, exportContext, this.oldCore.getXWikiContext());

        URL url = accessFactory.createAttachmentURL(ATTACHMENT_REFERENCE.getName(), SPACE_NAME, DOCUMENT_NAME,
            "download", null, "xwiki", this.oldCore.getXWikiContext());

        assertEquals("file://attachment/xwiki/space/document/test.txt", url.toString());
        assertFalse(new File(exportDir, "attachment/xwiki/space/document/test.txt").exists());

        assertEquals("User [xwiki:XWiki.Alice] doesn't have access to attachment "
                + "[Attachment xwiki:space.document@test.txt] so it won't be exported", this.logCapture.getMessage(0));
    }
}
