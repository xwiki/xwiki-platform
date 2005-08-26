package com.xpn.xwiki.plugin.charts.tests;

import java.io.File;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.ServletContext;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.textui.TestRunner;

import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.engine.BaseRenderEngine;
import org.radeox.engine.context.BaseInitialRenderContext;
import org.radeox.engine.context.BaseRenderContext;
import org.radeox.macro.parameter.BaseMacroParameter;
import org.radeox.macro.parameter.MacroParameter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.plugin.charts.ChartingMacro;
import com.xpn.xwiki.plugin.charts.mocks.MockChartingPlugin;
import com.xpn.xwiki.plugin.charts.mocks.MockSVGPlugin;
import com.xpn.xwiki.plugin.charts.mocks.MockServletContext;
import com.xpn.xwiki.plugin.charts.mocks.MockStore;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import com.xpn.xwiki.web.XWikiServletContext;

public class ChartingMacroTest extends TestCase {

	public ChartingMacroTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestRunner.run(ChartingMacroTest.class);
	}
	
	protected void setUp() throws Exception {
		InitialRenderContext initialContext = new BaseInitialRenderContext();
//		initialContext.set(RenderContext.INPUT_LOCALE, new Locale("mywiki", "mywiki"));
		engine = new BaseRenderEngine(initialContext);
		
		XWikiContext xcontext = new XWikiContext();
		// following lines needed by SVG Plugin
		ServletContext scontext = new MockServletContext();
		XWikiServletContext xscontext = new XWikiServletContext(scontext);
		xscontext.setAttribute("javax.servlet.context.tempdir", new File("."));
        xcontext.setEngineContext(xscontext);

		XWikiConfig config = new XWikiConfig();
		config.setProperty("xwiki.store.class", MockStore.class.getCanonicalName());
		XWiki wiki = new XWiki(config, xcontext);		
		
		XWikiPluginManager pluginManager = new XWikiPluginManager();
		pluginManager.addPlugin("svg", MockSVGPlugin.class.getCanonicalName(), xcontext);
		pluginManager.addPlugin("charting", MockChartingPlugin.class.getCanonicalName(), xcontext);
		wiki.setPluginManager(pluginManager);
		
		Assert.assertSame(wiki, xcontext.getWiki());
		
		RenderContext rcontext = new BaseRenderContext();
        params = new BaseMacroParameter(rcontext);
        rcontext.set("xcontext", xcontext);
        params.setParams("");
        
        // This is needed so that our local config is used
        InitialRenderContext ircontext = new BaseInitialRenderContext();
        Locale locale = new Locale("xwiki", "xwiki");
        ircontext.set(RenderContext.INPUT_LOCALE, locale);
        ircontext.set(RenderContext.OUTPUT_LOCALE, locale);

        XWikiRadeoxRenderEngine radeoxengine = new XWikiRadeoxRenderEngine(ircontext, xcontext);
        rcontext.setRenderEngine(radeoxengine);
        
        macro = new ChartingMacro();
	}

	protected void tearDown() throws Exception {
	}
	
	public void testBold() {
		String result = engine.render("__Radeox__", new BaseRenderContext());
		Assert.assertEquals("<b class=\"bold\">Radeox</b>", result);		
	}
	
	
	public void testExecute() throws Exception {
		params.getParams().put("title", "A title");
		params.getParams().put("type", "pie");
		params.getParams().put("source", "table2");
		
		StringWriter swriter = new StringWriter();
		macro.execute(swriter, params);
		System.out.println(swriter.getBuffer().toString());
	}
	
	MacroParameter params;
	RenderEngine engine;
	ChartingMacro macro;
}
