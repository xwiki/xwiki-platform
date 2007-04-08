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
 * @author ludovic
 * @author sdumitriu
 */
package com.xpn.xwiki.plugin.charts.tests;

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
import junit.framework.TestCase;

import javax.servlet.ServletContext;
import java.io.File;
import java.net.URL;

public class ChartingPluginTest extends TestCase {
    private ChartingPlugin plugin;
    private XWikiContext xcontext;
    private XWiki xwiki;
    private BaseClass xclass;
    private BaseObject xobject;
    private XWikiConfig config;
    private XWikiDocument doc;

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ChartingPluginTest.class);
	}

	public ChartingPluginTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");
        this.config.put("xwiki.store.attachment.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");
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
		 xobject = TestHelper.defineTable(this.xclass, this.doc, this.xcontext, 0, "A1-D4", true, true);

		 plugin = new ChartingPlugin("charting", "com.xpn.xwiki.plugin.charts.ChartingPlugin", xcontext);
	}

	protected void tearDown() throws Exception {
	}

	public void testGenerateChart() throws GenerateException, ParamException {
		ChartParams params = new ChartParams();
		params.set("title", "a chart");
		params.set("type", "pie");
		params.set("source", "type:table;doc:Page;table_number:0;range:A1-D4;has_header_row:true;has_header_column:true");
		params.set("series", "rows");
		params.set("width", "500");
		params.set("height", "400");		
		Chart chart = plugin.generateChart(params, xcontext);
		System.out.println(chart.getTitle());
		System.out.println(chart.getPageURL());
		System.out.println(chart.getImageURL());
	}
}
