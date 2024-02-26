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

import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link FileSizeAttachmentValidationStep}.
 *
 * @version $Id$
 * @since 14.10
 */
@ComponentTest
class FileSizeAttachmentValidationStepTest
{
    @InjectMockComponents
    private FileSizeAttachmentValidationStep validationStep;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext xwikiContext;

    @Mock
    private XWiki wiki;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.xwikiContext);
        when(this.xwikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE,
            this.xwikiContext)).thenReturn(42L);
    }

    @Test
    void validateAttachmentTooLarge()
    {
        AttachmentAccessWrapper attachmentAccessWrapper = mock(AttachmentAccessWrapper.class);
        when(attachmentAccessWrapper.getSize()).thenReturn(100L);
        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validationStep.validate(attachmentAccessWrapper));

        assertEquals("File size too big", exception.getMessage());
        assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), exception.getHttpStatus());
        assertEquals("attachment.validation.filesize.rejected", exception.getTranslationKey());
        assertEquals(List.of("42 bytes"), exception.getTranslationParameters());
        assertEquals("fileuploadislarge", exception.getContextMessage());
    }
}
