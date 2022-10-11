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
package org.xwiki.store.script;

import java.io.IOException;
import java.util.stream.Stream;

import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiRequest;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link TemporaryAttachmentsScriptService}.
 *
 * @version $Id$
 * @since 14.9RC1
 */
@ComponentTest
class TemporaryAttachmentsScriptServiceTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    @InjectMockComponents
    private TemporaryAttachmentsScriptService temporaryAttachmentsScriptService;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiRequest request;

    @Mock
    private Part part;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument xWikiDocument;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.context)).thenReturn(this.xWikiDocument);
        XWikiRightService xWikiRightService = mock(XWikiRightService.class);
        when(this.wiki.getRightService()).thenReturn(xWikiRightService);
        when(xWikiRightService.hasProgrammingRights(this.context)).thenReturn(true);
    }

    @Test
    void uploadTemporaryAttachment() throws Exception
    {
        XWikiAttachment xWikiAttachment = mock(XWikiAttachment.class);

        when(this.request.getPart("upload")).thenReturn(this.part);
        when(this.temporaryAttachmentSessionsManager.uploadAttachment(DOCUMENT_REFERENCE, this.part))
            .thenReturn(xWikiAttachment);

        Attachment temporaryAttachment =
            this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(DOCUMENT_REFERENCE, "upload");

        assertSame(xWikiAttachment, temporaryAttachment.getAttachment());

        verify(this.temporaryAttachmentSessionsManager).uploadAttachment(DOCUMENT_REFERENCE, this.part);
    }

    @ParameterizedTest
    @MethodSource("provideUploadTemporaryAttachmentWithException")
    void uploadTemporaryAttachmentPartWithException(Class<? extends Throwable> exceptionType, String expectedMessage)
        throws Exception
    {
        when(this.request.getPart("upload")).thenThrow(exceptionType);

        assertNull(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(DOCUMENT_REFERENCE,
            "upload"));

        verify(this.temporaryAttachmentSessionsManager, never()).uploadAttachment(DOCUMENT_REFERENCE, this.part);
        assertEquals(expectedMessage, this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    public static Stream<Arguments> provideUploadTemporaryAttachmentWithException()
    {
        return Stream.of(
            Arguments.of(IOException.class, "Error while reading the request content part: [IOException: ]"),
            Arguments.of(ServletException.class, "Error while reading the request content part: [ServletException: ]")
        );
    }

    @Test
    void uploadTemporaryAttachmentWithException()
        throws Exception
    {
        when(this.request.getPart("upload")).thenReturn(this.part);
        when(this.temporaryAttachmentSessionsManager.uploadAttachment(DOCUMENT_REFERENCE, this.part))
            .thenThrow(TemporaryAttachmentException.class);

        assertNull(this.temporaryAttachmentsScriptService.uploadTemporaryAttachment(DOCUMENT_REFERENCE,
            "upload"));

        assertEquals("Error while uploading the attachment: [TemporaryAttachmentException: ]",
            this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }
}

