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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.component.annotation.Component;
import org.xwiki.tika.internal.TikaUtils;

import com.xpn.xwiki.XWikiContext;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;

/**
 * @version $Id$
 * @since 14.10RC1
 */
@Component
@Singleton
public class DefaultAttachmentValidator implements AttachmentValidator
{
    @Inject
    @Named("readonly")
    private Provider<XWikiContext> contextProvider;

    @Inject
    private AttachmentValidationConfiguration attachmentValidationConfiguration;

    @Inject
    private Logger logger;

    @Override
    public void validateAttachment(Part part) throws AttachmentValidationException
    {
        // TODO: use constant instead of "filepath"
        long size = part.getSize();
        // We don't check the mimetype for parts that are not expected to be use as file.
        if (StringUtils.startsWith(part.getName(), "filepath")) {
            validateAttachment(size, () -> {
                try {
                    return Optional.of(part.getInputStream());
                } catch (IOException e) {
                    this.logger.warn("Failed to get the input stream for part [{}]. Cause: [{}]", part,
                        getRootCauseMessage(e));
                    return Optional.empty();
                }
            }, part.getSubmittedFileName());
        } else {
            validateSize(size);
        }
    }

    @Override
    public void validateAttachment(long attachmentSize, Supplier<Optional<InputStream>> supplier, String filename)
        throws AttachmentValidationException
    {
        validateAttachment(attachmentSize, supplier, filename);
        validateSize(attachmentSize);
        validateMimetype(supplier, filename);
    }

    private void validateSize(long attachmentSize) throws AttachmentValidationException
    {
        if (attachmentSize > getUploadMaxSize()) {
            throw new AttachmentValidationException("File size too big", SC_REQUEST_ENTITY_TOO_LARGE,
                "core.action.upload.failure.maxSize", "fileuploadislarge");
        }
    }

    private long getUploadMaxSize()
    {
        XWikiContext context = this.contextProvider.get();
        return context.getWiki().getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context);
    }

    private void validateMimetype(Supplier<Optional<InputStream>> supplier, String filename)
        throws AttachmentValidationException
    {
        String mimeType = getMimeType(supplier, filename).toLowerCase();
        List<String> allowedMimetypes = this.attachmentValidationConfiguration.getAllowedMimetypes();
        List<String> blockerMimetypes = this.attachmentValidationConfiguration.getBlockerMimetypes();
        boolean hasAllowedMimetypes = !allowedMimetypes.isEmpty();
        boolean hasBlockerMimetypes = !blockerMimetypes.isEmpty();

        if (hasAllowedMimetypes && !checkMimetype(allowedMimetypes, mimeType)
            || hasBlockerMimetypes && checkMimetype(blockerMimetypes, mimeType))
        {
            throw new AttachmentValidationException("Invalid mimetype", SC_UNSUPPORTED_MEDIA_TYPE,
                "core.action.upload.failure.mimetypeRejected", "TODO");
        }
    }

    private boolean checkMimetype(List<String> mimetypes, String mimeType)
    {
        return mimetypes.stream().anyMatch(mimeTypePattern -> Pattern.matches(mimeTypePattern, mimeType));
    }

    private String getMimeType(Supplier<Optional<InputStream>> supplier, String fileName)
    {
        String mimeType;
        try {
            Optional<InputStream> inputStream = supplier.get();
            if (inputStream.isEmpty()) {
                mimeType = "";
            } else {
                mimeType = TikaUtils.detect(inputStream.get(), fileName);
            }
        } catch (Exception e) {
            this.logger.warn("Failed to identify a mimetype. Cause: [{}]", getRootCauseMessage(e));
            mimeType = "";
        }
        return mimeType;
    }
}
