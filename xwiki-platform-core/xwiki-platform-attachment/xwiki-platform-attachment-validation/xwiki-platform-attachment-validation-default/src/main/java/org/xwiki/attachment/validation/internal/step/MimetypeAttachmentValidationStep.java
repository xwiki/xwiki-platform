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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidationStep;
import org.xwiki.component.annotation.Component;
import org.xwiki.tika.internal.TikaUtils;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Validate the attachment mimetype based on configured lists of allowed and blocker mimetypes.
 *
 * @version $Id$
 * @since 14.10
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
        try (InputStream inputStream = wrapper.getInputStream()) {
            String mimeType = detectMimeType(inputStream, wrapper.getFileName()).toLowerCase();
            List<String> allowedMimetypes = this.attachmentValidationConfiguration.getAllowedMimetypes();
            List<String> blockerMimetypes = this.attachmentValidationConfiguration.getBlockerMimetypes();
            boolean hasAllowedMimetypes = !allowedMimetypes.isEmpty();
            boolean hasBlockerMimetypes = !blockerMimetypes.isEmpty();

            if (hasAllowedMimetypes && !checkMimetype(allowedMimetypes, mimeType)
                || hasBlockerMimetypes && checkMimetype(blockerMimetypes, mimeType)) {
                throw new AttachmentValidationException(String.format("Invalid mimetype [%s]", mimeType),
                    Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), "attachment.validation.mimetype.rejected",
                    List.of(allowedMimetypes, blockerMimetypes), null);
            }
        } catch (IOException e) {
            throw new AttachmentValidationException(String.format("Failed to read the input stream for [%s]", wrapper),
                e, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "attachment.validation.inputStream.error");
        }
    }

    private boolean checkMimetype(List<String> mimetypes, String mimeType)
    {
        return mimetypes.stream().anyMatch(mimeTypePattern -> {
            if (!mimeTypePattern.contains("*")) {
                return Objects.equals(mimeTypePattern, mimeType);
            } else {
                // Use the first * char found as a joker. 
                String[] parts = mimeTypePattern.split("\\*", 2);
                return mimeType.startsWith(parts[0]) && mimeType.endsWith(parts[1]);
            }
        });
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
