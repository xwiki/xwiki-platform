package com.xpn.xwiki.web;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class RegisterAction extends XWikiAction {
	public boolean action(XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String register = request.getParameter("register");
        if ((register!=null)&&(register.equals("1"))) {
            int useemail = xwiki.getXWikiPreferenceAsInt("use_email_verification", 0, context);
            int result;
            if (useemail==1)
             result = xwiki.createUser(true, "edit", context);
            else
             result = xwiki.createUser(context);
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("reg", new Integer(result));
        }

        String redirect = Utils.getRedirect(request, null);
        if (redirect==null)
            return true;
        else {
            sendRedirect(response, redirect);
            return false;
        }
	}
	
	public String render(XWikiContext context) throws XWikiException {
        return "register";
	}
}
