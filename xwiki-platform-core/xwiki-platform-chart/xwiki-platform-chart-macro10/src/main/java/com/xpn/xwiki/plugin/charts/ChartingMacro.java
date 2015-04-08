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
 */
package com.xpn.xwiki.plugin.charts;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import org.radeox.macro.BaseLocaleMacro;
import org.radeox.macro.LocaleMacro;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams;
import com.xpn.xwiki.render.macro.XWikiMacro;

public class ChartingMacro extends BaseLocaleMacro implements LocaleMacro, XWikiMacro
{
    @Override
    public String getLocaleKey()
    {
        return "macro.charting";
    }

    @Override
    public void execute(Writer writer, MacroParameter params) throws IllegalArgumentException, IOException
    {
        try {
            // Ludovic does it this way
            // RenderEngine engine = params.getContext().getRenderEngine();
            // XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
            // TODO: can this fix be applied everywhere in xwiki?

            XWikiContext xcontext = (XWikiContext) params.getContext().get("xcontext");
            XWiki xwiki = xcontext.getWiki();

            ChartingPluginApi chartingPlugin = (ChartingPluginApi) xwiki.getPluginApi("charting", xcontext);
            if (chartingPlugin == null) {
                throw exception("ChartingPlugin not loaded");
            }

            ChartParams chartParams;
            try {
                chartParams = new ChartParams(params.getParams(), DefaultChartParams.getInstance(), true);
                chartParams.check();
            } catch (ParamException e) {
                throw exception("Parameter Exception", e);
            }

            Chart chart;
            try {
                chart = chartingPlugin.generateChart(chartParams, xcontext);
            } catch (GenerateException ge) {
                throw exception("Error generating chart", ge);
            }

            String title = chartParams.getString(ChartParams.TITLE_PREFIX + ChartParams.TITLE_SUFFIX);
            Integer height = chartParams.getInteger(ChartParams.HEIGHT);
            Integer width = chartParams.getInteger(ChartParams.WIDTH);
            Map imageAttr = chartParams.getMap(ChartParams.IMAGE_ATTRIBUTES);
            Map linkAttr = chartParams.getMap(ChartParams.LINK_ATTRIBUTES);

            // output the image links
            StringBuilder sbuffer = new StringBuilder();
            sbuffer.append("<a href=\"" + chart.getPageURL() + "\" ");
            if (title != null) {
                sbuffer.append("title=\"" + title + "\"");
            }
            if (linkAttr != null) {
                Iterator it = linkAttr.keySet().iterator();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    String value = (String) imageAttr.get(name);
                    sbuffer.append(name + "=\"" + value + "\" ");
                }
            }
            sbuffer.append(">");
            sbuffer.append("<img src=\"");
            sbuffer.append(chart.getImageURL());
            sbuffer.append("\" ");
            if (title != null) {
                sbuffer.append("alt=\"" + title + "\" ");
            }
            sbuffer.append("height=\"" + height + "\" ");
            sbuffer.append("width=\"" + width + "\" ");
            if (imageAttr != null) {
                Iterator it = imageAttr.keySet().iterator();
                while (it.hasNext()) {
                    String name = (String) it.next();
                    if (name != ChartParams.HEIGHT && name != ChartParams.WIDTH) {
                        String value = (String) imageAttr.get(name);
                        sbuffer.append(name + "=\"" + value + "\" ");
                    } else {
                        throw exception("The image " + name + " can only be set by the " + name + "parameter");
                    }
                }
            }
            sbuffer.append("/>");
            sbuffer.append("</a>");

            writer.write(sbuffer.toString());
        } catch (XWikiException xwe) {
            writer.write("Charting exception: " + xwe.getFullMessage());
        } catch (Throwable t) {
            writer.write("Unexpected charting exception: " + t.getMessage());
            t.printStackTrace(new PrintWriter(writer));
        }
    }

    private XWikiException exception(String message)
    {
        return new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, message);
    }

    private XWikiException exception(String message, Throwable throwable)
    {
        return new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, throwable
            .getMessage(), throwable);
    }
}
