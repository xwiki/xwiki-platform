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
package org.xwiki.wysiwyg;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.script.ScriptContext;
import javax.servlet.http.Part;

import org.apache.commons.lang3.function.FailableRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.store.merge.MergeScriptService;
import org.xwiki.store.script.TemporaryAttachmentsScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.fasterxml.jackson.databind.JsonNode;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.plugin.fileupload.FileUploadPluginApi;
import com.xpn.xwiki.render.ScriptXWikiServletRequest;

import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test of {@code XWiki.WYSIWYG.FileUploader}.
 *
 * @version $Id$
 * @since 14.10
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    DocumentReferenceConverter.class,
    EntityReferenceConverter.class,
    ModelScriptService.class,
    MergeScriptService.class
})
class FileUploaderPageTest extends PageTest
{
    private ScriptContext scriptContext;

    @Mock
    private CSRFTokenScriptService csrfScriptService;

    /**
     * Mocked because we don't want to deal with the underlying implementation but simply validate error handling.
     */
    @Mock
    private TemporaryAttachmentsScriptService temporaryAttachmentsScriptService;

    private DocumentReference documentReference =
        new DocumentReference("xwiki", List.of("XWiki", "WYSIWYG"), "FileUploader");

    @BeforeEach
    void setUp() throws Exception
    {
        ScriptContextManager scriptContextManager = this.oldcore.getMocker().getInstance(ScriptContextManager.class);
        this.scriptContext = scriptContextManager.getScriptContext();
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        this.componentManager.registerComponent(ScriptService.class, "temporaryAttachments",
            this.temporaryAttachmentsScriptService);
        this.componentManager.registerMockComponent(MergeManager.class);
        this.componentManager.registerMockComponent(MergeConflictDecisionsManager.class);
        // Make all the csrf tokens valid by default.
        when(this.csrfScriptService.isTokenValid(any())).thenReturn(true);
        setOutputSyntax(Syntax.PLAIN_1_0);
        setAttachmentSupportStatus(true);
    }

