package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: jerem
 * Date: Jan 18, 2007
 * Time: 3:14:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class XWikiDebugGWTURLFactory  extends XWikiServletURLFactory{


    public void init(XWikiContext context) {
        URL url = context.getURL();

        servletPath = "/xwiki/";

        actionPath = "bin/";

        try {
            serverURL = new URL(url.getProtocol(), url.getHost(), 1025, "/");
        } catch (MalformedURLException e) {
            // This can't happen
        }
    }

}

