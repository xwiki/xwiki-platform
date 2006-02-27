package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class ViewrevAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        try {
        handleRevision(context);
        } catch (XWikiException e) {
           if (e.getCode()==XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION)
              return "notexist";
           else
             throw e;
        }
        return "view";
    }
}
