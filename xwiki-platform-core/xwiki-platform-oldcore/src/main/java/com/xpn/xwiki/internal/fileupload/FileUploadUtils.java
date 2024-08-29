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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.internal.attachment.PartAttachmentAccessWrapper;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.fileupload.FileUploadPluginApi;
import com.xpn.xwiki.web.UploadAction;

/**
 * File upload related tools.
 * 
 * @version $Id$
 * @since 13.0
 */
public final class FileUploadUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadUtils.class);

    /**
     * Utility class.
     */
    private FileUploadUtils()
    {
    }

    /**
     * Loads the list of uploaded files in the context if there are any uploaded files. Note that the order of the
     * result is not guaranteed and might be different depending on the servlet engine used.
     *
     * @param uploadMaxSize Maximum size of the uploaded files
     * @param uploadSizeThreshold the threshold over which the file data should be stored on disk, and not in
     *     memory
     * @param tempdir Temporary directory to store the uploaded files that are not kept in memory
     * @param request the request to parse
     * @param attachmentValidator the validator used to validate if the request parts are valid attachments
     * @return the parts found in the request as a collection of {@link FileItem}
     * @throws XWikiException if the request could not be parsed
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     * @see FileUploadPluginApi#loadFileList(long, int, String)
     */
    public static Collection<FileItem> getFileItems(long uploadMaxSize, int uploadSizeThreshold, String tempdir,
        HttpServletRequest request, AttachmentValidator attachmentValidator)
        throws XWikiException, AttachmentValidationException
    {
        // The request multi-part content is automatically consumed by the application server when multi part support is
        // enabled so we cannot use the standard fileupload parser which expect to real the source content.
        // Problem is that we can't easily get rid of the fileupload plugin since it's expected by many extensions (as
        // usual exposing as part of the XWiki API something which is an 3rd party dependency was not such a great
        // idea...)
        Collection<Part> parts;
        try {
            parts = request.getParts();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_UPLOAD_PARSE_EXCEPTION, "Exception while getting uploaded files", e);
        }

        if (!parts.isEmpty()) {
            List<FileItem> items = new ArrayList<>(parts.size());
            for (Part part : parts) {
                if (StringUtils.startsWith(part.getName(), UploadAction.FILE_FIELD_NAME)) {
                    attachmentValidator.validateAttachment(new PartAttachmentAccessWrapper(part));
                }
                items.add(new PartFileItem(part));
            }

            return items;
        } else {
            // If there is no standard part try to parse the request with Fileupload
            // Get the FileUpload Data
            // Make sure the factory only ever creates file items which will be deleted when the jvm is stopped.
            DiskFileItemFactory factory = new DiskFileItemFactory(uploadSizeThreshold, new File(tempdir))
            {
                @Override
                public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName)
                {
                    try {
                        DiskFileItem item =
                            (DiskFileItem) super.createItem(fieldName, contentType, isFormField, fileName);
                        // Needed to make sure the File object is created.
                        item.getOutputStream();
                        return item;
                    } catch (IOException e) {
                        throw new RuntimeException(
                            String.format("Unable to create a temporary file for saving the attachment. "
                                + "Do you have write access on [%s]?", getRepository()),
                            e);
                    }
                }
            };

            // TODO: Does this work in portlet mode, or we must use PortletFileUpload?
            FileUpload fileupload = new ServletFileUpload(factory);
            RequestContext reqContext = new ServletRequestContext(request);
            fileupload.setSizeMax(uploadMaxSize);

            try {
                List<FileItem> list = fileupload.parseRequest(reqContext);
                if (list.size() > 0) {
                    LOGGER.info("Loaded " + list.size() + " uploaded files");
                }

                return list;
            } catch (FileUploadBase.SizeLimitExceededException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_FILE_EXCEPTION_MAXSIZE, "Exception uploaded file");
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_UPLOAD_PARSE_EXCEPTION, "Exception while parsing uploaded file", e);
            }
        }
    }
}
