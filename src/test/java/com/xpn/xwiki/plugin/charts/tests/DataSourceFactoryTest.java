package com.xpn.xwiki.plugin.charts.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.source.DataSource;
import com.xpn.xwiki.plugin.charts.source.MainDataSourceFactory;

import java.util.HashMap;
import java.util.Map;

public class DataSourceFactoryTest extends TestCase {
	
	public DataSourceFactoryTest(String arg0) {
		super(arg0);
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(DataSourceFactoryTest.class);
	}

	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");

        this.context = new XWikiContext();
        this.xwiki = new XWiki(this.config, this.context);
        this.context.setWiki(this.xwiki);
        
		// this.xclass = TestHelper.createTableDataSourceClass(context);

		this.doc = TestHelper.createDocument("Main.Doc", "{table}\n10\n{table}", context);
		
		//this.xobject = TestHelper.defineTable(this.xclass, this.doc, this.context,
		//		0, "A1:A1", false, false);
	}

	protected void tearDown() throws Exception {
	}

	public void testCreateFromTable() throws DataSourceException {
        Map params = new HashMap();
        params.put("type", "table");
        params.put("doc", doc.getFullName());
        params.put("table_number", "0");

		DataSource source = MainDataSourceFactory.getInstance()
			.create(params, context);
		Assert.assertEquals(1, source.getRowCount());
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(10, source.getCell(0, 0).intValue());
	}

	public void testCreateFromObjectId() throws DataSourceException {
        Map params = new HashMap();
        params.put("type", "objectid");
        params.put("id", "" + xobject.getId());

		DataSource source = MainDataSourceFactory.getInstance()
			.create(params, context);
		Assert.assertEquals(1, source.getRowCount());
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(10, source.getCell(0, 0).intValue());
	}

	private XWikiDocument doc; 
	private BaseClass xclass;
	private BaseObject xobject;
    private XWiki xwiki;
    private XWikiConfig config;
    private XWikiContext context;
}
