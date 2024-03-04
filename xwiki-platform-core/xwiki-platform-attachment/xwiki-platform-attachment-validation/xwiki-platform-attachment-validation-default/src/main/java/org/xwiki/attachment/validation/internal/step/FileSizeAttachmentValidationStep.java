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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidationStep;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * Validate the attachment size.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
@Named(FileSizeAttachmentValidationStep.HINT)
public class FileSizeAttachmentValidationStep implements AttachmentValidationStep
{
    /**
     * This component hint.
     */
    public static final String HINT = "size";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void validate(AttachmentAccessWrapper wrapper) throws AttachmentValidationException
    {
        long uploadMaxSize = getUploadMaxSize();
        if (wrapper.getSize() > uploadMaxSize) {
            throw new AttachmentValidationException("File size too big",
                Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), "attachment.validation.filesize.rejected",
                List.of(byteCountToDisplaySize(uploadMaxSize)), "fileuploadislarge");
        }
    }

    private long getUploadMaxSize()
    {
        XWikiContext context = this.contextProvider.get();
        return context.getWiki().getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context);
    }
}
