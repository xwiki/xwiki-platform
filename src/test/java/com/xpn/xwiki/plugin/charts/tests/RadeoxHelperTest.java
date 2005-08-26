package com.xpn.xwiki.plugin.charts.tests;

import org.radeox.macro.table.Table;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.charts.RadeoxHelper;

import junit.framework.Assert;
import junit.framework.TestCase;

public class RadeoxHelperTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(RadeoxHelperTest.class);
	}
	
	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");

        this.context = new XWikiContext();
        
        this.xwiki = new XWiki(this.config, this.context);
        this.context.setWiki(this.xwiki);
        this.doc = new XWikiDocument("XWiki", "Document");
        
        table0 = "Category | Sales (K€)\n" +
			"Category 1 | 100\n" +
			"Category 2 | 50\n" +
			"Category 3 | 50\n" +
			"Total | =sum(B2:B4)";
        
        table1 = "A single cell";
  
        doc.setContent("some text\n" +
        		"{pre}a{table}b{/pre}\n" +
        		"{table}\n" + table0 + "\n{table}\n" +
        		"some other text\n" +
        		"{table}\n" + table1 + "\n{table}\n" +
        		"end");

        this.rhelper = new RadeoxHelper(doc, context);
	}
	
	protected void tearDown() throws Exception {
	}

	public void testGetTableStrings() {
		String[] tables = rhelper.getTableStrings();
		Assert.assertEquals(2, tables.length);
		Assert.assertEquals(table0, tables[0]);
		Assert.assertEquals(table1, tables[1]);
	}

	public void testGetTables() {
		Table[] tables = rhelper.getTables();
		Assert.assertEquals(2, tables.length);
		Assert.assertEquals("Category", tables[0].getXY(0, 0).toString());
		Assert.assertEquals("Sales (K€)", tables[0].getXY(1, 0).toString());
		Assert.assertEquals("Category 1", tables[0].getXY(0, 1).toString());
		Assert.assertEquals("100", tables[0].getXY(1, 1).toString());
		Assert.assertEquals("Category 2", tables[0].getXY(0, 2).toString());
		Assert.assertEquals("50", tables[0].getXY(1, 2).toString());
		Assert.assertEquals("Category 3", tables[0].getXY(0, 3).toString());
		Assert.assertEquals("50", tables[0].getXY(1, 3).toString());
		Assert.assertEquals("Total", tables[0].getXY(0, 4).toString());
		Assert.assertEquals("200", tables[0].getXY(1, 4).toString());
		try {
			tables[0].getXY(1, 5);
			Assert.fail();
		} catch (IndexOutOfBoundsException e) {
			// exception was thrown ... fuck
		}
		Assert.assertEquals("A single cell", tables[1].getXY(0, 0).toString());
	}

	public void testGetTableString() {
		Assert.assertEquals(table0, rhelper.getTableString(0));
		Assert.assertEquals(table1, rhelper.getTableString(1));
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
