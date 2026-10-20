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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.servlet.ServletOutputStream;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.SkinAction} class.
 *
 * @version $Id$
 */
@ComponentTest
class SkinActionTest
{
    @InjectMockComponents
    private SkinAction action;

    private static final String FILENAME = "style.css";

    private static final String VELOCITY_CODE = "#set($x = 1)$x";

    private static final String EVALUATED_CODE = "1";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space", "Page");

    private static final DocumentReference DOCUMENT_AUTHOR = new DocumentReference("wiki", "XWiki", "DocumentAuthor");

    private static final DocumentReference ATTACHMENT_AUTHOR =
        new DocumentReference("wiki", "XWiki", "AttachmentAuthor");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private DocumentAuthorizationManager documentAuthorizationManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiResponse response;

    @Mock
    private ServletOutputStream outputStream;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiAttachment attachment;

    @BeforeEach
    void beforeEach() throws Exception
    {
        XWiki xwiki = mock();
        XWikiEngineContext engineContext = mock();
        when(this.context.getWiki()).thenReturn(xwiki);
        when(this.context.getResponse()).thenReturn(this.response);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
        when(engineContext.getMimeType(FILENAME)).thenReturn("text/css");
        when(this.response.getOutputStream()).thenReturn(this.outputStream);

        when(this.document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.document.getDate()).thenReturn(new Date());
        DocumentAuthors documentAuthors = mock();
        UserReference documentAuthorUserReference = mock();
        when(this.document.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getEffectiveMetadataAuthor()).thenReturn(documentAuthorUserReference);
        when(this.documentUserSerializer.serialize(documentAuthorUserReference)).thenReturn(DOCUMENT_AUTHOR);

        when(this.document.getAttachment(FILENAME)).thenReturn(this.attachment);
        when(this.attachment.getReference()).thenReturn(new AttachmentReference(FILENAME, DOCUMENT_REFERENCE));
        when(this.attachment.getAuthorReference()).thenReturn(ATTACHMENT_AUTHOR);
        when(this.attachment.getDate()).thenReturn(new Date());
        when(this.attachment.getContent(this.context)).thenReturn(VELOCITY_CODE.getBytes(StandardCharsets.UTF_8));

        when(this.authorExecutor.call(any(), any(), eq(DOCUMENT_REFERENCE))).thenReturn(EVALUATED_CODE);
    }

