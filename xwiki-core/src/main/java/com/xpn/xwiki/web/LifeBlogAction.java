/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.atom.lifeblog.LifeblogServices;

import java.io.IOException;

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
