package com.xpn.xwiki.xmlrpc;

import javax.servlet.Servlet;

import com.xpn.xwiki.XWikiException;

public interface InitializableHandler {
	public void init(Servlet servlet) throws XWikiException;
}
