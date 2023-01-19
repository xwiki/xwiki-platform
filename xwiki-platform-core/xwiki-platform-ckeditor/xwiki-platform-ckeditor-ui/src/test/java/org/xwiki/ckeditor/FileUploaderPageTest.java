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
package org.xwiki.ckeditor;

import javax.script.ScriptContext;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.internal.reference.converter.EntityReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.script.TemporaryAttachmentsScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.render.ScriptXWikiServletRequest;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test of {@code CKEditor.FileUploader}.
 *
 * @version $Id$
 * @since 14.10
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    DocumentReferenceConverter.class,
    EntityReferenceConverter.class
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

    @BeforeEach
    void setUp() throws Exception
    {
        ScriptContextManager scriptContextManager = this.oldcore.getMocker().getInstance(ScriptContextManager.class);
        this.scriptContext = scriptContextManager.getScriptContext();
        this.componentManager.registerComponent(ScriptService.class, "csrf", this.csrfScriptService);
        this.componentManager.registerComponent(ScriptService.class, "temporaryAttachments",
            this.temporaryAttachmentsScriptService);
        // Make all the csrf tokens valid by default.
        when(this.csrfScriptService.isTokenValid(any())).thenReturn(true);
        setOutputSyntax(Syntax.PLAIN_1_0);
        setAttachmentSupportStatus(true);
    }

    @Test
    void renderUploadSuccess() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "CKEditor", "FileUploader");

        this.context.setAction("get");

        this.request.put("filename", "test.txt");

        Attachment attachment = mock(Attachment.class);
        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "test.txt")).thenReturn(attachment);

        when(attachment.getFilename()).thenReturn("test.txt");

        JSON json = renderJSONPage(documentReference);

        assertEquals(1, ((JSONObject) json).get("uploaded"));
        assertEquals("/xwiki/bin/download/CKEditor/FileUploader/test.txt", ((JSONObject) json).get("url"));
        assertEquals("test.txt", ((JSONObject) json).get("fileName"));
    }

    @Test
    void renderUploadValidationIssue() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "CKEditor", "FileUploader");

        this.context.setAction("get");

        this.request.put("filename", "test.txt");

        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "test.txt")).thenThrow(new AttachmentValidationException("message", 42, "translationKey", null));

        JSONObject json = renderJSONPage(documentReference);

        assertEquals(0, json.get("uploaded"));
        assertEquals(400, json.getJSONObject("error").get("number"));
        assertEquals("translationKey", json.getJSONObject("error").get("message"));
    }

    @Test
    void renderUploadTemporaryAttachmentIssue() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "CKEditor", "FileUploader");

        this.context.setAction("get");

        this.request.put("filename", "test.txt");

        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "test.txt")).thenThrow(mock(TemporaryAttachmentException.class));

        JSONObject json = renderJSONPage(documentReference);

        assertEquals(0, json.get("uploaded"));
        assertEquals(400, json.getJSONObject("error").get("number"));
        assertEquals("ckeditor.upload.error.emptyReturn", json.getJSONObject("error").get("message"));
    }

    @Test
    void replaceFileCreatedFromDataURIExplicitFilename() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "CKEditor", "FileUploader");

        this.context.setAction("get");

        this.request.put("filename", "__fileCreatedFromDataURI__.zip");

        Attachment attachment = mock(Attachment.class);

        // The filename is not substituted when the filename is set explicitly in the query.
        when(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(documentReference, "upload",
            "__fileCreatedFromDataURI__.zip")).thenReturn(attachment);

        when(attachment.getFilename()).thenReturn("__fileCreatedFromDataURI__.zip");

        JSON json = renderJSONPage(documentReference);

        assertEquals(1, ((JSONObject) json).get("uploaded"));
        assertEquals("/xwiki/bin/download/CKEditor/FileUploader/__fileCreatedFromDataURI__.zip",
            ((JSONObject) json).get("url"));
        assertEquals("__fileCreatedFromDataURI__.zip", ((JSONObject) json).get("fileName"));
    }

    @Test
    void replaceFileCreatedFromDataURI() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "CKEditor", "FileUploader");

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

        JSON json = renderJSONPage(documentReference);

        String filename = attachment.getFilename();
        assertEquals(1, ((JSONObject) json).get("uploaded"));
        assertEquals(String.format("/xwiki/bin/download/CKEditor/FileUploader/%s", filename),
            ((JSONObject) json).get("url"));
        assertEquals(filename, ((JSONObject) json).get("fileName"));
    }

    private void setAttachmentSupportStatus(boolean status)
    {
        ScriptXWikiServletRequest requestSpy =
            spy((ScriptXWikiServletRequest) this.scriptContext.getAttribute("request"));
        this.scriptContext.setAttribute("request", requestSpy, GLOBAL_SCOPE);
        when(requestSpy.getHeader("X-XWiki-Temporary-Attachment-Support")).thenReturn(Boolean.toString(status));
    }
}
