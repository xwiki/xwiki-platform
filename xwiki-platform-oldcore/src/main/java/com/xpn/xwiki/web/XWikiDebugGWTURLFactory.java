package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;

import java.net.URL;
import java.net.MalformedURLException;

public class XWikiDebugGWTURLFactory  extends XWikiServletURLFactory{


    public void init(XWikiContext context) {
        URL url = context.getURL();

        contextPath = "xwiki/";

        try {
            serverURL = new URL(url.getProtocol(), url.getHost(), 1025, "/");
        } catch (MalformedURLException e) {
            // This can't happen
        }
    }

}

