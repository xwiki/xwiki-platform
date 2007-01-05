package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 17 dec. 2006
 * Time: 22:55:08
 * To change this template use File | Settings | File Templates.
 */
public class UnknownAction extends XWikiAction {

    // hook
    public String render(XWikiContext context) throws XWikiException {
        return "exception";
    }
}
