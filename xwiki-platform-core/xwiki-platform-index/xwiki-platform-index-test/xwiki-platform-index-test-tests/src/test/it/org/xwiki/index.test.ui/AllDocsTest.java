/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package org.xwiki.index.test.ui;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.index.test.po.AllDocsPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Tests for the AllDocs page.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class AllDocsTest extends AbstractTest
{
    @Test
    public void testTableViewActions() throws Exception
    {
        // Test 1: Verify that the Action column is displayed only for logged in users
        // Create a test user
        AllDocsPage page = new AllDocsPage();
        getUtil().registerLoginAndGotoPage(getClass().getSimpleName() + "_" + getTestMethodName(), "password",
            page.getURL());
        LiveTableElement livetable = page.clickIndexTab();
        Assert.assertTrue("No Actions column found", livetable.hasColumn("Actions"));
        // Logs out to be guest
        page.logout();
        livetable = page.clickIndexTab();
        Assert.assertFalse("Actions column shouldn't be visible for guests", livetable.hasColumn("Actions"));

        // Test 2: Verify filtering works by filtering on the document name
        livetable = page.clickIndexTab();
        livetable.filterColumn("xwiki-livetable-alldocs-filter-1", "XWikiAllGroup");
        Assert.assertTrue(livetable.hasRow("Page", "XWikiAllGroup"));
    }
}
