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
        this.config.put("xwiki.store.attachment.class", "com.xpn.xwiki.plugin.charts.mocks.MockStore");

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
