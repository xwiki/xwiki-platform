package com.xpn.xwiki.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

public class StatusAction extends Action {

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest req, HttpServletResponse resp) throws Exception {
		
		String action = mapping.getName();
        XWikiRequest request = new XWikiServletRequest(req);
        XWikiResponse response = new XWikiServletResponse(resp);
        XWikiContext context = Utils.prepareContext(action, request,
        		response, new XWikiServletContext(servlet.getServletContext()));
		
		// We should not go further for the Database Status
        // To make sure we don't have more database connections
        String renderResult = renderStatus(context);
        String page = Utils.getPage(request, renderResult);
        Utils.parseTemplate(page, !page.equals("direct"), context);
        return null;
	}
	
    public String renderStatus(XWikiContext context) throws XWikiException {
        XWiki xwiki = XWiki.getMainXWiki(context);
        VelocityContext vcontext = XWikiVelocityRenderer.prepareContext(context);
        vcontext.put("xwiki", xwiki);
        return "status";
    }
}
