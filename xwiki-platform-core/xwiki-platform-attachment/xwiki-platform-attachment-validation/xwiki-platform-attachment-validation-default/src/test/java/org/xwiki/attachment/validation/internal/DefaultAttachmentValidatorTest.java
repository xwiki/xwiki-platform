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

import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidationStep;
import org.xwiki.attachment.validation.internal.step.FileSizeAttachmentValidationStep;
import org.xwiki.attachment.validation.internal.step.MimetypeAttachmentValidationStep;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultAttachmentValidator}.
 *
 * @version $Id$
 * @since 14.10
 */
@ComponentTest
class DefaultAttachmentValidatorTest
{
    @InjectMockComponents
    private DefaultAttachmentValidator validator;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    @Named(FileSizeAttachmentValidationStep.HINT)
    private AttachmentValidationStep sizeAttachmentValidationStep;

    @MockComponent
    @Named(MimetypeAttachmentValidationStep.HINT)
    private AttachmentValidationStep mimetypeAttachmentValidationStep;

    @Mock
    private AttachmentAccessWrapper wrapper;

    @Mock
    private AttachmentValidationStep otherAttachmentValidationStep;

    @Test
    void validateAttachment() throws Exception
    {
        this.validator.validateAttachment(this.wrapper);
        verify(this.sizeAttachmentValidationStep).validate(this.wrapper);
        verify(this.mimetypeAttachmentValidationStep).validate(this.wrapper);
        verify(this.componentManager).getInstanceMap(AttachmentValidationStep.class);
    }

    @Test
    void validateAttachmentOneAdditionalStep() throws Exception
    {
        when(this.componentManager.getInstanceMap(AttachmentValidationStep.class)).thenReturn(Map.of(
            MimetypeAttachmentValidationStep.HINT, this.mimetypeAttachmentValidationStep,
            FileSizeAttachmentValidationStep.HINT, this.sizeAttachmentValidationStep,
            "other", this.otherAttachmentValidationStep
        ));
        this.validator.validateAttachment(this.wrapper);
        verify(this.sizeAttachmentValidationStep).validate(this.wrapper);
        verify(this.mimetypeAttachmentValidationStep).validate(this.wrapper);
        verify(this.otherAttachmentValidationStep).validate(this.wrapper);
        verify(this.componentManager).getInstanceMap(AttachmentValidationStep.class);
    }

    @Test
    void validateAttachmentGetInstanceMapError() throws Exception
    {
        when(this.componentManager.getInstanceMap(AttachmentValidationStep.class))
            .thenThrow(ComponentLookupException.class);

        AttachmentValidationException exception = assertThrows(AttachmentValidationException.class,
            () -> this.validator.validateAttachment(this.wrapper));

        assertEquals(
            "Failed to resolve the [interface org.xwiki.attachment.validation.AttachmentValidationStep] components.",
            exception.getMessage());
        verify(this.sizeAttachmentValidationStep).validate(this.wrapper);
        verify(this.mimetypeAttachmentValidationStep).validate(this.wrapper);
        verifyNoInteractions(this.otherAttachmentValidationStep);
    }
}