    @Test
    void renderUploadSuccess() throws Exception
    {
        this.context.setAction("get");

        this.request.put("filename", "test.txt");

        Attachment attachment = mock(Attachment.class);
        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "test.txt")).thenReturn(attachment);

        when(attachment.getFilename()).thenReturn("test.txt");

        JsonNode json = renderJSONPage(documentReference);

        assertEquals(1, json.get("uploaded").asInt());
        assertEquals("/xwiki/bin/download/XWiki/WYSIWYG/FileUploader/test.txt", json.get("url").asText());
        assertEquals("test.txt", json.get("fileName").asText());
    }

    @Test
    void renderUploadValidationIssue() throws Exception
    {
        this.context.setAction("get");

        this.request.put("filename", "test.txt");

        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "test.txt")).thenThrow(new AttachmentValidationException("message", 42, "translationKey", null));

        JsonNode json = renderJSONPage(documentReference);

        assertEquals(0, json.get("uploaded").asInt());
        assertEquals(400, json.get("error").get("number").asInt());
        assertEquals("translationKey", json.get("error").get("message").asText());
    }

    @Test
    void renderUploadTemporaryAttachmentIssue() throws Exception
    {
        this.context.setAction("get");

        this.request.put("filename", "test.txt");

        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "test.txt")).thenThrow(mock(TemporaryAttachmentException.class));

        JsonNode json = renderJSONPage(documentReference);

        assertEquals(0, json.get("uploaded").asInt());
        assertEquals(400, json.get("error").get("number").asInt());
        assertEquals("wysiwyg.upload.error.emptyReturn", json.get("error").get("message")
            .asText());
    }

    @Test
    void replaceFileCreatedFromDataURIExplicitFilename() throws Exception
    {
        this.context.setAction("get");

        this.request.put("filename", "__fileCreatedFromDataURI__.zip");

        Attachment attachment = mock(Attachment.class);

        // The filename is not substituted when the filename is set explicitly in the query.
        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "__fileCreatedFromDataURI__.zip")).thenReturn(attachment);

        when(attachment.getFilename()).thenReturn("__fileCreatedFromDataURI__.zip");

        JsonNode json = renderJSONPage(documentReference);

        assertEquals(1, json.get("uploaded").asInt());
        assertEquals("/xwiki/bin/download/XWiki/WYSIWYG/FileUploader/__fileCreatedFromDataURI__.zip",
            json.get("url").asText());
        assertEquals("__fileCreatedFromDataURI__.zip", json.get("fileName").asText());
    }

    @Test
    void replaceFileCreatedFromDataURI() throws Exception
    {
        this.context.setAction("get");

        Part part = mock(Part.class);
        when(part.getName()).thenReturn("upload");
        when(part.getSubmittedFileName()).thenReturn("__fileCreatedFromDataURI__.zip");
        this.request.getParts().add(part);

        Attachment attachment = mock(Attachment.class);
        // Match the call to uploadTemporaryAttachment only when the provided filename matches the name template used as
        // a replacement for __fileCreatedFromDataURI__: [current timestamp]-[random int between 1 and 1].[extension]
        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(eq(documentReference), eq("upload"),
            matches("\\d{13}-\\d+\\.zip"))).thenAnswer(invocation -> {
            when(attachment.getFilename()).thenReturn(invocation.getArgument(2));
            return attachment;
        });

        JsonNode json = renderJSONPage(documentReference);

        String filename = attachment.getFilename();
        assertEquals(1, json.get("uploaded").asInt());
        assertEquals(String.format("/xwiki/bin/download/XWiki/WYSIWYG/FileUploader/%s", filename),
            json.get("url").asText());
        assertEquals(filename, json.get("fileName").asText());
    }

    private void setAttachmentSupportStatus(boolean status)
    {
        ScriptXWikiServletRequest requestSpy =
            spy((ScriptXWikiServletRequest) this.scriptContext.getAttribute("request"));
        this.scriptContext.setAttribute("request", requestSpy, GLOBAL_SCOPE);
        when(requestSpy.getHeader("X-XWiki-Temporary-Attachment-Support")).thenReturn(Boolean.toString(status));
    }

    private DocumentReference mockOldUpload(String fileName, byte[] content) throws Exception
    {
        this.context.setAction("get");
        setAttachmentSupportStatus(false);

        FileUploadPluginApi fileUploadPluginApi = mock();
        when(fileUploadPluginApi.getFileName("upload")).thenReturn(fileName);
        when(fileUploadPluginApi.getFileItemData("upload")).thenReturn(content);
        doReturn(fileUploadPluginApi).when(this.xwiki).getPluginApi(eq("fileupload"), any());

        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(any(), any())).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(), any(), any())).thenReturn(true);
        this.oldcore.checkDocumentRevision(true);

        DocumentReference targetReference = new DocumentReference("xwiki", "Space", "Target");
        this.request.put("document", "Space.Target");
        return targetReference;
    }

    /**
     * Runs the given action right after the given document is loaded for the first time, i.e. after the upload request
     * got its copy of the document and before it saves it.
     */
    private void afterFirstLoad(DocumentReference reference, FailableRunnable<XWikiException> action)
        throws XWikiException
    {
        AtomicBoolean loaded = new AtomicBoolean();
        doAnswer(invocation -> {
            Object document = invocation.callRealMethod();
            if (loaded.compareAndSet(false, true)) {
                action.run();
            }
            return document;
        }).when(this.xwiki).getDocument(eq(reference), any(XWikiContext.class));
    }

    @Test
    void uploadToDocumentCreatedConcurrently() throws Exception
    {
        DocumentReference targetReference = mockOldUpload("test.txt", new byte[] { 1, 2, 3 });
        // Another user uploads a file to the same new document while this upload is in progress.
        afterFirstLoad(targetReference, () -> {
            XWikiDocument concurrentDocument = this.xwiki.getDocument(targetReference, this.context).clone();
            concurrentDocument.setTitle("concurrent");
            this.xwiki.saveDocument(concurrentDocument, this.context);
        });

        JsonNode json = renderJSONPage(this.documentReference);

        assertEquals(1, json.get("uploaded").asInt(), json.toString());
        assertEquals("test.txt", json.get("fileName").asText());
        XWikiDocument targetDocument = this.xwiki.getDocument(targetReference, this.context);
        // Both changes are kept.
        assertEquals("concurrent", targetDocument.getTitle());
        assertNotNull(targetDocument.getAttachment("test.txt"));
    }

    @Test
    void uploadFailingToSave() throws Exception
    {
        DocumentReference targetReference = mockOldUpload("test.txt", new byte[] { 1, 2, 3 });
        doThrow(new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Not allowed")).when(this.xwiki)
            .saveDocument(argThat(document -> targetReference.equals(document.getDocumentReference())), anyString(),
                anyBoolean(), anyBoolean(), any(XWikiContext.class));

        JsonNode json = renderJSONPage(this.documentReference);

        assertEquals(0, json.get("uploaded").asInt());
        assertEquals(500, json.get("error").get("number").asInt());
        assertTrue(json.get("error").get("message").asText().contains("Not allowed"),
            json.get("error").get("message").asText());
        assertTrue(this.xwiki.getDocument(targetReference, this.context).isNew());
    }
}
