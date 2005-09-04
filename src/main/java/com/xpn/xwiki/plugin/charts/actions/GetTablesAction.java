package com.xpn.xwiki.plugin.charts.actions;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.charts.RadeoxHelper;
import com.xpn.xwiki.web.XWikiAction;

/**
 * This clas generates the tables for the new datasource wizard.
 * @author Sergiu Dumitriu
 *
 */
public class GetTablesAction extends XWikiAction{
    public String render(XWikiContext context) throws XWikiException {
        VelocityContext vcontext = (VelocityContext)context.get("vcontext");
        vcontext.put("rhelper", new RadeoxHelper(context.getDoc(), context));
        return "gettables";
    }
}
