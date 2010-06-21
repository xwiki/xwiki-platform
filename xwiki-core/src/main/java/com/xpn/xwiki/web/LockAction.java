package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class LockAction extends XWikiAction
{
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        String language = ((EditForm) form).getLanguage();
        XWikiDocument tdoc = getTranslatedDocument(doc, language, context);

        String username = context.getUser();
        XWikiLock lock = tdoc.getLock(context);
        if ((lock == null) || (username.equals(lock.getUserName()))) {
            if ("inline".equals(request.get("action"))) {
                doc.setLock(username, context);
            } else {
                tdoc.setLock(username, context);
            }
        }

        // forward to view
        if (Utils.isAjaxRequest(context)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentLength(0);
        } else {
            String redirect = Utils.getRedirect("view", context);
            sendRedirect(response, redirect);
        }
        return false;
    }
}
