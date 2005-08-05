package com.xpn.xwiki.web;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.graphviz.GraphVizPlugin;

public class DotAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        String path = request.getRequestURI();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);
        try {
           ((GraphVizPlugin)context.getWiki().getPlugin("graphviz",context)).outputDotImageFromFile(filename, context);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
        }
        return null;
	}
}
