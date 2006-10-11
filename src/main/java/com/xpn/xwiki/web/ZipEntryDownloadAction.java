package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiAttachment;

import java.util.Iterator;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ravenees
 * Date: Oct 4, 2006
 * Time: 5:10:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ZipEntryDownloadAction extends XWikiAction {

     public boolean action(XWikiContext context) throws XWikiException {
        System.out.println("((((((( inside ZipEntryDownloadAction )))))))");

        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String path = request.getRequestURI().trim();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);

        XWikiAttachment attachment = null;
        String originalAttachment = request.getParameter("zipfile").trim();

        if (originalAttachment!=null) {
             Iterator itr = doc.getAttachmentList().iterator();
             while(itr.hasNext()){
                attachment = (XWikiAttachment)itr.next();
                if(originalAttachment.equalsIgnoreCase(attachment.getFilename())){
                     break;
                }
             }
        }

        if (attachment==null) {
            Object[] args = { filename };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                    "Attachment {0} not found", null, args);
        }

        XWikiPluginManager plugins = context.getWiki().getPluginManager();
        attachment = plugins.downloadAttachment(attachment, context);

        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);
        try {
            byte[] data = attachment.getContent(context);
            if(data != null )
                response.setContentLength(data.length);

            response.getOutputStream().write(data);
            response.getOutputStream().close();

        }catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
        }
        return true;
     }
}
