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
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UploadAction}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class UploadActionTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    private static final String FIELD_NAME = UploadAction.FILE_FIELD_NAME;

    private static final String FILE_NAME = "file.txt";

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private UploadAction uploadAction;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiResponse response;

    @Mock
    private FileUploadPlugin fileUpload;

    @BeforeEach
    void beforeEach() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        xcontext.setRequest(this.request);
        xcontext.setResponse(this.response);
        xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "User"));
        when(this.csrfToken.isTokenValid(any())).thenReturn(true);
        ServletOutputStream outputStream = mock();
        when(this.response.getOutputStream()).thenReturn(outputStream);
        xcontext.put("ajax", true);

        xcontext.put("fileuploadplugin", this.fileUpload);
        when(this.fileUpload.getFileItemNames(xcontext)).thenReturn(List.of(FIELD_NAME));
        when(this.fileUpload.getFileName(FIELD_NAME, xcontext)).thenReturn(FILE_NAME);
        // The uploaded content can be read again for each attempt.
        when(this.fileUpload.getFileItemInputStream(FIELD_NAME, xcontext))
            .thenAnswer(invocation -> new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void uploadAfterConcurrentSave() throws Exception
    {
        this.oldcore.checkDocumentRevision(true);
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(DOCUMENT_REFERENCE), xcontext);
        xcontext.setDoc(this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext));

        // Another request saves the document after this request loaded it.
        XWikiDocument concurrentDocument =
            this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext).clone();
        concurrentDocument.setTitle("concurrent title");
        this.oldcore.getSpyXWiki().saveDocument(concurrentDocument, xcontext);

        assertFalse(this.uploadAction.action(xcontext));

        // The file is attached to the version saved by the other request.
        XWikiDocument storedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, xcontext);
        assertEquals("concurrent title", storedDocument.getTitle());
        XWikiAttachment attachment = storedDocument.getAttachment(FILE_NAME);
        assertEquals("content", IOUtils.toString(attachment.getContentInputStream(xcontext), StandardCharsets.UTF_8));
    }
}
