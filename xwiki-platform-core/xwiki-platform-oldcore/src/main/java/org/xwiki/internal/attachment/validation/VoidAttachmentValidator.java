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
package org.xwiki.internal.attachment.validation;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.component.annotation.Component;

/**
 * Implementation of {@link AttachmentValidator} with a low priority ({@code 2000}) doing nothing but logging a debug
 * trace. This is meant as a replacement in contexts where no actual implementation of {@link AttachmentValidator} is
 * provided by another extension. The default expected extension is
 * {@code xwiki-platform-attachment-validation-default}.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
public class VoidAttachmentValidator implements AttachmentValidator
{
    @Inject
    private Logger logger;

    @Override
    public void validateAttachment(AttachmentAccessWrapper wrapper) throws AttachmentValidationException
    {
        this.logger.debug("This is a minimal default implementation with a low priority. This component is expected "
            + "to be replaced by an actual implementation in production.");
    }
}
