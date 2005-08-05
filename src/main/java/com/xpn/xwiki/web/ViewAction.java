package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class ViewAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        handleRevision(context);
        return "view";
	}
}
