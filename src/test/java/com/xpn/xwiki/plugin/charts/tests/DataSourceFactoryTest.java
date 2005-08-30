package com.xpn.xwiki.plugin.charts.tests;

import java.util.HashMap;
import java.util.Map;

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
import com.xpn.xwiki.plugin.charts.source.TableDataSource;

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
        
		this.xclass = TestHelper.createTableDataSourceClass(context);

		this.doc = TestHelper.createDocument("Main.Doc", "{table}\n10\n{table}", context);
		
		this.xobject = TestHelper.defineTable(this.xclass, this.doc, this.context,
				0, "A1-A1", false, false);
	}

	protected void tearDown() throws Exception {
	}

	public void testCreateFromObject() throws DataSourceException {
		Map map = new HashMap();
		map.put("type", "object");
		map.put("doc", doc.getFullName());
		map.put("class", "TableDataSource");
		map.put("object_number", "0");
		DataSource source = MainDataSourceFactory.getInstance().create(map, context);
		Assert.assertEquals(1, source.getRowCount());
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(10, source.getCell(0, 0).intValue());
	}

	public void testCreateFromObjectId() throws DataSourceException {
		Map map = new HashMap();
		map.put("type", "objectid");
		map.put("id", ""+xobject.getId());
		DataSource source = MainDataSourceFactory.getInstance().create(map, context);
		Assert.assertEquals(1, source.getRowCount());
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(10, source.getCell(0, 0).intValue());
	}
	
	public void testCreateFromTable() throws DataSourceException {
		
		Map map = new HashMap();
		map.put("type", "table");
		map.put(TableDataSource.DOC, doc.getFullName());
		map.put(TableDataSource.TABLE_NUMBER, "0");
		map.put(TableDataSource.RANGE, "A1-A1");
		map.put(TableDataSource.HAS_HEADER_ROW, "false");
		map.put(TableDataSource.HAS_HEADER_COLUMN, "false");
		
		DataSource source = MainDataSourceFactory.getInstance().create(map, context);
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
