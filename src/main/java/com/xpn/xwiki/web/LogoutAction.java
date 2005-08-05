package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class LogoutAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        String redirect;
        redirect = context.getRequest().getParameter("xredirect");
        if ((redirect == null)||(redirect.equals("")))
            redirect = context.getURLFactory().createURL("Main", "WebHome", "view", context).toString();
        sendRedirect(response, redirect);
        return false;
	}
}
