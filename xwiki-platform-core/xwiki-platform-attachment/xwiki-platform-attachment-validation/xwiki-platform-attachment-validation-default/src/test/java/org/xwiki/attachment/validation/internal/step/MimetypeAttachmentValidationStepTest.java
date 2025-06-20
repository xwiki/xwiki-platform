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
package org.xwiki.attachment.validation.internal.step;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MimetypeAttachmentValidationStep}.
 *
 * @version $Id$
 * @since 14.10
 */
@ComponentTest
class MimetypeAttachmentValidationStepTest
{
    @InjectMockComponents
    private MimetypeAttachmentValidationStep validationStep;

    @MockComponent
    private AttachmentValidationConfiguration attachmentValidationConfiguration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @ParameterizedTest
    @ValueSource(strings = {
        "text/*",
        "text/plain"
    })
    void validateMimetypePlainTextBlocked(String blockerMimetype) throws Exception
    {
        when(this.attachmentValidationConfiguration.getBlockerMimetypes()).thenReturn(List.of(blockerMimetype));

        AttachmentAccessWrapper wrapper = mock(AttachmentAccessWrapper.class);
        when(wrapper.getInputStream()).thenReturn(mock(InputStream.class));
        when(wrapper.getFileName()).thenReturn("test.txt");
        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validationStep.validate(wrapper));

        assertEquals("Invalid mimetype [text/plain]", exception.getMessage());
        assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), exception.getHttpStatus());
        assertEquals("attachment.validation.mimetype.rejected", exception.getTranslationKey());
        assertEquals(List.of(List.of(), List.of(blockerMimetype)), exception.getTranslationParameters());
        assertNull(exception.getContextMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "image/png",
        "image/*"
    })
    void validateMimetypePlainTextAllowed(String blockerMimetype) throws Exception
    {
        when(this.attachmentValidationConfiguration.getAllowedMimetypes()).thenReturn(List.of(blockerMimetype));

        AttachmentAccessWrapper wrapper = mock(AttachmentAccessWrapper.class);
        when(wrapper.getInputStream()).thenReturn(mock(InputStream.class));
        when(wrapper.getFileName()).thenReturn("test.txt");
        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validationStep.validate(wrapper));

        assertEquals("Invalid mimetype [text/plain]", exception.getMessage());
        assertEquals(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), exception.getHttpStatus());
        assertEquals("attachment.validation.mimetype.rejected", exception.getTranslationKey());
        assertEquals(List.of(List.of(blockerMimetype), List.of()), exception.getTranslationParameters());
        assertNull(exception.getContextMessage());
    }
}
