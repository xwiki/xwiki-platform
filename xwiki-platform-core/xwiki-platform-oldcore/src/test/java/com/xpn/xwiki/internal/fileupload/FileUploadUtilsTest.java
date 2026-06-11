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
package com.xpn.xwiki.internal.fileupload;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static com.xpn.xwiki.web.UploadAction.FILE_FIELD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link  FileUploadUtils}.
 *
 * @version $Id$
 * @since 14.10
 */
@ComponentTest
class FileUploadUtilsTest
{
    @Mock
    private HttpServletRequest request;

    @Mock
    private AttachmentValidator validator;

    @Mock
    private Part part0;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.request.getParts()).thenReturn(List.of(this.part0));
        when(this.part0.getName()).thenReturn(FILE_FIELD_NAME + "_aaa");
        when(this.part0.getSubmittedFileName()).thenReturn("file.png");
    }

    @Test
    void getFileItems() throws Exception
    {
        Collection<FileItem> fileItems = FileUploadUtils.getFileItems(100, 100, "/tmp", this.request, this.validator);
        assertEquals(1, fileItems.size());
        verify(this.validator).validateAttachment(any(AttachmentAccessWrapper.class));
    }

    @Test
    void getFileItemsValidationIssue() throws Exception
    {
        doThrow(AttachmentValidationException.class).when(this.validator)
            .validateAttachment(any(AttachmentAccessWrapper.class));
        assertThrows(AttachmentValidationException.class, () -> FileUploadUtils.getFileItems(100, 100, "/tmp",
            this.request, this.validator));
        verify(this.validator).validateAttachment(any(AttachmentAccessWrapper.class));
    }

    @Test
    void getFileItemsValidatesFilePartWithNonFilepathFieldName() throws Exception
    {
        // The WYSIWYG editor (legacy upload mechanism) submits the file under the "upload" field name rather than
        // "filepath". Such file parts must still be validated against the attachment size limit.
        when(this.part0.getName()).thenReturn("upload");

        Collection<FileItem> fileItems = FileUploadUtils.getFileItems(100, 100, "/tmp", this.request, this.validator);

        assertEquals(1, fileItems.size());
        verify(this.validator).validateAttachment(any(AttachmentAccessWrapper.class));
    }

    @Test
    void getFileItemsDoesNotValidateFormFields() throws Exception
    {
        // A plain form field (e.g. xredirect, form_token) has no submitted file name and must not be validated.
        when(this.part0.getName()).thenReturn("xredirect");
        when(this.part0.getSubmittedFileName()).thenReturn(null);

        Collection<FileItem> fileItems = FileUploadUtils.getFileItems(100, 100, "/tmp", this.request, this.validator);

        assertEquals(1, fileItems.size());
        verify(this.validator, never()).validateAttachment(any(AttachmentAccessWrapper.class));
    }
}
