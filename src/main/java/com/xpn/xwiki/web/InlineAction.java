package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class InlineAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();
        XWikiForm form = context.getForm();

        PrepareEditForm peform = (PrepareEditForm) form;
        String parent = peform.getParent();
        if (parent!=null)
            doc.setParent(parent);
        String creator = peform.getCreator();
        if (creator!=null)
            doc.setCreator(creator);
        String defaultLanguage = peform.getDefaultLanguage();
        if ((defaultLanguage!=null)&&!defaultLanguage.equals(""))
            doc.setDefaultLanguage(defaultLanguage);
        if (doc.getDefaultLanguage().equals(""))
            doc.setDefaultLanguage(context.getWiki().getLanguagePreference(context));

        doc.readFromTemplate(peform, context);

        // Set display context to 'view'
        context.put("display", "edit");
        return "inline";
	}
}
