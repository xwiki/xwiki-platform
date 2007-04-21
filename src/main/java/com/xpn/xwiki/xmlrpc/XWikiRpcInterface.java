package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiException;

import java.util.List;

public interface XWikiRpcInterface
{

    public abstract String getPage(String name) throws XWikiException, Exception;

    public abstract List getAllPages() throws XWikiException, Exception;

}
