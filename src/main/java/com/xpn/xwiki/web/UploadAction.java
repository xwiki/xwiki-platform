/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */
package com.xpn.xwiki.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;

public class UploadAction extends XWikiAction
{
    private static final Log log = LogFactory.getLog(UploadAction.class);

    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String username = context.getUser();
        Object exception = context.get("exception");
        boolean ajax = ((Boolean) context.get("ajax")).booleanValue();
        // check Exception File upload is large
        if (exception != null) {
            if (exception instanceof XWikiException) {
                XWikiException exp = (XWikiException) exception;
                if (exp.getCode() == XWikiException.ERROR_XWIKI_APP_FILE_EXCEPTION_MAXSIZE) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    context.put("message", "fileuploadislarge");
                    return true;
                }
            }
        }
        FileUploadPlugin fileupload = (FileUploadPlugin) context.get("fileuploadplugin");
        String filename;
        try {
            filename = fileupload.getFileItemAsString("filename", context);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            context.put("message", "tempdirnotset");
            return true;
        }

        if (filename != null) {
            if (filename.indexOf("/") != -1 || filename.indexOf("\\") != -1
                || filename.indexOf(";") != -1) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                context.put("message", "notsupportcharacters");
                return true;
            }
        }

        byte[] data = fileupload.getFileItemData("filepath", context);
        if (filename == null || filename.trim().equals("")) {
            String fname = fileupload.getFileName("filepath", context);
            int i = fname.lastIndexOf("\\");
            if (i == -1)
                i = fname.lastIndexOf("/");
            filename = fname.substring(i + 1);
        }
        filename = filename.replaceAll("\\+", " ");

        XWikiDocument olddoc = (XWikiDocument) doc.clone();
        // Read XWikiAttachment
        XWikiAttachment attachment = olddoc.getAttachment(filename);

        if (attachment == null) {
            attachment = new XWikiAttachment();
            olddoc.getAttachmentList().add(attachment);
        }
        attachment.setContent(data);
        attachment.setFilename(filename);
        // TODO: handle Author
        attachment.setAuthor(username);

        // Add the attachment to the document
        attachment.setDoc(olddoc);

        olddoc.setAuthor(username);
        if (olddoc.isNew()) {
            olddoc.setCreator(username);
        }

        // Save the content and the archive
        try {
            olddoc.saveAttachmentContent(attachment, context);
        } catch (XWikiException e) {
            // check Exception is ERROR_XWIKI_APP_JAVA_HEAP_SPACE when saving Attachment
            if (e.getCode() == XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                context.put("message", "javaheapspace");
                return true;
            }
            throw e;
        }

        if (ajax) {
            try {
                response.getOutputStream().println("ok");
            } catch (IOException ex) {
                log.error("Unhandled exception writing output:", ex);
            }
            return false;
        }
        // forward to attach page
        String redirect = fileupload.getFileItemAsString("xredirect", context);
        if ((redirect == null) || (redirect.equals("")))
            redirect = context.getDoc().getURL("attach", true, context);
        sendRedirect(response, redirect);
        return false;
    }

    public String render(XWikiContext context) throws XWikiException
    {
        boolean ajax = ((Boolean) context.get("ajax")).booleanValue();
        if (ajax) {
            try {
                context.getResponse().getOutputStream().println("error: " + context.getMessageTool().get((String)context.get("message")));
            } catch (IOException ex) {
                log.error("Unhandled exception writing output:", ex);
            }
            return null;
        }
        return "exception";

    }
}
