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
 * @author sdumitriu
 */
package com.xpn.xwiki.plugin.charts.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.xpn.xwiki.plugin.charts.exceptions.ColumnIndexOutOfBoundsException;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.NoHeaderColumnException;
import com.xpn.xwiki.plugin.charts.exceptions.NoHeaderRowException;
import com.xpn.xwiki.plugin.charts.exceptions.RowIndexOutOfBoundsException;
import com.xpn.xwiki.plugin.charts.source.DefaultDataSource;

public class DefaultDataSourceTest extends TestCase {
	public DefaultDataSourceTest(String arg0) {
		super(arg0);
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(DefaultDataSourceTest.class);
	}

	protected void setUp() throws Exception {
		headerRow = new String[1];
		headerRow[0] = "Sales (K€)";

		headerColumn = new String[4];
		headerColumn[0] = "Category 1";
		headerColumn[1] = "Category 2";
		headerColumn[2] = "Category 3";
		headerColumn[3] = "Total";
		
		data = new Number[4][1];
		data[0][0] = new Double(50);
		data[1][0] = new Double(50);
		data[2][0] = new Double(100);
		data[3][0] = new Double(200);
		
		source = new DefaultDataSource(data, headerRow, headerColumn);
		
		noHeaderSource = new DefaultDataSource(data);
		
		emptySource = new DefaultDataSource();
		
	}

