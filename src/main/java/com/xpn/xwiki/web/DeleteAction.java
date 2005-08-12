package com.xpn.xwiki.web;

import java.util.List;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class DeleteAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String confirm = request.getParameter("confirm");
        if ((confirm!=null)&&(confirm.equals("1"))) {
            String language = xwiki.getLanguagePreference(context);
            if ((language==null)||(language.equals(""))||language.equals(doc.getDefaultLanguage())) {
                xwiki.deleteAllDocuments(doc, context);
            } else {
                // Only delete the translation
                XWikiDocument tdoc = doc.getTranslatedDocument(language, context);
                xwiki.deleteDocument(tdoc, context);
            }
            return true;
        } else {
            String redirect = Utils.getRedirect(request, null);
            if (redirect==null)
                return true;
            else {
                sendRedirect(response, redirect);
                return false;
            }
        }
	}
	
	public String render(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String confirm = request.getParameter("confirm");
        if ((confirm!=null)&&(confirm.equals("1"))) {
            return "deleted";
        } else {
            return "delete";
        }
	}
}
