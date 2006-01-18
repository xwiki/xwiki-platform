package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class ViewAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        handleRevision(context);
        XWikiDocument doc = (XWikiDocument) context.get("doc");
        String defaultTemplate = doc.getDefaultTemplate();
        if ((defaultTemplate !=null) && (!defaultTemplate.equals(""))) {
        	return defaultTemplate;
        }
        else
        	return "view";
	}
}
