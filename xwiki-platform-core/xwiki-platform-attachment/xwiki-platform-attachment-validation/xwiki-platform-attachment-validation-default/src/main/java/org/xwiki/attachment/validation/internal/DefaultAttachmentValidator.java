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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidationStep;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.attachment.validation.internal.step.FileSizeAttachmentValidationStep;
import org.xwiki.attachment.validation.internal.step.MimetypeAttachmentValidationStep;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * Default implementation of {@link AttachmentValidator}. Check for the file size and mimetype of a given file. The
 * configuration values are retrieved using {@link AttachmentValidationConfiguration}.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
public class DefaultAttachmentValidator implements AttachmentValidator
{
    private static final List<String> KNOWN_STEPS =
        List.of(FileSizeAttachmentValidationStep.HINT, MimetypeAttachmentValidationStep.HINT);

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    @Named(FileSizeAttachmentValidationStep.HINT)
    private AttachmentValidationStep sizeAttachmentValidationStep;

    @Inject
    @Named(MimetypeAttachmentValidationStep.HINT)
    private AttachmentValidationStep mimetypeAttachmentValidationStep;

    @Override
    public void validateAttachment(AttachmentAccessWrapper wrapper) throws AttachmentValidationException
    {
        // Known attachment validators are hardcoded because we want to control the order in which they are called.
        // It is better to first check the attachment size, before running an expensive mimetype validation.
        // TODO: the hardcoding can be removed once XCOMMONS-2507 is implemented.
        this.sizeAttachmentValidationStep.validate(wrapper);
        this.mimetypeAttachmentValidationStep.validate(wrapper);
        try {
            Map<String, AttachmentValidationStep> map =
                this.componentManager.getInstanceMap(AttachmentValidationStep.class);
            for (Entry<String, AttachmentValidationStep> entry : map.entrySet()) {
                // Hardcoded steps are skipped as they have already been executed.
                if (!KNOWN_STEPS.contains(entry.getKey())) {
                    entry.getValue().validate(wrapper);
                }
            }
        } catch (ComponentLookupException e) {
            throw new AttachmentValidationException(
                String.format("Failed to resolve the [%s] components.", AttachmentValidationStep.class), e,
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "attachment.validation.attachmentValidationStep.error");
        }
    }
}
