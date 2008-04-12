package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.util.Util;

import java.io.IOException;

public class DownloadRevAction extends XWikiAction {
    public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String rev = request.getParameter("rev");
        String path = request.getRequestURI();
        String filename = Util.decodeURI(path.substring(path.lastIndexOf("/")+1),context);
        XWikiAttachment attachment = null;

        if (context.getWiki().hasAttachmentRecycleBin(context)
            && request.getParameter("rid") != null) {
            int recycleId = Integer.parseInt(request.getParameter("rid"));
            attachment = new XWikiAttachment(doc, filename);
            attachment =
                (XWikiAttachment) context.getWiki().getAttachmentRecycleBinStore()
                    .restoreFromRecycleBin(attachment, recycleId, context, true);
        } else if (request.getParameter("id")!=null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        } else {
            attachment = doc.getAttachment(filename);
        }
        if (attachment==null) {
            Object[] args = { filename };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                    "Attachment {0} not found", null, args);
        }

        synchronized (attachment) {
            try {
                attachment = attachment.getAttachmentRevision(rev, context);
                if (attachment == null) {
                    throw new XWikiException();
                }
            } catch(XWikiException e){
                String url = context.getDoc().getURL("viewattachrev", true, context);
                url += "/" + filename;
                if (request.getParameter("rid") != null) {
                    url += "?rid=" + request.getParameter("rid");
                }
                try {
                    context.getResponse().sendRedirect(url);
                    return null;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        XWikiPluginManager plugins = context.getWiki().getPluginManager();
        attachment = plugins.downloadAttachment(attachment, context);

        // Choose the right content type
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);

        response.setDateHeader("Last-Modified", attachment.getDate().getTime());
        // Sending the content of the attachment
        byte[] data = attachment.getContent(context);
        response.setContentLength(data.length);
        try {
            response.getOutputStream().write(data);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
        }
        return null;
	}
}
