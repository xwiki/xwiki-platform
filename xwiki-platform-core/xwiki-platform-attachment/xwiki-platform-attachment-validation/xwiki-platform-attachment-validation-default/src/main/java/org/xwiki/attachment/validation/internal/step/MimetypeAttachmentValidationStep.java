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
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidationStep;
import org.xwiki.bridge.attachment.AttachmentAccessWrapper;
import org.xwiki.bridge.attachment.AttachmentAccessWrapperException;
import org.xwiki.component.annotation.Component;
import org.xwiki.tika.internal.TikaUtils;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Validate the attachment mimetype.
 *
 * @version $Id$
 * @since 14.10RC1
 */
@Component
@Singleton
@Named(MimetypeAttachmentValidationStep.HINT)
public class MimetypeAttachmentValidationStep implements AttachmentValidationStep
{
    /**
     * This component hint.
     */
    public static final String HINT = "mimetype";

    @Inject
    private AttachmentValidationConfiguration attachmentValidationConfiguration;

    @Inject
    private Logger logger;

    @Override
    public void validate(AttachmentAccessWrapper wrapper) throws AttachmentValidationException
    {
        InputStream inputStream;
        try {
            inputStream = wrapper.getInputStream();
        } catch (AttachmentAccessWrapperException e) {
            throw new AttachmentValidationException(
                String.format("Failed to read the input stream for [%s]", wrapper), e,
                SC_INTERNAL_SERVER_ERROR, "attachment.validation.inputStream.error");
        }
        String mimeType = detectMimeType(inputStream, wrapper.getFileName()).toLowerCase();
        List<String> allowedMimetypes = this.attachmentValidationConfiguration.getAllowedMimetypes();
        List<String> blockerMimetypes = this.attachmentValidationConfiguration.getBlockerMimetypes();
        boolean hasAllowedMimetypes = !allowedMimetypes.isEmpty();
        boolean hasBlockerMimetypes = !blockerMimetypes.isEmpty();

        if (hasAllowedMimetypes && !checkMimetype(allowedMimetypes, mimeType)
            || hasBlockerMimetypes && checkMimetype(blockerMimetypes, mimeType))
        {
            throw new AttachmentValidationException(String.format("Invalid mimetype [%s]", mimeType),
                SC_UNSUPPORTED_MEDIA_TYPE, "attachment.validation.mimetype.rejected", null);
        }
    }

    private boolean checkMimetype(List<String> mimetypes, String mimeType)
    {
        return mimetypes.stream().anyMatch(mimeTypePattern -> Pattern.matches(mimeTypePattern, mimeType));
    }

    private String detectMimeType(InputStream inputStream, String fileName)
    {
        String mimeType;
        try {
            mimeType = TikaUtils.detect(inputStream, fileName);
        } catch (Exception e) {
            this.logger.warn("Failed to identify the mimetype of [{}]. Cause: [{}]", fileName, getRootCauseMessage(e));
            mimeType = "";
        }
        return mimeType;
    }
}
