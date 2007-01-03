package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ResetVersionsAction extends XWikiAction {
    public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String language = context.getRequest().getParameter("language");
        XWikiDocument tdoc;

        String confirm = request.getParameter("confirm");
        if ((confirm!=null)&&(confirm.equals("1"))) {
            if ((language==null)||(language.equals(""))||(language.equals("default"))||(language.equals(doc.getDefaultLanguage()))) {
                // Need to save parent and defaultLanguage if they have changed
                tdoc = doc;
            } else {
                tdoc = doc.getTranslatedDocument(language, context);
                if (tdoc == doc) {
                    tdoc = new XWikiDocument(doc.getSpace(), doc.getName());
                    tdoc.setLanguage(language);
                }
                tdoc.setTranslation(1);
            }

            // Do it
            tdoc.resetArchive(context);
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
            return "resetversionsdone";
        } else {
            return "resetversions";
        }
    }

}
