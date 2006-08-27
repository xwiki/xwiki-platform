package com.xpn.xwiki.xmlrpc;

import java.util.List;

import com.xpn.xwiki.XWikiException;

public interface XWikiRpcInterface {

	public abstract String getPage(String name) throws XWikiException, Exception;

	public abstract List getAllPages() throws XWikiException, Exception;

}