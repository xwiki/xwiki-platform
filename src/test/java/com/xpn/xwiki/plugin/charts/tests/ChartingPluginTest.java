package com.xpn.xwiki.plugin.charts.tests;

import java.io.File;
import java.net.URL;

import javax.servlet.ServletContext;

import junit.framework.TestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.charts.Chart;
import com.xpn.xwiki.plugin.charts.ChartingPlugin;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.mocks.MockHttpServletRequest;
import com.xpn.xwiki.plugin.charts.mocks.MockServletContext;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class ChartingPluginTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ChartingPluginTest.class);
	}

	public ChartingPluginTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");
        this.xcontext = new XWikiContext();
        this.xwiki = new XWiki(this.config, this.xcontext);
        this.xcontext.setWiki(this.xwiki);
        String url = "http://www.example.com/xwiki/bin/view/Page";
		xcontext.setURL(new URL(url));
		xcontext.setRequest(new XWikiServletRequest(new MockHttpServletRequest()));
		xcontext.setURLFactory(new XWikiServletURLFactory(xcontext));
		
		// following lines needed by SVG Plugin
		ServletContext scontext = new MockServletContext();
		XWikiServletContext xscontext = new XWikiServletContext(scontext);
		xscontext.setAttribute("javax.servlet.context.tempdir", new File("."));
        xcontext.setEngineContext(xscontext);
        
        this.doc = new XWikiDocument(url, "Page");
        xcontext.setDoc(doc);

		this.xclass = TestHelper.createTableDataSourceClass(xcontext);
		xobject = TestHelper.defineTable(this.xclass, this.doc, this.xcontext, 0, "A1:D4", true, true);

		plugin = new ChartingPlugin("charting", "com.xpn.xwiki.plugin.charts.ChartingPlugin", xcontext);
	}

	protected void tearDown() throws Exception {
	}

	public void testGenerateChart() throws GenerateException, ParamException {
		ChartParams params = new ChartParams();
		params.set("title", "a chart");
		params.set("type", "pie");
		params.set("source", "table:Page:0");
		params.set("width", "500");
		params.set("height", "400");		
		Chart chart = plugin.generateChart(params, xcontext);
		System.out.println(chart.getTitle());
		System.out.println(chart.getPageURL());
		System.out.println(chart.getImageURL());
	}

	private ChartingPlugin plugin;
	private XWikiContext xcontext;
	private XWiki xwiki;
	private BaseClass xclass;
	private BaseObject xobject;
    private XWikiConfig config;
    private XWikiDocument doc;
}
