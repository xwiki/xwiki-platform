package com.xpn.xwiki.web;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.lifeblog.LifeblogServices;

public class LifeBlogAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        LifeblogServices services = new LifeblogServices(context);
        try {
    	    // Check Authentication
    	    if (!services.isAuthenticated()) {
    	      response.setHeader("WWW-Authenticate", "WSSE realm=\"foo\", profile=\"UsernameToken\"");
    	      response.sendError(401, "Unauthorized");  
    	    } else if (request.getPathInfo().equals("/lifeblog")) {
    	      services.listUserBlogs();
    	    }		        	
	    } catch (IOException e) {
	        throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
	             XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
	             "Exception while sending response", e);
        }
		return null;
	}
}
