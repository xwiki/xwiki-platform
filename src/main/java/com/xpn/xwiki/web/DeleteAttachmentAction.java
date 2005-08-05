package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class DeleteAttachmentAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String path = request.getPathInfo();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);
        XWikiAttachment attachment = null;

        if (request.getParameter("id")!=null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        }
        else {
            attachment = doc.getAttachment(filename);
        }

        doc.deleteAttachment(attachment, context);
        // forward to attach page
        String redirect = Utils.getRedirect("attach", context);
        sendRedirect(response, redirect);
        return false;
	}
}
