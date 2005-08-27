package com.xpn.xwiki.plugin.charts;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * This clas generates the tables for the new datasource wizard.
 * @author Sergiu Dumitriu
 *
 */
public class GetTablesAction extends XWikiAction{
    public String render(XWikiContext context) throws XWikiException {
        XWikiResponse response = context.getResponse();
        response.setContentType("application/html+xml");
        response.setCharacterEncoding("UTF-8"); //TODO: make this work!
        
        VelocityContext vcontext = (VelocityContext)context.get("vcontext");
        vcontext.put("rhelper", new RadeoxHelper(context.getDoc(), context));
        
        XWikiDocument doc = context.getDoc();
        return "gettables";
    }
}
