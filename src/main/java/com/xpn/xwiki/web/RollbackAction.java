package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class RollbackAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        RollbackForm form = (RollbackForm) context.getForm();

        String rev = form.getRev();
        String language = form.getLanguage();
        XWikiDocument tdoc;

        if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
            // Need to save parent and defaultLanguage if they have changed
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
                tdoc.setLanguage(language);
            }
            tdoc.setTranslation(1);
        }

        XWikiDocument olddoc = (XWikiDocument) tdoc.clone();
        XWikiDocument newdoc = xwiki.getDocument(tdoc, rev, context);

        String username = context.getUser();
        newdoc.setAuthor(username);
        newdoc.setRCSVersion(tdoc.getRCSVersion());
        xwiki.saveDocument(newdoc, olddoc, context);

        // forward to view
        String redirect = Utils.getRedirect("view", context);
        sendRedirect(response, redirect);
        return false;
	}
}
