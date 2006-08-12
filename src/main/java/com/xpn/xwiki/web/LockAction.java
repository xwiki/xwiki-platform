package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

public class LockAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        String language = ((EditForm)form).getLanguage();
        String defaultLanguage = ((EditForm)form).getDefaultLanguage();
        XWikiDocument tdoc;

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(language);
                tdoc.setStore(doc.getStore());
            }
            tdoc.setTranslation(1);
        }

        String username = context.getUser();
        XWikiLock lock = tdoc.getLock(context);
        if ((lock==null)||(username.equals(lock.getUserName()))) {
            if ("inline".equals(request.get("action")))
                doc.setLock(username, context);
            else
                tdoc.setLock(username, context);
        }

        // forward to view
        String ajax = request.getParameter("ajax");
        if(ajax == null || !ajax.equals("1")){
        	String redirect = Utils.getRedirect("view", context);
        	sendRedirect(response, redirect);
        }
        return false;
	}
}
