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
package org.xwiki.attachment.validation.internal;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Provider;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.UploadAction;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static com.xpn.xwiki.web.UploadAction.FILE_FIELD_NAME;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultAttachmentValidator}.
 *
 * @version $Id$
 * @since 14.10RC1
 */
@ComponentTest
class DefaultAttachmentValidatorTest
{
    @InjectMockComponents
    private DefaultAttachmentValidator validator;

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private AttachmentValidationConfiguration attachmentValidationConfiguration;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiContext xwikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private Part part;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.xwikiContext);
        when(this.xwikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE,
            this.xwikiContext)).thenReturn(42L);
        when(this.part.getSubmittedFileName()).thenReturn("fileName.txt");
        when(this.part.getName()).thenReturn("token");
        when(this.part.getSize()).thenReturn(10L);
        when(this.part.getInputStream()).thenReturn(mock(InputStream.class));
    }

    @Test
    void validateAttachmentTooLarge()
    {
        Supplier<Optional<InputStream>> inputStream = () -> Optional.of(Mockito.mock(InputStream.class));
        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validator.validateAttachment(100, inputStream, "fileName.txt"));

        assertEquals("File size too big", exception.getMessage());
        assertEquals(SC_REQUEST_ENTITY_TOO_LARGE, exception.getHttpStatus());
        assertEquals("core.action.upload.failure.maxSize", exception.getTranslationKey());
        assertEquals("fileuploadislarge", exception.getContextMessage());
    }

    @Test
    void validateMimetypeImagesOnly()
    {
        when(this.attachmentValidationConfiguration.getAllowedMimetypes()).thenReturn(List.of("image/.*"));

        Supplier<Optional<InputStream>> inputStream = () -> Optional.of(Mockito.mock(InputStream.class));
        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validator.validateAttachment(10, inputStream, "fileName.txt"));

        assertEquals("Invalid mimetype [text/plain]", exception.getMessage());
        assertEquals(SC_UNSUPPORTED_MEDIA_TYPE, exception.getHttpStatus());
        assertEquals("attachment.validation.mimetype.rejected", exception.getTranslationKey());
        assertNull(exception.getContextMessage());
    }

    @Test
    void validateMimetypePlainTextBlocked()
    {
        when(this.attachmentValidationConfiguration.getBlockerMimetypes()).thenReturn(List.of("text/.*"));

        Supplier<Optional<InputStream>> inputStream = () -> Optional.of(Mockito.mock(InputStream.class));
        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validator.validateAttachment(10, inputStream, "fileName.txt"));

        assertEquals("Invalid mimetype [text/plain]", exception.getMessage());
        assertEquals(SC_UNSUPPORTED_MEDIA_TYPE, exception.getHttpStatus());
        assertEquals("attachment.validation.mimetype.rejected", exception.getTranslationKey());
        assertNull(exception.getContextMessage());
    }

    @Test
    void validateMimetypePartTooLarge()
    {
        when(this.part.getSize()).thenReturn(100L);
        when(this.attachmentValidationConfiguration.getBlockerMimetypes()).thenReturn(List.of("text/.*"));

        AttachmentValidationException exception =
            assertThrows(AttachmentValidationException.class, () -> this.validator.validateAttachment(this.part));

        assertEquals("File size too big", exception.getMessage());
        assertEquals(SC_REQUEST_ENTITY_TOO_LARGE, exception.getHttpStatus());
        assertEquals("core.action.upload.failure.maxSize", exception.getTranslationKey());
        assertEquals("fileuploadislarge", exception.getContextMessage());
    }

    @Test
    void validateMimetypePartInvalidMimetype()
    {
        when(this.part.getName()).thenReturn(FILE_FIELD_NAME + "_suffix");
        when(this.attachmentValidationConfiguration.getBlockerMimetypes()).thenReturn(List.of("text/.*"));

        AttachmentValidationException exception =
            assertThrows(AttachmentValidationException.class, () -> this.validator.validateAttachment(this.part));

        assertEquals("Invalid mimetype [text/plain]", exception.getMessage());
        assertEquals(SC_UNSUPPORTED_MEDIA_TYPE, exception.getHttpStatus());
        assertEquals("attachment.validation.mimetype.rejected", exception.getTranslationKey());
        assertNull(exception.getContextMessage());
    }

    @Test
    void validateMimetypePartMimetypeSkipped() throws Exception
    {
        this.validator.validateAttachment(this.part);
        verifyNoInteractions(this.attachmentValidationConfiguration);
    }
}
