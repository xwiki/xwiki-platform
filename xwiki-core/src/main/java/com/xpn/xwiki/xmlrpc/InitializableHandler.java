package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiException;

import javax.servlet.Servlet;

public interface InitializableHandler
{
    public void init(Servlet servlet) throws XWikiException;
}
