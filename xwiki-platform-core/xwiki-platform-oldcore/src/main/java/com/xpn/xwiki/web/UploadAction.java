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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;

/**
 * Action that handles uploading document attachments. It saves all the uploaded files whose fieldname start with
 * {@code filepath}.
 * 
 * @version $Id$
 */
public class UploadAction extends XWikiAction
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadAction.class);

    /** The prefix of the accepted file input field name. */
    private static final String FILE_FIELD_NAME = "filepath";

    /** The prefix of the corresponding filename input field name. */
    private static final String FILENAME_FIELD_NAME = "filename";

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#action(XWikiContext)
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiResponse response = context.getResponse();
        Object exception = context.get("exception");
        boolean ajax = ((Boolean) context.get("ajax")).booleanValue();
        // check Exception File upload is large
        if (exception != null) {
            if (exception instanceof XWikiException) {
                XWikiException exp = (XWikiException) exception;
                if (exp.getCode() == XWikiException.ERROR_XWIKI_APP_FILE_EXCEPTION_MAXSIZE) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    ((VelocityContext) context.get("vcontext")).put("message", "core.action.upload.failure.maxSize");
                    context.put("message", "fileuploadislarge");
                    return true;
                }
            }
        }

        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWikiDocument doc = context.getDoc().clone();

        // The document is saved for each attachment in the group.
        FileUploadPlugin fileupload = (FileUploadPlugin) context.get("fileuploadplugin");
        Map<String, String> fileNames = new LinkedHashMap<String, String>();
        List<String> wrongFileNames = new ArrayList<String>();
        Map<String, String> failedFiles = new LinkedHashMap<String, String>();
        for (String fieldName : fileupload.getFileItemNames(context)) {
            try {
                if (fieldName.startsWith(FILE_FIELD_NAME)) {
                    String fileName = getFileName(fieldName, fileupload, context);
                    if (fileName != null) {
                        fileNames.put(fileName, fieldName);
                    }
                }
            } catch (Exception ex) {
                wrongFileNames.add(fileupload.getFileName(fieldName, context));
            }
        }

        for (Entry<String, String> file : fileNames.entrySet()) {
            try {
                uploadAttachment(file.getValue(), file.getKey(), fileupload, doc, context);
            } catch (Exception ex) {
                LOGGER.warn("Saving uploaded file failed", ex);
                failedFiles.put(file.getKey(), ExceptionUtils.getRootCauseMessage(ex));
            }
        }

        LOGGER.debug("Found files to upload: " + fileNames);
        LOGGER.debug("Failed attachments: " + failedFiles);
        LOGGER.debug("Wrong attachment names: " + wrongFileNames);
        if (ajax) {
            try {
                response.getOutputStream().println("ok");
            } catch (IOException ex) {
                LOGGER.error("Unhandled exception writing output:", ex);
            }
            return false;
        }
        // Forward to the attachment page
        if (failedFiles.size() > 0 || wrongFileNames.size() > 0) {
            ((VelocityContext) context.get("vcontext")).put("message", "core.action.upload.failure");
            ((VelocityContext) context.get("vcontext")).put("failedFiles", failedFiles);
            ((VelocityContext) context.get("vcontext")).put("wrongFileNames", wrongFileNames);
            return true;
        }
        String redirect = fileupload.getFileItemAsString("xredirect", context);
        if (StringUtils.isEmpty(redirect)) {
            redirect = context.getDoc().getURL("attach", true, context);
        }
        sendRedirect(response, redirect);
        return false;
    }

    /**
     * Attach a file to the current document.
     * 
     * @param fieldName the target file field
     * @param filename
     * @param fileupload the {@link FileUploadPlugin} holding the form data
     * @param doc the target document
     * @param context the current request context
     * @return {@code true} if the file was successfully attached, {@code false} otherwise.
     * @throws XWikiException if the form data cannot be accessed, or if the database operation failed
     */
    public boolean uploadAttachment(String fieldName, String filename, FileUploadPlugin fileupload, XWikiDocument doc,
        XWikiContext context) throws XWikiException
    {
        XWikiResponse response = context.getResponse();
        String username = context.getUser();

        // Read XWikiAttachment
        XWikiAttachment attachment = doc.getAttachment(filename);

        if (attachment == null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
        }

        try {
            attachment.setContent(fileupload.getFileItemInputStream(fieldName, context));
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_UPLOAD_FILE_EXCEPTION, "Exception while reading uploaded parsed file", e);
        }

        attachment.setFilename(filename);
        attachment.setAuthor(username);

        // Add the attachment to the document
        attachment.setDoc(doc);

        doc.setAuthor(username);
        if (doc.isNew()) {
            doc.setCreator(username);
        }

        // Adding a comment with a link to the download URL
        String comment;
        String nextRev = attachment.getNextVersion();
        ArrayList<String> params = new ArrayList<String>();
        params.add(filename);
        params.add(doc.getAttachmentRevisionURL(filename, nextRev, context));
        if (attachment.isImage(context)) {
            comment = context.getMessageTool().get("core.comment.uploadImageComment", params);
        } else {
            comment = context.getMessageTool().get("core.comment.uploadAttachmentComment", params);
        }

        // Save the document.
        try {
            context.getWiki().saveDocument(doc, comment, context);
        } catch (XWikiException e) {
            // check Exception is ERROR_XWIKI_APP_JAVA_HEAP_SPACE when saving Attachment
            if (e.getCode() == XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                context.put("message", "javaheapspace");
                return true;
            }
            throw e;
        }
        return false;
    }

    /**
     * Extract the corresponding attachment name for a given file field. It can either be specified in a separate form
     * input field, or it is extracted from the original filename.
     * 
     * @param fieldName the target file field
     * @param fileupload the {@link FileUploadPlugin} holding the form data
     * @param context the current request context
     * @return a valid attachment name
     * @throws XWikiException if the form data cannot be accessed, or if the specified filename is invalid
     */
    protected String getFileName(String fieldName, FileUploadPlugin fileupload, XWikiContext context)
        throws XWikiException
    {
        String filenameField = FILENAME_FIELD_NAME + fieldName.substring(FILE_FIELD_NAME.length());
        String filename = null;

        // Try to use the name provided by the user
        filename = fileupload.getFileItemAsString(filenameField, context);
        if (!StringUtils.isBlank(filename)) {
            // TODO These should be supported, the URL should just contain escapes.
            if (filename.indexOf("/") != -1 || filename.indexOf("\\") != -1 || filename.indexOf(";") != -1) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_INVALID_CHARS,
                    "Invalid filename: " + filename);
            }
        }

        if (StringUtils.isBlank(filename)) {
            // Try to get the actual filename on the client
            String fname = fileupload.getFileName(fieldName, context);
            if (StringUtils.indexOf(fname, "/") >= 0) {
                fname = StringUtils.substringAfterLast(fname, "/");
            }
            if (StringUtils.indexOf(fname, "\\") >= 0) {
                fname = StringUtils.substringAfterLast(fname, "\\");
            }
            filename = fname;
        }
        // Sometimes spaces are replaced with '+' by the browser.
        filename = filename.replaceAll("\\+", " ");

        if (StringUtils.isBlank(filename)) {
            // The file field was left empty, ignore this
            return null;
        }

        return filename;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        boolean ajax = ((Boolean) context.get("ajax")).booleanValue();
        if (ajax) {
            try {
                context.getResponse().getOutputStream()
                    .println("error: " + context.getMessageTool().get((String) context.get("message")));
            } catch (IOException ex) {
                LOGGER.error("Unhandled exception writing output:", ex);
            }
            return null;
        }
        ((VelocityContext) context.get("vcontext")).put("viewer", "uploadfailure");
        return "view";
    }
}
