package com.xpn.xwiki.plugin.charts;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiAction;

public class ChartWizardAction extends XWikiAction {
    public String render(XWikiContext context) throws XWikiException {
        VelocityContext vcontext = (VelocityContext)context.get("vcontext");
        vcontext.put("fonthelper", new FontHelper());
        return "chwmain";
    }
}
