package com.xpn.xwiki.web;

import java.io.File;
import java.util.List;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileUploadException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class UploadAction extends XWikiAction {
    private static final long UPLOAD_DEFAULT_MAXSIZE = 10000000L;
    private static final long UPLOAD_DEFAULT_SIZETHRESHOLD = 100000L;
    
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String username = context.getUser();
        // Get the FileUpload Data
        DiskFileUpload fileupload = new DiskFileUpload();
        fileupload.setSizeMax(xwiki.getXWikiPreferenceAsLong("upload_maxsize", UPLOAD_DEFAULT_MAXSIZE, context));
        fileupload.setSizeThreshold((int)xwiki.getXWikiPreferenceAsLong("upload_sizethreshold", UPLOAD_DEFAULT_SIZETHRESHOLD, context));

        String tempdir = xwiki.Param("xwiki.upload.tempdir");
        if (tempdir!=null) {
            fileupload.setRepositoryPath(tempdir);
            (new File(tempdir)).mkdirs();
        }
        else
            fileupload.setRepositoryPath(".");
        List filelist = null;
        try {
            filelist = fileupload.parseRequest(request.getHttpServletRequest());
        } catch (FileUploadException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_UPLOAD_PARSE_EXCEPTION,
                    "Exception while parsing uploaded file", e);
        }

        // I don't like it.. But this is the way
        // to get form elements..
        byte[] data = Utils.getContent(filelist, "filename");
        String filename = null;

        if (data!=null) {
            filename = new String(data);
        }

        // Get the file content
        data = Utils.getContent(filelist, "filepath");

        if (filename==null) {
            String fname = Utils.getFileName(filelist, "filepath");
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
        // I don't like it.. But this is the way
        // to get form elements..
        byte[] redirectdata = Utils.getContent(filelist, "xredirect");
        String redirect = null;
        if (redirectdata!=null) {
            redirect = new String(redirectdata);
        }
        if ((redirect == null)||(redirect.equals("")))
           redirect = context.getDoc().getURL("attach", true, context);
        sendRedirect(response, redirect);
        return false;
	}
}
