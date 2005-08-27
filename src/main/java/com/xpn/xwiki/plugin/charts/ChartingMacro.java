package com.xpn.xwiki.plugin.charts;

import java.io.IOException;
import java.io.Writer;

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

public class ChartingMacro extends BaseLocaleMacro implements LocaleMacro, XWikiMacro {
	public ChartingMacro() {
		super();
	}

	public String getLocaleKey() {
		return "macro.charting";
	}

	public void execute(Writer writer, MacroParameter params)
			throws IllegalArgumentException, IOException {
		try {
// Ludovic does it this way
//			RenderEngine engine = params.getContext().getRenderEngine();
//	        XWikiContext xcontext = ((XWikiRadeoxRenderEngine)engine).getContext();
// TODO: can this fix be applied everywhere in xwiki?

			XWikiContext xcontext = (XWikiContext)params.getContext().get("xcontext");
	        XWiki xwiki = xcontext.getWiki();

	        ChartingPluginApi chartingPlugin = (ChartingPluginApi)
	        		xwiki.getPluginApi("charting", xcontext);
	        if (chartingPlugin == null) {
	        	throw exception("ChartingPlugin not loaded");
	        }

	        ChartParams chartParams;
	        try {
	        	chartParams = new ChartParams(params.getParams(),
	        		DefaultChartParams.getInstance(), true);
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

	        String title = chartParams.getString(
	        		ChartParams.TITLE_PREFIX+ChartParams.TITLE_SUFFIX);
	        Integer height = chartParams.getInteger(ChartParams.HEIGHT);
	        Integer width = chartParams.getInteger(ChartParams.WIDTH);
	        
	        // output the links
	        StringBuffer sbuffer = new StringBuffer();
	        sbuffer.append("<a href=\""+ chart.getPageURL() + "\" ");
	        sbuffer.append("title=\""+chart.getTitle()+"\">");
	        sbuffer.append("<img src=\"");
	        sbuffer.append(chart.getImageURL());
	        sbuffer.append("\" ");
	        sbuffer.append("alt=\""+ title + "\" ");
	        if (height != null) {
		        sbuffer.append("height=\"" + height + "\" ");
	        }
	        if (width != null) {
	        	sbuffer.append("width=\"" + width + "\" ");
	        }
	        sbuffer.append("\" />");
	        sbuffer.append("</a>");
	        
	        writer.write(sbuffer.toString());
		} catch (XWikiException xwe) {
			writer.write("Charting exception: "+xwe.getFullMessage());
		}
	}
	
	private XWikiException exception(String message) {
		return new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
				XWikiException.ERROR_XWIKI_UNKNOWN, message);
	}

	private XWikiException exception(String message, Throwable throwable) {
		return new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
				XWikiException.ERROR_XWIKI_UNKNOWN, throwable.getMessage(), throwable);
	}
}