	protected void tearDown() throws Exception {
	}
	
	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.DefaultDataSource()'
	 */
	public void testDefaultDataSource() {
		Assert.assertEquals(0, emptySource.getRowCount());
		Assert.assertEquals(0, emptySource.getColumnCount());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.DefaultDataSource(Number[][])'
	 */
	public void testDefaultDataSourceNumberArrayArray() throws DataSourceException {
		Assert.assertEquals(4, noHeaderSource.getRowCount());
		Assert.assertEquals(1, noHeaderSource.getColumnCount());
		Assert.assertEquals(new Double(50), noHeaderSource.getCell(0, 0));
		Assert.assertEquals(new Double(50), noHeaderSource.getCell(1, 0));
		Assert.assertEquals(new Double(100), noHeaderSource.getCell(2, 0));
		Assert.assertEquals(new Double(200), noHeaderSource.getCell(3, 0));		
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.DefaultDataSource(Number[][], String[], String[])'
	 */
	public void testDefaultDataSourceNumberArrayArrayStringArrayStringArray() throws DataSourceException {
		Assert.assertEquals(4, source.getRowCount());
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(new Double(50), source.getCell(0, 0));
		Assert.assertEquals(new Double(50), source.getCell(1, 0));
		Assert.assertEquals(new Double(100), source.getCell(2, 0));
		Assert.assertEquals(new Double(200), source.getCell(3, 0));	
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getRowCount()'
	 */
	public void testGetRowCount() {
		Assert.assertEquals(4, source.getRowCount());
		Assert.assertEquals(4, noHeaderSource.getRowCount());
		Assert.assertEquals(0, emptySource.getRowCount());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getColumnCount()'
	 */
	public void testGetColumnCount() {
		Assert.assertEquals(1, source.getColumnCount());
		Assert.assertEquals(1, noHeaderSource.getColumnCount());
		Assert.assertEquals(0, emptySource.getColumnCount());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getCell(int, int)'
	 */
	public void testGetCell() throws DataSourceException {
		Assert.assertEquals(new Double(50), source.getCell(0, 0));
		Assert.assertEquals(new Double(50), source.getCell(1, 0));
		Assert.assertEquals(new Double(100), source.getCell(2, 0));
		Assert.assertEquals(new Double(200), source.getCell(3, 0));
		try {
			emptySource.getCell(0, 0);
			Assert.fail("DataSourceIndexOutOfBoundsException not thrown");
		} catch (RowIndexOutOfBoundsException e) { /*empty*/ }
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.setCell(int, int, Number)'
	 */
	public void testSetCell() throws DataSourceException {
		Assert.assertEquals(new Double(50), source.getCell(0, 0));
		source.setCell(0, 0, new Double(0));
		Assert.assertEquals(new Double(0), source.getCell(0, 0));		

		Assert.assertEquals(new Double(50), source.getCell(1, 0));
		source.setCell(1, 0, new Double(1));
		Assert.assertEquals(new Double(1), source.getCell(1, 0));		

		Assert.assertEquals(new Double(100), source.getCell(2, 0));
		source.setCell(2, 0, new Double(2));
		Assert.assertEquals(new Double(2), source.getCell(2, 0));		

		Assert.assertEquals(new Double(200), source.getCell(3, 0));
		source.setCell(3, 0, new Double(3));
		Assert.assertEquals(new Double(3), source.getCell(3, 0));		

		try {
			emptySource.setCell(0, 0, new Double(13));
			Assert.fail("DataSourceIndexOutOfBoundsException not thrown");
		} catch (RowIndexOutOfBoundsException e) { /*empty*/ }
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getRow(int)'
	 */
	public void testGetRow() throws DataSourceException {
		Number[] raw0 = source.getRow(0);
		Assert.assertEquals(1, raw0.length);
		Assert.assertEquals(new Double(50), raw0[0]);

		Number[] raw1 = source.getRow(1);
		Assert.assertEquals(1, raw1.length);
		Assert.assertEquals(new Double(50), raw1[0]);

		Number[] raw2 = source.getRow(2);
		Assert.assertEquals(1, raw2.length);
		Assert.assertEquals(new Double(100), raw2[0]);

		Number[] raw3 = source.getRow(3);
		Assert.assertEquals(1, raw3.length);
		Assert.assertEquals(new Double(200), raw3[0]);
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getColumn(int)'
	 */
	public void testGetColumn() throws DataSourceException {
		Number[] column = source.getColumn(0);
		Assert.assertEquals(4, column.length);
		Assert.assertEquals(new Double(50), column[0]);
		Assert.assertEquals(new Double(50), column[1]);
		Assert.assertEquals(new Double(100), column[2]);
		Assert.assertEquals(new Double(200), column[3]);
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getAllCells()'
	 */
	public void testGetAllCells() throws DataSourceException {
		Assert.assertEquals(data, source.getAllCells());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.hasHeaderRow()'
	 */
	public void testHasHeaderRow() throws DataSourceException {
		Assert.assertFalse(noHeaderSource.hasHeaderRow());
		Assert.assertFalse(emptySource.hasHeaderRow());
		Assert.assertTrue(source.hasHeaderRow());
		Assert.assertTrue((new DefaultDataSource(
				new Number[0][0], new String[0], null))
				.hasHeaderRow());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.hasHeaderColumn()'
	 */
	public void testHasHeaderColumn() throws DataSourceException {
		Assert.assertFalse(noHeaderSource.hasHeaderColumn());
		Assert.assertFalse(emptySource.hasHeaderColumn());
		Assert.assertTrue(source.hasHeaderColumn());
		Assert.assertTrue((new DefaultDataSource(
				new Number[0][0], null, new String[0]))
				.hasHeaderColumn());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getHeaderRowValue(int)'
	 */
	public void testGetHeaderRowValue() throws DataSourceException {
		try {
			noHeaderSource.getHeaderRowValue(0);
			Assert.fail("NoHeaderRowException not thrown");
		} catch (NoHeaderRowException e) { /* empty */ }
		Assert.assertEquals("Sales (K€)", source.getHeaderRowValue(0));
		try {
			source.getHeaderRowValue(1);
			Assert.fail("ColumnIndexOutOfBoundsException not thrown");
		} catch (ColumnIndexOutOfBoundsException e) { /* empty */ }
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getHeaderRow()'
	 */
	public void testGetHeaderRow() throws DataSourceException {
		try {
			noHeaderSource.getHeaderRow();
			Assert.fail("NoHeaderRowException not thrown");
		} catch (NoHeaderRowException e) { /* empty */ }
		Assert.assertEquals(headerRow, source.getHeaderRow());
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getHeaderColumnValue(int)'
	 */
	public void testGetHeaderColumnValue() throws DataSourceException {
		try {
			noHeaderSource.getHeaderColumnValue(0);
			Assert.fail("NoHeaderColumnException not thrown");
		} catch (NoHeaderColumnException e) { /* empty */ }
		Assert.assertEquals("Category 1", source.getHeaderColumnValue(0));
		Assert.assertEquals("Category 2", source.getHeaderColumnValue(1));
		Assert.assertEquals("Category 3", source.getHeaderColumnValue(2));
		Assert.assertEquals("Total", source.getHeaderColumnValue(3));
		try {
			source.getHeaderColumnValue(4);
			Assert.fail("RowIndexOutOfBoundsException not thrown");
		} catch (RowIndexOutOfBoundsException e) { /* empty */ }
	}

	/*
	 * Test method for 'com.xpn.xwiki.plugin.charts.tests.DefaultDataSource.getHeaderColumn()'
	 */
	public void testGetHeaderColumn() throws DataSourceException {
		try {
			noHeaderSource.getHeaderColumn();
			Assert.fail("NoHeaderColumnException not thrown");
		} catch (NoHeaderColumnException e) { /* empty */ }
		Assert.assertEquals(headerColumn, source.getHeaderColumn());
	}

	private DefaultDataSource source;
	private DefaultDataSource emptySource;
	private DefaultDataSource noHeaderSource;
	private String[] headerRow;
	private String[] headerColumn;
	private Number[][] data;
}
