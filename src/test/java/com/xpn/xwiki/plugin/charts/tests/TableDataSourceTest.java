package com.xpn.xwiki.plugin.charts.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.EmptyDataSourceException;
import com.xpn.xwiki.plugin.charts.source.DataSource;
import com.xpn.xwiki.plugin.charts.source.TableDataSource;

public class TableDataSourceTest extends TestCase {
	public TableDataSourceTest(String arg0) {
		super(arg0);
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(TableDataSourceTest.class);
	}

	protected void setUp() throws Exception {
        this.config = new XWikiConfig();
        this.config.put("xwiki.store.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");
        this.context = new XWikiContext();
        this.xwiki = new XWiki(this.config, this.context);
        this.context.setWiki(this.xwiki);

        this.doc = TestHelper.createDocument("Doc",
        		"{table}\n" +
        		"Empty | Column0 | Column1 | Column2 \n" +
        		"Row0 | 1 | 2 | 3 \n" +
        		"Row1 | 4 | 5 | 6 \n" +
        		"Row2 | 7 | 8 | 9 \n" +
        		"{table}", context
        );

		this.xclass = TestHelper.createTableDataSourceClass(context);
	}

	protected void tearDown() throws Exception {
	}
	
	public void testTableDataSourceWholeWithHeaders() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "A1:D4", true, true);
		
		DataSource source = new TableDataSource(xobject, context);
                
		Assert.assertEquals(3, source.getColumnCount());
		Assert.assertEquals(3, source.getRowCount());
		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		Assert.assertEquals(2, source.getCell(0,1).longValue());
		Assert.assertEquals(3, source.getCell(0,2).longValue());
		Assert.assertEquals(4, source.getCell(1,0).longValue());
		Assert.assertEquals(5, source.getCell(1,1).longValue());
		Assert.assertEquals(6, source.getCell(1,2).longValue());
		Assert.assertEquals(7, source.getCell(2,0).longValue());
		Assert.assertEquals(8, source.getCell(2,1).longValue());
		Assert.assertEquals(9, source.getCell(2,2).longValue());
		
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderColumn());
		
		Assert.assertEquals(3, source.getHeaderRow().length);
		Assert.assertEquals(3, source.getHeaderColumn().length);
		
		Assert.assertEquals("Column0", source.getHeaderRowValue(0));
		Assert.assertEquals("Column1", source.getHeaderRowValue(1));
		Assert.assertEquals("Column2", source.getHeaderRowValue(2));		

		Assert.assertEquals("Row0", source.getHeaderColumnValue(0));
		Assert.assertEquals("Row1", source.getHeaderColumnValue(1));
		Assert.assertEquals("Row2", source.getHeaderColumnValue(2));
	}
	
	public void testTableDataSourceWholeWithHeaders2() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "*", true, true);
		
		DataSource source = new TableDataSource(xobject, context);
                
		Assert.assertEquals(3, source.getColumnCount());
		Assert.assertEquals(3, source.getRowCount());
		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		Assert.assertEquals(2, source.getCell(0,1).longValue());
		Assert.assertEquals(3, source.getCell(0,2).longValue());
		Assert.assertEquals(4, source.getCell(1,0).longValue());
		Assert.assertEquals(5, source.getCell(1,1).longValue());
		Assert.assertEquals(6, source.getCell(1,2).longValue());
		Assert.assertEquals(7, source.getCell(2,0).longValue());
		Assert.assertEquals(8, source.getCell(2,1).longValue());
		Assert.assertEquals(9, source.getCell(2,2).longValue());
		
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderColumn());
		
		Assert.assertEquals(3, source.getHeaderRow().length);
		Assert.assertEquals(3, source.getHeaderColumn().length);
		
		Assert.assertEquals("Column0", source.getHeaderRowValue(0));
		Assert.assertEquals("Column1", source.getHeaderRowValue(1));
		Assert.assertEquals("Column2", source.getHeaderRowValue(2));		

		Assert.assertEquals("Row0", source.getHeaderColumnValue(0));
		Assert.assertEquals("Row1", source.getHeaderColumnValue(1));
		Assert.assertEquals("Row2", source.getHeaderColumnValue(2));
	}

	public void testTableDataSourceWholeNoHeaders() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "B2:D4", false, false);
		
		DataSource source = new TableDataSource(xobject, context);
                
		Assert.assertEquals(3, source.getColumnCount());
		Assert.assertEquals(3, source.getRowCount());
		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		Assert.assertEquals(2, source.getCell(0,1).longValue());
		Assert.assertEquals(3, source.getCell(0,2).longValue());
		Assert.assertEquals(4, source.getCell(1,0).longValue());
		Assert.assertEquals(5, source.getCell(1,1).longValue());
		Assert.assertEquals(6, source.getCell(1,2).longValue());
		Assert.assertEquals(7, source.getCell(2,0).longValue());
		Assert.assertEquals(8, source.getCell(2,1).longValue());
		Assert.assertEquals(9, source.getCell(2,2).longValue());
		
		Assert.assertFalse(source.hasHeaderRow());
		Assert.assertFalse(source.hasHeaderColumn());
	}
	
	public void testTableDataSourceWholeNoHeaders2() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "*", false, false);
		
		DataSource source = new TableDataSource(xobject, context);
                
		Assert.assertEquals(4, source.getColumnCount());
		Assert.assertEquals(4, source.getRowCount());
		
		Assert.assertNull(source.getCell(0,0));
		Assert.assertNull(source.getCell(0,1));
		Assert.assertNull(source.getCell(0,2));
		Assert.assertNull(source.getCell(0,3));
		Assert.assertNull(source.getCell(0,0));
		Assert.assertNull(source.getCell(1,0));
		Assert.assertNull(source.getCell(2,0));
		Assert.assertNull(source.getCell(3,0));
		Assert.assertEquals(1, source.getCell(1,1).longValue());
		Assert.assertEquals(2, source.getCell(1,2).longValue());
		Assert.assertEquals(3, source.getCell(1,3).longValue());
		Assert.assertEquals(4, source.getCell(2,1).longValue());
		Assert.assertEquals(5, source.getCell(2,2).longValue());
		Assert.assertEquals(6, source.getCell(2,3).longValue());
		Assert.assertEquals(7, source.getCell(3,1).longValue());
		Assert.assertEquals(8, source.getCell(3,2).longValue());
		Assert.assertEquals(9, source.getCell(3,3).longValue());
		
		Assert.assertFalse(source.hasHeaderRow());
		Assert.assertFalse(source.hasHeaderColumn());
	}

	public void testTableDataSourceSingleCellWithHeader() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "A1:B2", true, true);
		DataSource source = new TableDataSource(xobject, context);

		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(1, source.getRowCount());		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderColumn());
		
		Assert.assertEquals(1, source.getHeaderRow().length);
		Assert.assertEquals(1, source.getHeaderColumn().length);
		
		Assert.assertEquals("Row0", source.getHeaderColumnValue(0));
		Assert.assertEquals("Column0", source.getHeaderRowValue(0));

	}
	
	public void testTableDataSourceSingleCellNoHeader() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "B2:B2", false, false);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(1, source.getRowCount());		
		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		
		Assert.assertFalse(source.hasHeaderRow());
		Assert.assertFalse(source.hasHeaderColumn());
	}

	public void testTableDataSourceWholeRowsWithHeader() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "1:3", true, true);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(2, source.getRowCount());
		Assert.assertEquals(3, source.getColumnCount());
		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		Assert.assertEquals(2, source.getCell(0,1).longValue());
		Assert.assertEquals(3, source.getCell(0,2).longValue());
		Assert.assertEquals(4, source.getCell(1,0).longValue());
		Assert.assertEquals(5, source.getCell(1,1).longValue());
		Assert.assertEquals(6, source.getCell(1,2).longValue());
		
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderColumn());
		
		Assert.assertEquals(3, source.getHeaderRow().length);
		Assert.assertEquals(2, source.getHeaderColumn().length);
		
		Assert.assertEquals("Column0", source.getHeaderRowValue(0));
		Assert.assertEquals("Column1", source.getHeaderRowValue(1));
		Assert.assertEquals("Column2", source.getHeaderRowValue(2));

		Assert.assertEquals("Row0", source.getHeaderColumnValue(0));
		Assert.assertEquals("Row1", source.getHeaderColumnValue(1));
	}
	
	public void testTableDataSourceWholeRowsNoHeaderRow() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "3:4", false, true);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(3, source.getColumnCount());
		Assert.assertEquals(2, source.getRowCount());
		
		Assert.assertEquals(4, source.getCell(0,0).longValue());
		Assert.assertEquals(5, source.getCell(0,1).longValue());
		Assert.assertEquals(6, source.getCell(0,2).longValue());
		Assert.assertEquals(7, source.getCell(1,0).longValue());
		Assert.assertEquals(8, source.getCell(1,1).longValue());
		Assert.assertEquals(9, source.getCell(1,2).longValue());
		
		Assert.assertFalse(source.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderColumn());
		
		Assert.assertEquals(2, source.getHeaderColumn().length);
		
		Assert.assertEquals("Row1", source.getHeaderColumnValue(0));
		Assert.assertEquals("Row2", source.getHeaderColumnValue(1));
	}
	
	public void testTableDataSourceWholeRowsNoHeader() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "3:4", false, false);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(2, source.getRowCount());
		Assert.assertEquals(4, source.getColumnCount());
		
		Assert.assertNull(source.getCell(0,0));
		Assert.assertNull(source.getCell(1,0));
		Assert.assertEquals(4, source.getCell(0,1).longValue());
		Assert.assertEquals(5, source.getCell(0,2).longValue());
		Assert.assertEquals(6, source.getCell(0,3).longValue());
		Assert.assertEquals(7, source.getCell(1,1).longValue());
		Assert.assertEquals(8, source.getCell(1,2).longValue());
		Assert.assertEquals(9, source.getCell(1,3).longValue());
		
		Assert.assertFalse(source.hasHeaderRow());
		Assert.assertFalse(source.hasHeaderColumn());
	}
	
	public void testTableDataSourceWholeColumnsWithHeader() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "A:C", true, true);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(3, source.getRowCount());
		Assert.assertEquals(2, source.getColumnCount());
		
		Assert.assertEquals(1, source.getCell(0,0).longValue());
		Assert.assertEquals(2, source.getCell(0,1).longValue());
		Assert.assertEquals(4, source.getCell(1,0).longValue());
		Assert.assertEquals(5, source.getCell(1,1).longValue());
		Assert.assertEquals(7, source.getCell(2,0).longValue());
		Assert.assertEquals(8, source.getCell(2,1).longValue());
		
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderColumn());
		
		Assert.assertEquals(2, source.getHeaderRow().length);
		Assert.assertEquals(3, source.getHeaderColumn().length);
		
		Assert.assertEquals("Column0", source.getHeaderRowValue(0));
		Assert.assertEquals("Column1", source.getHeaderRowValue(1));

		Assert.assertEquals("Row0", source.getHeaderColumnValue(0));
		Assert.assertEquals("Row1", source.getHeaderColumnValue(1));
		Assert.assertEquals("Row2", source.getHeaderColumnValue(2));
	}
	
	public void testTableDataSourceWholeColumnsNoHeaderColumn() throws DataSourceException, XWikiException {
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "C:D", true, false);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(3, source.getRowCount());
		Assert.assertEquals(2, source.getColumnCount());
		
		Assert.assertEquals(2, source.getCell(0,0).longValue());
		Assert.assertEquals(3, source.getCell(0,1).longValue());
		Assert.assertEquals(5, source.getCell(1,0).longValue());
		Assert.assertEquals(6, source.getCell(1,1).longValue());
		Assert.assertEquals(8, source.getCell(2,0).longValue());
		Assert.assertEquals(9, source.getCell(2,1).longValue());
		
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertFalse(source.hasHeaderColumn());
		
		Assert.assertEquals(2, source.getHeaderRow().length);
		
		Assert.assertEquals("Column1", source.getHeaderRowValue(0));
		Assert.assertEquals("Column2", source.getHeaderRowValue(1));
	}
	
	public void testTableDataSourceWholeColumnsNoHeader() throws DataSourceException, XWikiException {		
		BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "C:D", false, false);
		DataSource source = new TableDataSource(xobject, context);
		
		Assert.assertEquals(4, source.getRowCount());
		Assert.assertEquals(2, source.getColumnCount());
		
		Assert.assertNull(source.getCell(0,0));
		Assert.assertNull(source.getCell(0,1));
		Assert.assertEquals(2, source.getCell(1,0).longValue());
		Assert.assertEquals(3, source.getCell(1,1).longValue());
		Assert.assertEquals(5, source.getCell(2,0).longValue());
		Assert.assertEquals(6, source.getCell(2,1).longValue());
		Assert.assertEquals(8, source.getCell(3,0).longValue());
		Assert.assertEquals(9, source.getCell(3,1).longValue());
		
		Assert.assertFalse(source.hasHeaderRow());
		Assert.assertFalse(source.hasHeaderColumn());		
	}

	public void testTableDataSourceOneCellWithHeaders() throws DataSourceException, XWikiException {
		try {
			BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "B2:B2", true, true);
			new TableDataSource(xobject, context);			
			Assert.fail("Empty data source with headers");
		} catch (EmptyDataSourceException e) {
			// ok
		}
	}

	public void testTableDataSourceNoCell() throws DataSourceException, XWikiException {
		try {
			BaseObject xobject = TestHelper.defineTable(this.xclass, this.doc, this.context, 0, "B2:A1", true, true);
			new TableDataSource(xobject, context);			
			Assert.fail("Bad range");
		} catch (EmptyDataSourceException e) {
			// TODO: BadRangeDataSourceException?
		}
	}
	private BaseClass xclass;
	private XWikiContext context;
    private XWikiDocument doc;
    private XWiki xwiki;
    private XWikiConfig config;
}
