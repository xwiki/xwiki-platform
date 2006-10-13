/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author sdumitriu
 */
package com.xpn.xwiki.plugin.charts.actions;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams2;
import com.xpn.xwiki.plugin.charts.wizard.DatasourceDefaultsHelper;
import com.xpn.xwiki.plugin.charts.wizard.FontHelper;
import com.xpn.xwiki.web.XWikiAction;
import org.apache.velocity.VelocityContext;

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
