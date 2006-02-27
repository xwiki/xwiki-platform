package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import java.io.IOException;

public class ViewAction extends XWikiAction {
    public boolean action(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        String rev = request.getParameter("rev");
        if (rev!=null) {
            String url = context.getDoc().getURL("viewrev", request.getQueryString(), context);
            try {
                context.getResponse().sendRedirect(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public String render(XWikiContext context) throws XWikiException {
        return "view";
    }
}
