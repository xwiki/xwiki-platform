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
        byte[] data = fileupload.getFileItemData("filepath", context);

        if (filename==null) {
            String fname = fileupload.getFileName("filepath", context);
            int i = fname.indexOf("\\");
            if (i==-1)
                i = fname.indexOf("/");
            filename = fname.substring(i+1);
        }

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
}
