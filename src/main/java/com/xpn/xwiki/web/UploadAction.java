/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 * @author ludovic
 * @author namphunghai
 * @author wr0ngway
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;

public class UploadAction extends XWikiAction {

    public boolean action(XWikiContext context) throws XWikiException {
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String username = context.getUser();

        FileUploadPlugin fileupload = (FileUploadPlugin) context.get("fileuploadplugin");

        String filename = fileupload.getFileItem("filename", context);
        if(filename.indexOf("/") != -1 || filename.indexOf("\\") != -1 || filename.indexOf(";") != -1){
             context.put("message","notsupportcharacters");
             return true ;
        }
        byte[] data = fileupload.getFileItemData("filepath", context);

        if (filename==null) {
            String fname = fileupload.getFileName("filepath", context);
            int i = fname.indexOf("\\");
            if (i==-1)
                i = fname.indexOf ("/");
            filename = fname.substring(i+1);
        }
        filename = filename.replaceAll("\\+"," ");
        // Read XWikiAttachment
        XWikiAttachment attachment = doc.getAttachment(filename);

        if (attachment==null) {
            attachment = new XWikiAttachment();
            doc.getAttachmentList().add(attachment);
        }
        attachment.setContent(data);
        attachment.setFilename(filename);

        // TODO: handle Author
        attachment.setAuthor(username);

        // Add the attachment to the document
        attachment.setDoc(doc);

        // Save the content and the archive
        doc.saveAttachmentContent(attachment, context);

        // forward to attach page
        String redirect = fileupload.getFileItem("xredirect", context);
        if ((redirect == null)||(redirect.equals("")))
            redirect = context.getDoc().getURL("attach", true, context);
        sendRedirect(response, redirect);
        return false;
    }
    public String render(XWikiContext context) throws XWikiException {
        return "exception";
    }
}