    private void grantScriptRight(DocumentReference user)
    {
        when(this.documentAuthorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, user,
            DOCUMENT_REFERENCE)).thenReturn(true);
    }

    private void mockSkinObject()
    {
        BaseObject object = mock();
        when(this.document.getObject("XWiki.XWikiSkins")).thenReturn(object);
        when(object.getStringValue(FILENAME)).thenReturn(VELOCITY_CODE);
        when(object.getReference())
            .thenReturn(new BaseObjectReference(new ObjectReference("XWiki.XWikiSkins[0]", DOCUMENT_REFERENCE)));
    }

    private void assertWrittenContent(String expected) throws IOException
    {
        verify(this.outputStream).write(expected.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void renderFileFromObjectFieldWithScriptRight() throws Exception
    {
        mockSkinObject();
        grantScriptRight(DOCUMENT_AUTHOR);

        assertTrue(this.action.renderFileFromObjectField(FILENAME, this.document, this.context));

        verify(this.authorExecutor).call(any(), eq(DOCUMENT_AUTHOR), eq(DOCUMENT_REFERENCE));
        assertWrittenContent(EVALUATED_CODE);
    }

    @Test
    void renderFileFromObjectFieldWithoutScriptRight() throws Exception
    {
        mockSkinObject();

        assertTrue(this.action.renderFileFromObjectField(FILENAME, this.document, this.context));

        verify(this.authorExecutor, never()).call(any(), any(), any());
        assertWrittenContent(VELOCITY_CODE);
        assertEquals("The Velocity code of [Object_property wiki:Space.Page^XWiki.XWikiSkins[0].style\\.css] is not "
            + "evaluated because its author [wiki:XWiki.DocumentAuthor] doesn't have script right on the document.",
            this.logCapture.getMessage(0));
    }

    @Test
    void renderFileFromObjectFieldOnRestrictedDocument() throws Exception
    {
        mockSkinObject();
        grantScriptRight(DOCUMENT_AUTHOR);
        when(this.document.isRestricted()).thenReturn(true);

        assertTrue(this.action.renderFileFromObjectField(FILENAME, this.document, this.context));

        verify(this.authorExecutor, never()).call(any(), any(), any());
        assertWrittenContent(VELOCITY_CODE);
        assertEquals(1, this.logCapture.size());
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void renderFileFromAttachmentWithScriptRight() throws Exception
    {
        grantScriptRight(DOCUMENT_AUTHOR);
        grantScriptRight(ATTACHMENT_AUTHOR);

        assertTrue(this.action.renderFileFromAttachment(FILENAME, this.document, this.context));

        // The content is evaluated with the rights of the user who uploaded the attachment.
        verify(this.authorExecutor).call(any(), eq(ATTACHMENT_AUTHOR), eq(DOCUMENT_REFERENCE));
        assertWrittenContent(EVALUATED_CODE);
    }

    @Test
    void renderFileFromAttachmentWhenAttachmentAuthorHasNoScriptRight() throws Exception
    {
        // The document author having script right (e.g. an administrator who edited the document after the attachment
        // was uploaded) must not be enough.
        grantScriptRight(DOCUMENT_AUTHOR);

        assertTrue(this.action.renderFileFromAttachment(FILENAME, this.document, this.context));

        verify(this.authorExecutor, never()).call(any(), any(), any());
        assertWrittenContent(VELOCITY_CODE);
        assertEquals("The Velocity code of [Attachment wiki:Space.Page@style.css] is not evaluated because either "
            + "its author [wiki:XWiki.AttachmentAuthor] or the author [wiki:XWiki.DocumentAuthor] of the document "
            + "doesn't have script right on the document.", this.logCapture.getMessage(0));
    }

    @Test
    void renderFileFromAttachmentWhenDocumentAuthorHasNoScriptRight() throws Exception
    {
        grantScriptRight(ATTACHMENT_AUTHOR);

        assertTrue(this.action.renderFileFromAttachment(FILENAME, this.document, this.context));

        verify(this.authorExecutor, never()).call(any(), any(), any());
        assertWrittenContent(VELOCITY_CODE);
        assertEquals(1, this.logCapture.size());
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void renderFileFromAttachmentOnRestrictedDocument() throws Exception
    {
        grantScriptRight(DOCUMENT_AUTHOR);
        grantScriptRight(ATTACHMENT_AUTHOR);
        when(this.document.isRestricted()).thenReturn(true);

        assertTrue(this.action.renderFileFromAttachment(FILENAME, this.document, this.context));

        verify(this.authorExecutor, never()).call(any(), any(), any());
        assertWrittenContent(VELOCITY_CODE);
        assertEquals(1, this.logCapture.size());
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void isTextJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("text/javascript"));
    }

    @Test
    void isApplicationJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/javascript"));
    }

    @Test
    void isApplicationXJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/x-javascript"));
    }

    @Test
    void isTextEcmascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("text/ecmascript"));
    }

    @Test
    void isApplicationEcmascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/ecmascript"));
    }

    @Test
    void npeJavascriptMimetype()
    {
        assertFalse(this.action.isJavascriptMimeType(null));
    }

    @Test
    void incorrectSkinFile()
    {
        Throwable exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("../../resources/js/xwiki/xwiki.js", "colibri");
        });
        assertEquals("Invalid filename: '../../resources/js/xwiki/xwiki.js' for skin 'colibri'",
            exception.getMessage());
        assertEquals("Illegal access, tried to use file [/resources/js/xwiki/xwiki.js] as a skin. "
            + "Possible break-in attempt!", logCapture.getMessage(0));

        exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("../../../", "colibri");
        });
        assertEquals("Invalid filename: '../../../' for skin 'colibri'", exception.getMessage());
        assertEquals("Illegal access, tried to use file [/../] as a skin. Possible break-in attempt!",
            logCapture.getMessage(1));

        exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("resources/js/xwiki/xwiki.js", "..");
        });
        assertEquals("Invalid filename: 'resources/js/xwiki/xwiki.js' for skin '..'", exception.getMessage());
        assertEquals("Illegal access, tried to use file [/resources/js/xwiki/xwiki.js] as a skin. "
            + "Possible break-in attempt!", logCapture.getMessage(2));

        exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("../resources/js/xwiki/xwiki.js", ".");
        });
        assertEquals("Invalid filename: '../resources/js/xwiki/xwiki.js' for skin '.'", exception.getMessage());
        assertEquals("Illegal access, tried to use file [/resources/js/xwiki/xwiki.js] as a skin. "
            + "Possible break-in attempt!", logCapture.getMessage(3));
    }

    @Test
    void incorrectResourceFile()
    {
        Throwable exception = assertThrows(IOException.class, () -> {
            this.action.getResourceFilePath("../../skins/js/xwiki/xwiki.js");
        });
        assertEquals("Invalid filename: '../../skins/js/xwiki/xwiki.js'", exception.getMessage());
        assertEquals("Illegal access, tried to use file [/../skins/js/xwiki/xwiki.js] as a resource. "
            + "Possible break-in attempt!", logCapture.getMessage(0));

        exception = assertThrows(IOException.class, () -> {
            this.action.getResourceFilePath("../../../");
        });
        assertEquals("Invalid filename: '../../../'", exception.getMessage());
        assertEquals("Illegal access, tried to use file [/../../] as a resource. Possible break-in attempt!",
            logCapture.getMessage(1));

        exception = assertThrows(IOException.class, () -> {
            this.action.getResourceFilePath("../../redirect");
        });
        assertEquals("Invalid filename: '../../redirect'", exception.getMessage());
        assertEquals("Illegal access, tried to use file [/../redirect] as a resource. Possible break-in attempt!",
            logCapture.getMessage(2));
    }
}
