package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Util;

public class ResetVersionsAction extends XWikiAction
{
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String language = Util.normalizeLanguage(context.getRequest().getParameter("language"));

        String confirm = request.getParameter("confirm");
        if ((confirm != null) && (confirm.equals("1"))) {
            // CSRF prevention
            if (!csrfTokenCheck(context)) {
                return false;
            }

            XWikiDocument tdoc = getTranslatedDocument(doc, language, context);
            // Do it
            tdoc.resetArchive(context);
            return true;
        } else {
            String redirect = Utils.getRedirect(request, null);
            if (redirect == null) {
                return true;
            } else {
                sendRedirect(response, redirect);
                return false;
            }
        }
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String confirm = request.getParameter("confirm");
        if ((confirm != null) && (confirm.equals("1"))) {
            return "resetversionsdone";
        } else {
            return "resetversions";
        }
    }

}
