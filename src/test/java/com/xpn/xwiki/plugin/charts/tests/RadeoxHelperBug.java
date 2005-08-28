package com.xpn.xwiki.plugin.charts.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.radeox.macro.table.Table;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.charts.RadeoxHelper;

public class RadeoxHelperBug extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(RadeoxHelperBug.class);
	}
	
	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");

        this.context = new XWikiContext();
        
        this.xwiki = new XWiki(this.config, this.context);
        this.context.setWiki(this.xwiki);
        this.doc = new XWikiDocument("XWiki", "Document");
        
        table0 = "A single cell";
     
        doc.setContent("{table}\n" + table0 + "\n{table}\n");

        this.rhelper = new RadeoxHelper(doc, context);
	}
	
	protected void tearDown() throws Exception {
	}

	public void testGetTableStrings() {
		String[] tables = rhelper.getTableStrings();
		Assert.assertEquals(1, tables.length);
		Assert.assertEquals(table0, tables[0]);
	}

	public void testGetTables() {
		Table[] tables = rhelper.getTables();
		Assert.assertEquals(1, tables.length);
		Assert.assertEquals("A single cell", tables[0].getXY(0, 0).toString());
	}

	public void testGetTableString() {
		Assert.assertEquals(table0, rhelper.getTableString(0));
	}

	public void testRenderTable() {

	}

	private String table0, table1;
	private RadeoxHelper rhelper;
    private XWiki xwiki;
    private XWikiDocument doc;
    private XWikiConfig config;
    private XWikiContext context;
}
