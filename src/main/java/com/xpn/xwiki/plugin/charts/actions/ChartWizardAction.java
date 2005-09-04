package com.xpn.xwiki.plugin.charts.actions;

import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams2;
import com.xpn.xwiki.plugin.charts.params.HorizontalAlignmentChartParam;
import com.xpn.xwiki.plugin.charts.wizard.FontHelper;
import com.xpn.xwiki.plugin.charts.wizard.DatasourceDefaultsHelper;
import com.xpn.xwiki.web.XWikiAction;

public class ChartWizardAction extends XWikiAction {
    public String render(XWikiContext context) throws XWikiException {
        VelocityContext vcontext = (VelocityContext)context.get("vcontext");
        vcontext.put("fontHelper", new FontHelper());
        
        try{
          vcontext.put("chartDefaults", DefaultChartParams2.getInstance());
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        vcontext.put("datasourceDefaults", new DatasourceDefaultsHelper());
        return "chwmain";
    }
}
